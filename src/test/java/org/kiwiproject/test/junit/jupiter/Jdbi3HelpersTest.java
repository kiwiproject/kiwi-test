package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.ConnectionFactory;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kiwiproject.test.h2.H2FileBasedDatabase;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@DisplayName("Jdbi3Helpers")
@ExtendWith(H2FileBasedDatabaseExtension.class)
@Slf4j
class Jdbi3HelpersTest {

    @Nested
    class DatabaseTypeEnum {

        @Test
        void shouldGetH2PluginInstance() {
            var pluginOptional = Jdbi3Helpers.DatabaseType.pluginFromDatabaseUrl("jdbc:h2:/tmp/h2-test-db-12345/testdb");
            assertThat(pluginOptional).containsInstanceOf(H2DatabasePlugin.class);
        }

        @Test
        void shouldGetPostgresPluginInstance() {
            var pluginOptional = Jdbi3Helpers.DatabaseType.pluginFromDatabaseUrl("jdbc:postgresql://localhost:5432/testdb");
            assertThat(pluginOptional).containsInstanceOf(PostgresPlugin.class);
        }

        @Test
        void shouldReturnEmptyOptionalForUnsupportedDatabase() {
            assertThat(Jdbi3Helpers.DatabaseType.pluginFromDatabaseUrl("jdbc:mysql://localhost:33060/sakila"))
                    .isEmpty();
        }
    }

    @Nested
    class GetPluginInstance {

        @Test
        void shouldReturnOptionalContainingPlugin_WhenClassIsAvailable() {
            assertThat(Jdbi3Helpers.getPluginInstance(PostgresPlugin.class.getName()))
                    .containsInstanceOf(PostgresPlugin.class);

            assertThat(Jdbi3Helpers.getPluginInstance(H2DatabasePlugin.class.getName()))
                    .containsInstanceOf(H2DatabasePlugin.class);
        }

        @Test
        void shouldReturnEmptyOptional_WhenClassISNotAvailable() {
            assertThat(Jdbi3Helpers.getPluginInstance("org.jdbi.v3.mysql.MysqlPlugin"))
                    .isEmpty();
        }

        @Test
        void shouldReturnEmptyOptional_WhenErrorInstantiatingPluginClass() {
            assertThat(Jdbi3Helpers.getPluginInstance(MisbehavingDatabasePlugin.class.getName()))
                    .isEmpty();
        }
    }

    public static class MisbehavingDatabasePlugin extends JdbiPlugin.Singleton {
        public MisbehavingDatabasePlugin() {
            throw new IllegalStateException("I cannot be created");
        }
    }

    @Nested
    class BuildJdbi {

        @Test
        void shouldThrow_WhenNoArgumentsProvided() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Jdbi3Helpers.buildJdbi(null, null, null, null, null, List.of()));
        }

        @Test
        void shouldAcceptConnectionFactory(@H2Database H2FileBasedDatabase database) {
            var connectionFactory = new DataSourceConnectionFactory(database.getDataSource());
            var jdbi = Jdbi3Helpers.buildJdbi(null, connectionFactory, null, null, null, List.of());
            assertThat(jdbi).isNotNull();
            assertCanExecuteQuery(jdbi);
        }

        @Test
        void shouldAcceptJdbcConnectionProperties(@H2Database H2FileBasedDatabase database) {
            var jdbi = Jdbi3Helpers.buildJdbi(null, null, database.getUrl(), "", "", List.of());
            assertThat(jdbi).isNotNull();
            assertCanExecuteQuery(jdbi);
        }

        @SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
        private void assertCanExecuteQuery(Jdbi jdbi) {
            try (var handle = jdbi.open()) {
                var count = handle.createQuery("select count(*) as cnt from test_table")
                        .mapTo(Integer.class)
                        .findOne()
                        .orElseThrow();
                assertThat(count).isZero();
            }
        }
    }

    @Nested
    class ConfigureSqlLogger {

        private Jdbi jdbi;

        @BeforeEach
        void setUp() {
            jdbi = mock(Jdbi.class);
        }

        @Test
        void shouldUseDefaultLoggerName_WhenGivenBlankLoggerName() {
            var jdbiSqlLogger = Jdbi3Helpers.configureSqlLogger(jdbi, "");
            assertThat(jdbiSqlLogger.getLoggerName()).isEqualTo(Jdbi.class.getName());

            verify(jdbi).setSqlLogger(jdbiSqlLogger);
        }

        @Test
        void shouldUseGivenLoggerName() {
            var loggerName = "My Jdbi Logger";
            var jdbiSqlLogger = Jdbi3Helpers.configureSqlLogger(jdbi, loggerName);
            assertThat(jdbiSqlLogger.getLoggerName()).isEqualTo(loggerName);

            verify(jdbi).setSqlLogger(jdbiSqlLogger);
        }
    }

    @AllArgsConstructor
    static class DataSourceConnectionFactory implements ConnectionFactory {
        private final DataSource dataSource;

        @Override
        public Connection openConnection() throws SQLException {
            return dataSource.getConnection();
        }
    }
}