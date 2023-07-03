package org.kiwiproject.test.junit.jupiter;

import static java.util.Objects.nonNull;
import static org.kiwiproject.base.KiwiStrings.f;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.test.jdbc.JdbcTests;
import org.kiwiproject.test.jdbc.SimpleSingleConnectionDataSource;

/**
 * This JUnit Jupiter extension uses the Embedded Postgres Jupiter extension but always returns a
 * {@link SimpleSingleConnectionDataSource}. This ensures that classes under test all use the same
 * JDBC {@code Connection} which is useful when the test code executes in a transaction and thus
 * all those classes can all see the uncommitted data.
 * <p>
 * When registering this extension, you must use a {@code static} field because both {@link BeforeAllCallback}
 * and {@link AfterAllCallback} must be registered at the class level.
 * <p>
 * Example usage showing this extension used to supply the {@code DataSource} for a {@link Jdbi3Extension}:
 * <pre>
 * class MyDatabaseTest {
 *
 *    {@literal @}RegisterExtension
 *     static final PostgresLiquibaseTestExtension DATABASE_EXTENSION =
 *             new PostgresLiquibaseTestExtension("migrations.xml");
 *
 *    {@literal @}RegisterExtension
 *     final Jdbi3Extension jdbi3Extension =  Jdbi3Extension.builder()
 *             .dataSource(DATABASE_EXTENSION.getDataSource())
 *             .plugin(new PostgresPlugin())
 *             .build();
 *
 *     // ...tests...
 * }
 * </pre>
 * Note in the above that the {@link PostgresLiquibaseTestExtension} is declared both static and final, while the
 * {@link Jdbi3Extension} is not static but is final. This configuration ensures that the embedded Postgres database
 * is set up only one time before all tests run. The {@link Jdbi3Extension} is initialized before each test with
 * a new transaction that is rolled back after each test, which again ensures code running in the transaction
 * participates in the same transaction and can see uncommitted data, but also ensures no data is actually committed
 * since the transaction is rolled back. Each test therefore does not need to worry about cleaning up any data from
 * previous tests.
 *
 * @see PreparedDbExtension
 * @see SimpleSingleConnectionDataSource
 */
@Slf4j
public class PostgresLiquibaseTestExtension implements BeforeAllCallback, AfterAllCallback {

    private static final String EMPTY_PASSWORD = "";

    /**
     * The embedded Postgres extension. Generally you should not need this directly when using this extension, but
     * we are exposing it just in case and because this is a testing library, not a production API.
     */
    @Getter
    private final PreparedDbExtension postgres;

    /**
     * The test DataSource, which is a {@link SimpleSingleConnectionDataSource}.
     */
    @Getter
    private SimpleSingleConnectionDataSource testDataSource;

    /**
     * Construct a new instance using the given classpath location of a Liquibase migrations file.
     *
     * @param migrationClassPathLocation classpath location of Liquibase migrations file
     */
    public PostgresLiquibaseTestExtension(String migrationClassPathLocation) {
        LOG.warn("Constructing new instance for migration path: {}", migrationClassPathLocation);  // TODO revert back to TRACE

        var liquibasePreparer = LiquibasePreparer.forClasspathLocation(migrationClassPathLocation);
        postgres = EmbeddedPostgresExtension.preparedDatabase(liquibasePreparer);
    }

    /**
     * Start the embedded Postgres extension and initialize a test DataSource that connects to it. This test
     * DataSource should be used in your tests to obtain a Connection.
     *
     * @param context the extension context
     * @throws Exception if any error occurs initializing the embedded Postgres or connecting to it
     */
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        LOG.warn("Invoke PreparedDbExtension.beforeAll() to initialize the embedded Postgres");  // TODO revert back to TRACE
        postgres.beforeAll(context);

        var connectionInfo = postgres.getConnectionInfo();
        var url = f("jdbc:postgresql://localhost:{}/{}", connectionInfo.getPort(), connectionInfo.getDbName());
        var user = connectionInfo.getUser();
        LOG.warn("Initializing new single-connection test DataSource for URL {} and user {}", url, user);  // TODO revert back to TRACE
        testDataSource = JdbcTests.newTestDataSource(url, user, EMPTY_PASSWORD);
    }

    /**
     * Closes the Connection provided by the test DataSource, then shuts down the embedded Postgres.
     *
     * @param context the extension context
     */
    @Override
    public void afterAll(ExtensionContext context) {
        if (nonNull(testDataSource)) {
            LOG.warn("Closing test DataSource");  // TODO revert back to TRACE
            testDataSource.close();
        }

        LOG.warn("Invoke PreparedDbExtension.afterAll() to shut down the embedded Postgres");  // TODO revert back to TRACE
        postgres.afterAll(context);
    }
}
