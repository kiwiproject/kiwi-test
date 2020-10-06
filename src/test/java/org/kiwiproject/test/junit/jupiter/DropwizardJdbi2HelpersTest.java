package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.kiwiproject.test.h2.H2DatabaseTestHelper;
import org.kiwiproject.test.h2.H2FileBasedDatabase;
import org.kiwiproject.test.junit.jupiter.params.provider.BlankStringArgumentsProvider;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.logging.SLF4JLog;
import org.skife.jdbi.v2.tweak.ConnectionFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@DisplayName("DropwizardJdbi2Helpers")
@Slf4j
class DropwizardJdbi2HelpersTest {

    private static H2FileBasedDatabase database;

    @BeforeAll
    static void beforeAll() {
        LOG.trace("Create H2 file-based database");
        database = H2DatabaseTestHelper.buildH2FileBasedDatabase();
    }

    @AfterAll
    static void afterAll() throws IOException {
        LOG.trace("Deleting H2 database directory: {}", database.getDirectory());
        FileUtils.deleteDirectory(database.getDirectory());
    }

    @Nested
    class BuildDBI {

        @Test
        void shouldThrow_WhenNoArgumentsProvided() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> DropwizardJdbi2Helpers.buildDBI(null, null, null, null, null));
        }

        @Test
        void shouldAcceptConnectionFactory() {
            var connectionFactory = new DataSourceConnectionFactory(database.getDataSource());
            var dbi = DropwizardJdbi2Helpers.buildDBI(null, connectionFactory, null, null, null);
            assertThat(dbi).isNotNull();
            assertCanGetExecuteQuery(dbi);
        }

        @Test
        void shouldAcceptJdbcConnectionProperties() {
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
