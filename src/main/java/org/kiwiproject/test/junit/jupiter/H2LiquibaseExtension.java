package org.kiwiproject.test.junit.jupiter;

import static java.util.Objects.nonNull;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.kiwiproject.test.h2.H2FileBasedDatabase;

import java.io.File;

import javax.sql.DataSource;

/**
 * This JUnit Jupiter extension provides a file-based H2 database and runs Liquibase migrations to allow testing
 * against a schema. The database is created and migrations are executed before all tests. The extension provides
 * an {@link H2FileBasedDatabase} for use by tests. It also provides for injection of the database into test
 * lifecycle methods that declare a {@link H2FileBasedDatabase} annotated with {@link H2Database}.
 * <p>
 * When registering this extension, you must use a {@code static} field because both {@link BeforeAllCallback}
 * and {@link AfterAllCallback} must be registered at the class level.
 * <p>
 * Example usage showing this extension used in conjunction with a {@link Jdbi3Extension}:
 * <pre>
 * class SomeDatabaseTest {
 *
 *    {@literal @}H2LiquibaseExtension
 *     static final H2LiquibaseExtension H2_LIQUIBASE_EXTENSION =
 *             new H2LiquibaseExtension("test-migrations.xml");
 *
 *    {@literal @}RegisterExtension
 *     final Jdbi3Extension jdbi3Extension =  Jdbi3Extension.builder()
 *             .dataSource(H2_LIQUIBASE_EXTENSION.getDataSource())
 *             .plugin(new PostgresPlugin())
 *             .build();
 *
 *     // ...tests...
 *
 *     // test showing parameter annotated with @H2Database
 *    {@literal @}Test
 *     void shouldDoSomething(@H2Database H2FileBasedDatabase database) {
 *         // use the database...
 *     }
 *
 *     // ...more tests...
 * }
 * </pre>
 * Note in the above that the {@link H2LiquibaseExtension} is declared both static and final, while the
 * {@link Jdbi3Extension} is not static but is final. This configuration ensures that the H2 database
 * is set up only one time before all tests run. The {@link Jdbi3Extension} is initialized before each test with
 * a new transaction that is rolled back after each test, which again ensures code running in the transaction
 * participates in the same transaction and can see uncommitted data, but also ensures no data is actually committed
 * since the transaction is rolled back. Each test therefore does not need to worry about cleaning up any data from
 * previous tests.
 * <p>
 * If you need to ensure only a single {@link java.sql.Connection Connection} is used during a test, e.g. when testing
 * multiple DAOs (Data Access Objects) in the same test which each fetch a database connection, consider creating a
 * {@link org.kiwiproject.test.jdbc.SimpleSingleConnectionDataSource SimpleSingleConnectionDataSource} inside your
 * tests and providing it to the DAOs.
 *
 * @implNote This extension uses {@link H2FileBasedDatabaseExtension} to create the H2 database. It then performs the
 * Liquibase migrations once the database is created.
 * @see H2FileBasedDatabaseExtension
 */
@Slf4j
public class H2LiquibaseExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    /**
     * The H2 file-based database extension. Generally you should not need this directly when using this
     * extension, but we are exposing it just in case and because this is a testing library, not a production API.
     */
    @Getter
    private final H2FileBasedDatabaseExtension h2Extension;

    private final String migrationClassPathLocation;

    /**
     * The H2 file-based database for use by tests, e.g. to obtain the DataSource.
     */
    @Getter
    private H2FileBasedDatabase database;

    /**
     * Construct a new instance using the given classpath location of a Liquibase migrations file.
     *
     * @param migrationClassPathLocation classpath location of Liquibase migrations file
     */
    public H2LiquibaseExtension(String migrationClassPathLocation) {
        LOG.trace("Constructing new instance with migration path: {}", migrationClassPathLocation);
        this.migrationClassPathLocation = migrationClassPathLocation;
        this.h2Extension = new H2FileBasedDatabaseExtension();
    }

    /**
     * If a database does not already exist, creates a new H2 file-based database and runs Liquibase migrations.
     *
     * @param context the current extension context; never {@code null}
     * @throws Exception if any error occurs initializing the H2 database, executing migrations, or connecting
     */
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (nonNull(database)) {
            LOG.trace("Database exists (we are probably inside a @Nested test class) so not doing anything");
            return;
        }

        LOG.trace("Invoke H2FileBasedDatabaseExtension.beforeAll() to initialize H2 database");
        h2Extension.beforeAll(context);

        database = h2Extension.getDatabase();

        LOG.trace("Running Liquibase migrations using migrations file: {}", migrationClassPathLocation);
        try (var conn = database.getDataSource().getConnection()) {
            var liquibaseDb = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
            var liquibase = new Liquibase(migrationClassPathLocation, new ClassLoaderResourceAccessor(), liquibaseDb);
            liquibase.update(new Contexts());
        }
    }

    /**
     * Deletes the directory where the H2 test database resides, unless exiting a nested test class.
     *
     * @param context the current extension context; never {@code null}
     * @throws Exception if any error occurs deleting the test database directory
     */
    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        LOG.trace("Invoke H2FileBasedDatabaseExtension.afterAll() to shut down H2 database (if not in nested class)");
        h2Extension.afterAll(context);
    }

    /**
     * Does the parameter need to be resolved?
     *
     * @param parameterContext parameter context
     * @param extensionContext extension context
     * @return true if the {@code parameterContext} is annotated with {@link H2Database}
     */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return h2Extension.supportsParameter(parameterContext, extensionContext);
    }

    /**
     * Resolve the parameter annotated with {@link H2Database} into a {@link H2FileBasedDatabase}.
     *
     * @param parameterContext parameter context
     * @param extensionContext extension context
     * @return the resolved {@link H2FileBasedDatabase}
     */
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return h2Extension.resolveParameter(parameterContext, extensionContext);
    }

    public File getDirectory() {
        return this.getDatabase().getDirectory();
    }

    public String getUrl() {
        return this.getDatabase().getUrl();
    }

    public DataSource getDataSource() {
        return this.getDatabase().getDataSource();
    }
}
