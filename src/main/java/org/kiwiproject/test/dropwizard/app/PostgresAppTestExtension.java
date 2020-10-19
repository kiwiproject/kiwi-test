package org.kiwiproject.test.dropwizard.app;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Multi-purpose extension for {@link Application} testing requiring a PostgreSQL database. The extension will spin up an
 * embedded PostgreSQL database instance, run the migrations and then configure the {@link DropwizardAppExtension} with the
 * database information.
 * <p>
 * Note: The embedded PostgreSQL extension supports both Flyway and Liquibase, but we are assuming migrations are Liquibase.
 * <p>
 * To include this extension in an AppTest then add the following at the top of the class:
 * <p>
 * <pre>
 * {@literal @}RegisterExtension
 *  public static PostgresAppTestExtension&lt;AppConfiguration&gt; POSTGRES_APP =
 *      new PostgresAppTestExtension("migrations.xml", "config.yml", App.class);
 * </pre>
 * <p>
 * The test instance will have access to the application and the PostgreSQL instance by calling
 * {@code POSTGRES_APP.getApp()} and {@code POSTGRES_APP.getPostgres()} respectively.
 * <p>
 * You can also provide one or more {@link ConfigOverride} values to the extension:
 * <pre>
 * {@literal @}RegisterExtension
 *  public static PostgresAppTestExtension<AppConfiguration> POSTGRES_APP =
 *      new PostgresAppTestExtension("migrations.xml", "config.yml", App.class,
 *          ConfigOverride.config("someValue", "42"),
 *          ConfigOverride.config("aLazyValue", () -> calculateTheLazyValue()));
 * </pre>
 * <p>
 * WARNING: When using this extension you should <strong>not</strong> register {@code DropwizardExtensionsSupport} or
 * the embedded Postgres extension since this extension programmatically registers both of them. Doing so will almost
 * certainly result in unexpected behavior such as {@code NullPointerException}s being thrown.
 * <p>
 * For information on how the embedded PostgreSQL extension works see: https://github.com/zonkyio/embedded-postgres
 * <br>
 * For information on how the dropwizard app extension works see: https://www.dropwizard.io/en/latest/manual/testing.html#junit-5
 *
 * @param <T> the {@link Configuration} implementation for the application
 */
@Getter
public class PostgresAppTestExtension<T extends Configuration> implements BeforeAllCallback, AfterAllCallback {

    private final PreparedDbExtension postgres;
    private final DropwizardAppExtension<T> app;

    /**
     * Create a new instance.
     *
     * @param migrationClasspathLocation the classpath location of the Liquibase migrations file to use
     * @param configFileName             the name of the classpath resource to use as the application configuration file
     * @param appClass                   the Dropwizard application class
     * @param configOverrides            optional configuration override values
     */
    public PostgresAppTestExtension(String migrationClasspathLocation,
                                    String configFileName,
                                    Class<? extends Application<T>> appClass,
                                    ConfigOverride... configOverrides) {

        var liquibasePreparer = LiquibasePreparer.forClasspathLocation(migrationClasspathLocation);
        postgres = EmbeddedPostgresExtension.preparedDatabase(liquibasePreparer);

        var userConfigOverride = ConfigOverride.config("database.user", () -> postgres.getConnectionInfo().getUser());
        var urlConfigOverride = ConfigOverride.config("database.url",
                () -> "jdbc:postgresql://localhost:" + postgres.getConnectionInfo().getPort()
                        + "/" + postgres.getConnectionInfo().getDbName());

        var postgresConfigOverrides = new ConfigOverride[]{userConfigOverride, urlConfigOverride};
        var combinedConfigOverrides = ArrayUtils.addAll(postgresConfigOverrides, configOverrides);

        app = new DropwizardAppExtension<>(
                appClass,
                ResourceHelpers.resourceFilePath(configFileName),
                combinedConfigOverrides);
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        postgres.beforeAll(extensionContext);
        app.before();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        app.after();
        postgres.afterAll(extensionContext);
    }
}
