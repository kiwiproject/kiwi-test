package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.kiwiproject.test.h2.H2FileBasedDatabase;
import org.kiwiproject.test.junit.jupiter.params.provider.BlankStringArgumentsProvider;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.logging.SLF4JLog;
import org.skife.jdbi.v2.tweak.ConnectionFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@DisplayName("DropwizardJdbi2Helpers")
@ExtendWith(H2FileBasedDatabaseExtension.class)
@Slf4j
class DropwizardJdbi2HelpersTest {

    @Nested
    class BuildDBI {

        @Test
        void shouldThrow_WhenNoArgumentsProvided() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> DropwizardJdbi2Helpers.buildDBI(null, null, null, null, null));
        }

        @Test
        void shouldAcceptConnectionFactory(@H2Database H2FileBasedDatabase database) {
            var connectionFactory = new DataSourceConnectionFactory(database.getDataSource());
            var dbi = DropwizardJdbi2Helpers.buildDBI(null, connectionFactory, null, null, null);
            assertThat(dbi).isNotNull();
            assertCanGetExecuteQuery(dbi);
        }

        @Test
        void shouldAcceptJdbcConnectionProperties(@H2Database H2FileBasedDatabase database) {
            var dbi = DropwizardJdbi2Helpers.buildDBI(null, null, database.getUrl(), "", "");
            assertThat(dbi).isNotNull();
            assertCanGetExecuteQuery(dbi);
        }

        private void assertCanGetExecuteQuery(DBI dbi) {
            try (var handle = dbi.open()) {
                var count = handle.createQuery("select count(*) as cnt from test_table")
                        .mapTo(Integer.class)
                        .first();
                assertThat(count).isZero();
            }
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

    @Nested
    class Slf4jLoggerName {

        @ParameterizedTest
        @ArgumentsSource(BlankStringArgumentsProvider.class)
        void shouldReturnDefault_WhenGivenBlank(String value) {
            assertThat(DropwizardJdbi2Helpers.slf4jLoggerName(value)).isEqualTo(DBI.class.getName());
        }
    }

    @Nested
    class Slf4jLogLevel {

        @Test
        void shouldReturnTRACE_WhenGivenNull() {
            assertThat(DropwizardJdbi2Helpers.slf4jLogLevel(null)).isEqualTo(SLF4JLog.Level.TRACE);
        }
    }

}
