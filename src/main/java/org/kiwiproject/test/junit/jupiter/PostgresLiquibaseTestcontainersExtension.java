package org.kiwiproject.test.junit.jupiter;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.annotations.VisibleForTesting;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.test.jdbc.JdbcTests;
import org.kiwiproject.test.jdbc.SimpleSingleConnectionDataSource;
import org.kiwiproject.test.liquibase.LiquibaseTestHelpers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.function.UnaryOperator;

/**
 * A JUnit Jupiter extension for tests that need a Postgres database with Liquibase migrations applied
 * before running.
 * <p>
 * This extension uses a Testcontainers {@link PostgreSQLContainer} but always returns a
 * {@link SimpleSingleConnectionDataSource}. This ensures that classes under test all use the same
 * JDBC {@code Connection} which is useful when the test code executes in a transaction, and thus
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
 *     static final PostgresLiquibaseTestcontainersExtension DATABASE_EXTENSION =
 *             PostgresLiquibaseTestcontainersExtension.builder()
 *                     .changelogPath("migrations.xml")
 *                     .build();
 *
 *    {@literal @}RegisterExtension
 *     final Jdbi3Extension jdbi3Extension =  Jdbi3Extension.builder()
 *             .dataSource(DATABASE_EXTENSION.getTestDataSource())
 *             .plugin(new PostgresPlugin())
 *             .build();
 *
 *     // ...tests...
 * }
 * </pre>
 * Note in the above that the {@link PostgresLiquibaseTestcontainersExtension} is declared both static and final,
 * while the {@link Jdbi3Extension} is not static but is final. This configuration ensures that the Testcontainers
 * Postgres database is set up only one time before all tests run. The {@link Jdbi3Extension} is initialized before
 * each test with a new transaction that is rolled back after each test. This ensures code running in the
 * transaction participates in the same transaction and can see uncommitted data, but also ensures no data is
 * actually committed since the transaction is rolled back. Each test, therefore, does not need to worry about
 * cleaning up any data from previous tests.
 * <p>
 * By default, this extension uses a standard Postgres Docker image, but you can supply any Postgres-compatible
 * image, for example, one that includes PostGIS support:
 * <pre>
 * class MyPostGISDatabaseTest {
 *
 *    {@literal @}RegisterExtension
 *     static final PostgresLiquibaseTestcontainersExtension DATABASE_EXTENSION =
 *             PostgresLiquibaseTestcontainersExtension.builder()
 *                     .imageName(DockerImageName.parse("postgis/postgis:18-3.6"))
 *                     .changelogPath("migrations.xml")
 *                     .build();
 *
 *     // ...tests...
 * }
 * </pre>
 *
 * @see PostgreSQLContainer
 * @see SimpleSingleConnectionDataSource
 * @see DockerImageName
 */
@Slf4j
public class PostgresLiquibaseTestcontainersExtension implements BeforeAllCallback, AfterAllCallback {

    /**
     * The default classpath location of the Liquibase changelog file used when no {@code changelogPath}
     * is provided.
     */
    public static final String DEFAULT_CHANGELOG_PATH = "migrations.xml";

    /**
     * The name of the system property that can be used to specify the Postgres Docker image to use, when
     * no {@code imageName} is provided.
     */
    public static final String POSTGRES_IMAGE_SYSTEM_PROPERTY = "kiwi.test.postgres.image";

    /**
     * The name of the environment variable that can be used to specify the Postgres Docker image to use, when
     * no {@code imageName} is provided and {@link #POSTGRES_IMAGE_SYSTEM_PROPERTY} is not set.
     */
    public static final String POSTGRES_IMAGE_ENV_VAR = "KIWI_TEST_POSTGRES_IMAGE";

    /**
     * The default Postgres Docker image used when no {@code imageName} is provided, and neither
     * {@link #POSTGRES_IMAGE_SYSTEM_PROPERTY} nor {@link #POSTGRES_IMAGE_ENV_VAR} is set.
     */
    public static final DockerImageName DEFAULT_POSTGRES_IMAGE =
            DockerImageName.parse("postgres:18-alpine");

    /**
     * The Postgres Docker image used by this extension, resolved from the provided {@code imageName}, the
     * {@link #POSTGRES_IMAGE_SYSTEM_PROPERTY} system property, the {@link #POSTGRES_IMAGE_ENV_VAR} environment
     * variable, or {@link #DEFAULT_POSTGRES_IMAGE}, in that order.
     */
    @Getter
    private final DockerImageName imageName;

    /**
     * The classpath location of the Liquibase changelog file used by this extension.
     */
    @Getter
    private final String changelogPath;

    /**
     * The Testcontainers Postgres container. Generally you should not need this directly when using this
     * extension, but we are exposing it just in case and because this is a testing library, not a production API.
     */
    @Getter
    private PostgreSQLContainer postgresContainer;

    /**
     * The test DataSource, which is a {@link SimpleSingleConnectionDataSource}.
     */
    @Getter
    private SimpleSingleConnectionDataSource testDataSource;

    /**
     * Construct a new instance using the given Postgres Docker image and classpath location of a Liquibase
     * changelog file.
     *
     * @param imageName     the Postgres Docker image to use; may be {@code null} to resolve a default (see
     *                      {@link #POSTGRES_IMAGE_SYSTEM_PROPERTY}, {@link #POSTGRES_IMAGE_ENV_VAR}, and
     *                      {@link #DEFAULT_POSTGRES_IMAGE})
     * @param changelogPath the classpath location of a Liquibase changelog file; may be {@code null} to use
     *                      {@link #DEFAULT_CHANGELOG_PATH}
     */
    @Builder
    private PostgresLiquibaseTestcontainersExtension(DockerImageName imageName,
                                                     String changelogPath) {
        this.imageName = resolveDefaultImage(imageName, System::getProperty, System::getenv);
        this.changelogPath = resolveChangelogPath(changelogPath);
    }

    @VisibleForTesting
    static DockerImageName resolveDefaultImage(DockerImageName imageName,
                                               UnaryOperator<String> systemPropertyLookup,
                                               UnaryOperator<String> envVarLookup) {

        if (nonNull(imageName)) {
            LOG.trace("Using provided image name: {}", imageName);
            return imageName;
        }

        var systemProperty = systemPropertyLookup.apply(POSTGRES_IMAGE_SYSTEM_PROPERTY);
        if (isNotBlank(systemProperty)) {
            LOG.trace("Resolved Postgres image from system property {}: {}", POSTGRES_IMAGE_SYSTEM_PROPERTY, systemProperty);
            return DockerImageName.parse(systemProperty);
        }

        var envVar = envVarLookup.apply(POSTGRES_IMAGE_ENV_VAR);
        if (isNotBlank(envVar)) {
            LOG.trace("Resolved Postgres image from env var {}: {}", POSTGRES_IMAGE_ENV_VAR, envVar);
            return DockerImageName.parse(envVar);
        }

        LOG.trace("Using default Postgres image: {}", DEFAULT_POSTGRES_IMAGE);
        return DEFAULT_POSTGRES_IMAGE;
    }

    private static String resolveChangelogPath(String changelogPath) {
        if (isNotBlank(changelogPath)) {
            LOG.trace("Using provided changelog path: {}", changelogPath);
            return changelogPath;
        }

        LOG.trace("Using default changelog path: {}", DEFAULT_CHANGELOG_PATH);
        return DEFAULT_CHANGELOG_PATH;
    }

    /**
     * Creates a new instance using the default Postgres Docker image and the default changelog path.
     *
     * @return a new instance with all default settings
     * @see #DEFAULT_POSTGRES_IMAGE
     * @see #DEFAULT_CHANGELOG_PATH
     */
    public static PostgresLiquibaseTestcontainersExtension ofDefaults() {
        return builder().build();
    }

    /**
     * Start the Testcontainers Postgres container, initialize a test DataSource that connects to it, and run
     * Liquibase migrations. This test DataSource should be used in your tests to get a Connection.
     *
     * @param context the extension context
     * @throws Exception if any error occurs while starting the container, connecting to it, or running migrations
     */
    @Override
    public void beforeAll(@NonNull ExtensionContext context) throws Exception {
        LOG.trace("Creating new PostgreSQLContainer with image {}", imageName);
        postgresContainer = new PostgreSQLContainer(
                imageName.asCompatibleSubstituteFor("postgres"));
        postgresContainer.start();

        LOG.trace("Initializing new single-connection test DataSource for URL {}",
                postgresContainer.getJdbcUrl());
        testDataSource = buildDataSource(postgresContainer);

        LOG.trace("Running Liquibase migrations in {}", changelogPath);
        LiquibaseTestHelpers.runMigrations(testDataSource, changelogPath);
    }

    private static SimpleSingleConnectionDataSource buildDataSource(PostgreSQLContainer postgres) {
        return JdbcTests.newTestDataSource(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
    }

    /**
     * Closes the Connection provided by the test DataSource, then stops the Testcontainers Postgres container.
     *
     * @param context the extension context
     */
    @Override
    public void afterAll(@NonNull ExtensionContext context) {
        if (nonNull(testDataSource)) {
            LOG.trace("Closing test DataSource");
            testDataSource.close();
        }

        if (nonNull(postgresContainer)) {
            LOG.trace("Stopping PostgreSQLContainer");
            postgresContainer.stop();
        }
    }
}
