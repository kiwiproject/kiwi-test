package org.kiwiproject.test.dropwizard.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * @implNote Due to several known issues in the zonkyio / embedded-postgres library on macOS Catalina,
 * we are enabling this only on Linux. See https://github.com/zonkyio/embedded-postgres/issues/32
 * and https://github.com/zonkyio/embedded-postgres/issues/40 for more details.
 */
@DisplayName("PostgresAppTestExtension")
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

    private final static PostgresAppTestExtension<Config> EXTENSION = new PostgresAppTestExtension<>("PostgresAppTestExtensionTest/test-migrations.xml",
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
