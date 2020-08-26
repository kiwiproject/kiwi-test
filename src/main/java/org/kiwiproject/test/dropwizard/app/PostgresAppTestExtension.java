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
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Multi-purpose extension for {@link Application} testing requiring a postgres database. The extension will spin up an
 * embedded postgres database instance, run the migrations and then configure the {@link DropwizardAppExtension} with the
 * database information.
 * <p>
 * To include this extension in an AppTest then add the following at the top of the class:
 * <p>
 * {@code @RegisterExtension}
 * <br>
 * {@code public static PostgresAppTestExtension<AppConfiguration> APP = new PostgresAppTestExtension("migrations.xml", "config.yml", App.class);}
 * <p>
 * The test instance will have access to the application and the postgres instance by calling {@code APP.getApp()} and {@code APP.getPostgres()}
 * respectively.
 * <p>
 * For information on how the embedded postgres extension works see: https://github.com/zonkyio/embedded-postgres
 * <br>
 * For information on how the dropwizard app extension works see: https://www.dropwizard.io/en/latest/manual/testing.html#junit-5
 *
 * @param <T> the {@link Configuration} implementation for the application
 */
@Getter
public class PostgresAppTestExtension<T extends Configuration> implements BeforeAllCallback, AfterAllCallback {

    private final PreparedDbExtension postgres;
    private final DropwizardAppExtension<T> app;

    public PostgresAppTestExtension(String migrationClasspathLocation, String configFileName, Class<? extends Application<T>> appClass) {
        postgres = EmbeddedPostgresExtension.preparedDatabase(LiquibasePreparer.forClasspathLocation(migrationClasspathLocation));
        app = new DropwizardAppExtension<>(appClass,
                ResourceHelpers.resourceFilePath(configFileName),
                ConfigOverride.config("database.user", () -> postgres.getConnectionInfo().getUser()),
                ConfigOverride.config("database.url",
                        () -> "jdbc:postgresql://localhost:" + postgres.getConnectionInfo().getPort()
                                + "/" + postgres.getConnectionInfo().getDbName()));
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
