package org.kiwiproject.test.dropwizard.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ConfigOverride;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Disabled
@DisplayName("PostgresAppTestExtension: ConfigOverrides")
class PostgresAppTestExtensionConfigOverridesTest {

    @Getter
    @Setter
    static class Config extends Configuration {
        @Valid
        @NotNull
        @JsonProperty("database")
        private DataSourceFactory dataSourceFactory = new DataSourceFactory();

        private String sdkVersionOverride;

        @NotBlank
        private String registryType = "DEFAULT";

        @NotBlank
        private String registryUrls = "http://eureka-server:8761/eureka";

        @NotBlank
        private String serviceName = "some-service";
    }

    public static class App extends Application<Config> {
        @Override
        public void run(Config config, Environment environment) {
            // Nothing to really do
        }
    }

    private static final PostgresAppTestExtension<Config> EXTENSION = new PostgresAppTestExtension<>(
            "PostgresAppTestExtensionTest/test-migrations.xml",
            "PostgresAppTestExtensionTest/test-config.yml",
            App.class,
            ConfigOverride.config("sdkVersionOverride", "1.0.0-SNAPSHOT"),
            ConfigOverride.config("registryType", "NOOP"),
            ConfigOverride.config("registryUrls", "http://localhost:8761/eureka"),
            ConfigOverride.config("serviceName", "test-service"));

    private static ExtensionContext mockContext;

    @BeforeAll
    static void setupAndStartExtension() throws Exception {
        mockContext = mock(ExtensionContext.class);
        EXTENSION.beforeAll(mockContext);
    }

    @AfterAll
    static void stopExtension() {
        EXTENSION.afterAll(mockContext);
    }

    @Test
    void shouldStartPostgresAndApp() {
        var appExtension = EXTENSION.getApp();
        assertThat(appExtension).isNotNull();
        assertThat(appExtension.getTestSupport().getEnvironment()).isNotNull();

        var preparedDbExtension = EXTENSION.getPostgres();
        assertThat(preparedDbExtension).isNotNull();
        assertThat(preparedDbExtension.getTestDatabase()).isNotNull();
    }

    @Test
    void shouldSetConfigOverrideValues() {
        var configuration = EXTENSION.getApp().getConfiguration();
        assertThat(configuration.getSdkVersionOverride()).isEqualTo("1.0.0-SNAPSHOT");
        assertThat(configuration.getRegistryType()).isEqualTo("NOOP");
        assertThat(configuration.getRegistryUrls()).isEqualTo("http://localhost:8761/eureka");
        assertThat(configuration.getServiceName()).isEqualTo("test-service");
    }
}
