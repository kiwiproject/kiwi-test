package org.kiwiproject.test.dropwizard.app;

import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
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
import org.postgresql.Driver;

/**
 * Multipurpose extension for Dropwizard {@link Application} testing requiring a PostgreSQL database. The extension
 * will spin up an embedded PostgreSQL database instance, run the migrations and then configure the
 * {@link DropwizardAppExtension} with the database information.
 * <p>
 * There are several things to note when using this extension:
 * <ul>
 *     <li>
 *         The embedded PostgreSQL extension supports both Flyway and Liquibase, but we are only supporting Liquibase
 *         currently.
 *     </li>
 *     <li>
 *         The application's {@link Configuration} class must have a Dropwizard
 *         {@link io.dropwizard.db.DataSourceFactory DataSourceFactory}. By default, the property name is expected to
 *         be "database", i.e., the property will either have {@code @JsonProperty("database")} or be named "database",
 *         but you can change this using the alternate constructor.
 *      </li>
 *      <li>
 *          The YAML configuration file provided to this extension can contain a
 *          {@link io.dropwizard.db.DataSourceFactory DataSourceFactory} property with custom values, but this extension
 *          always overrides the following properties: {@code driverClass}, {@code user}, and {@code url} because the
 *          driver is always Postgres, and the user and url are provided by the embedded Postgres database.
 *      </li>
 *      <li>
 *          You should <strong>not</strong> register {@code DropwizardExtensionsSupport} or the embedded Postgres
 *          extension. See the WARNING below.
 *      </li>
 * </ul>
 * <p>
 * To include this extension in an Application test, add the following at the top of the class:
 * <pre>
 * {@literal @}RegisterExtension
 *  public static PostgresAppTestExtension&lt;AppConfiguration&gt; POSTGRES_APP =
 *      new PostgresAppTestExtension("migrations.xml", "config.yml", App.class);
 * </pre>
 * Here is a one of the simplest {@code config.yml} files you can have:
 * <pre>
 * ---
 * server:
 *  type: simple
 * </pre>
 * Note that you do <strong>not</strong> need to declare a database property because this extension overrides the
 * required properties. But you can add other custom properties to the configuration if desired:
 * <pre>
 * ---
 * server:
 *  type: simple
 *
 * database:
 *   initialSize: 1
 *   minSize: 1
 *   maxSize: 5
 * </pre>
 * <p>
 * Most of the time these won't really matter in unit/integration testing scenarios. Next is a fragment of a sample
 * Configuration class that uses the default property name:
 * <pre>
 * {@literal @}JsonProperty("database")
 *  private DataSourceFactory dataSourceFactory = new DataSourceFactory();
 * </pre>
 * The extension assumes the configuration uses {@code "database"} as the name of the DataSourceFactory property, as
 * shown in the above examples which specify the name via the {@code JsonProperty} annotation. If the property in
 * your configuration is named something else, you can use the alternate constructor which accepts a custom property
 * name. For example:
 * <pre>
 *  // Uses "db" as the name of the DataSourceFactory in the configuration
 * {@literal @}RegisterExtension
 *  public static PostgresAppTestExtension&lt;AppConfiguration&gt; POSTGRES_APP =
 *      new PostgresAppTestExtension("migrations.xml", "config.yml", App.class, "db");
 * </pre>
 * <p>
 * The test extension instance can access the application and the PostgreSQL instance by calling
 * {@code POSTGRES_APP.getApp()} and {@code POSTGRES_APP.getPostgres()} respectively.
 * <p>
 * You can also provide one or more {@link ConfigOverride} values to the extension:
 * <pre>
 * {@literal @}RegisterExtension
 *  public static PostgresAppTestExtension&lt;AppConfiguration&gt; POSTGRES_APP =
 *      new PostgresAppTestExtension("migrations.xml", "config.yml", App.class,
 *          ConfigOverride.config("someValue", "42"),
 *          ConfigOverride.config("aLazyValue", () -&gt; calculateTheLazyValue()));
 * </pre>
 * <p>
 * <strong>WARNING</strong>: When using this extension you should <strong>not</strong> register
 * {@code DropwizardExtensionsSupport} or the embedded Postgres extension since this extension programmatically
 * registers both of them. Doing so will almost certainly result in unexpected behavior such as
 * {@code NullPointerException}s being thrown.
 * <p>
 * For information on how the embedded PostgreSQL extension works see:
 * <a href="https://github.com/zonkyio/embedded-postgres">https://github.com/zonkyio/embedded-postgres</a>
 * <br>
 * For information on how the dropwizard app extension works see:
 * <a href="https://www.dropwizard.io/en/latest/manual/testing.html#junit-5">https://www.dropwizard.io/en/latest/manual/testing.html#junit-5</a>
 *
 * @param <T> the {@link Configuration} implementation for the application
 */
@Getter
public class PostgresAppTestExtension<T extends Configuration> implements BeforeAllCallback, AfterAllCallback {

    private static final String DEFAULT_DATASOURCE_FACTORY_PROPERTY = "database";

    private final PreparedDbExtension postgres;
    private final DropwizardAppExtension<T> app;

    /**
     * Create a new instance using the default property name ("database") for the DataSourceFactory.
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

        this(migrationClasspathLocation, configFileName, appClass, DEFAULT_DATASOURCE_FACTORY_PROPERTY, configOverrides);
    }

    /**
     * Create a new instance with a custom property name for the DataSourceFactory.
     *
     * @param migrationClasspathLocation    the classpath location of the Liquibase migrations file to use
     * @param configFileName                the name of the classpath resource to use as the application
     *                                      configuration file
     * @param appClass                      the Dropwizard application class
     * @param dataSourceFactoryPropertyName the name of the DataSourceFactory property in the Configuration class,
     *                                      i.e., what is specified in the YAML configuration
     * @param configOverrides               optional configuration override values
     */
    public PostgresAppTestExtension(String migrationClasspathLocation,
                                    String configFileName,
                                    Class<? extends Application<T>> appClass,
                                    String dataSourceFactoryPropertyName,
                                    ConfigOverride... configOverrides) {

        var liquibasePreparer = LiquibasePreparer.forClasspathLocation(migrationClasspathLocation);
        postgres = EmbeddedPostgresExtension.preparedDatabase(liquibasePreparer);

        var postgresConfigOverrides = buildPostgresConfigOverrides(dataSourceFactoryPropertyName);
        var combinedConfigOverrides = ArrayUtils.addAll(postgresConfigOverrides, configOverrides);

        app = new DropwizardAppExtension<>(
                appClass,
                ResourceHelpers.resourceFilePath(configFileName),
                combinedConfigOverrides);
    }

    private ConfigOverride[] buildPostgresConfigOverrides(String dataSourceFactoryPropertyName) {
        var dbUserProperty = dataSourceFactoryPropertyName + ".user";
        var dbUrlProperty = dataSourceFactoryPropertyName + ".url";
        var dbDriverClassProperty = dataSourceFactoryPropertyName + ".driverClass";

        var userConfigOverride = ConfigOverride.config(dbUserProperty, () -> postgres.getConnectionInfo().getUser());
        var urlConfigOverride = ConfigOverride.config(dbUrlProperty,
                () -> "jdbc:postgresql://localhost:" + postgres.getConnectionInfo().getPort()
                        + "/" + postgres.getConnectionInfo().getDbName());
        var driverConfigOverride = ConfigOverride.config(dbDriverClassProperty, Driver.class.getName());

        return new ConfigOverride[] { userConfigOverride, urlConfigOverride, driverConfigOverride };
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
