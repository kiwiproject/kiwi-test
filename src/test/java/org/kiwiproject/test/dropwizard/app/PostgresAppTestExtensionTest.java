package org.kiwiproject.test.dropwizard.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.test.junit.jupiter.ResetLogbackLoggingExtension;

@DisplayName("PostgresAppTestExtension")
@ExtendWith(ResetLogbackLoggingExtension.class)
class PostgresAppTestExtensionTest {

    @Getter
    @Setter
    static class Config extends Configuration {
        @Valid
        @NotNull
        @JsonProperty("database")
        private DataSourceFactory dataSourceFactory = new DataSourceFactory();
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
            App.class);

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
        assertThat(EXTENSION.getApp()).isNotNull();
        assertThat(EXTENSION.getApp().getTestSupport().getEnvironment()).isNotNull();
        assertThat(EXTENSION.getPostgres()).isNotNull();
        assertThat(EXTENSION.getPostgres().getTestDatabase()).isNotNull();
    }
}
