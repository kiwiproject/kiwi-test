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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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

    @Test
    void shouldStartPostgresAndApp() throws Exception {
        var extension = new PostgresAppTestExtension<>("PostgresAppTestExtensionTest/test-migrations.xml",
                "PostgresAppTestExtensionTest/test-config.yml",
                App.class);

        extension.beforeAll(mock(ExtensionContext.class));

        assertThat(extension.getApp()).isNotNull();
        assertThat(extension.getApp().getTestSupport().getEnvironment()).isNotNull();
        assertThat(extension.getPostgres()).isNotNull();
        assertThat(extension.getPostgres().getTestDatabase()).isNotNull();
    }
}
