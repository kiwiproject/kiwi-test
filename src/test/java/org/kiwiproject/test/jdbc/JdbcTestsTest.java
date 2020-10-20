package org.kiwiproject.test.jdbc;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kiwiproject.test.h2.H2FileBasedDatabase;
import org.kiwiproject.test.junit.jupiter.H2Database;
import org.kiwiproject.test.junit.jupiter.H2FileBasedDatabaseExtension;

import java.sql.SQLException;

@DisplayName("JdbcTests")
@ExtendWith(H2FileBasedDatabaseExtension.class)
class JdbcTestsTest {

    private static final String H2_DATABASE_USERNAME = "";
    private static final String H2_DATABASE_PASSWORD = "";

    private String databaseUrl;
    private SimpleSingleConnectionDataSource dataSource;

    @BeforeEach
    void setUp(@H2Database H2FileBasedDatabase database) {
        this.databaseUrl = database.getUrl();
    }

    @AfterEach
    void tearDown() {
        if (nonNull(dataSource)) {
            dataSource.close();
        }
    }

    @Nested
    class NewTestDataSource {

        @Test
        void shouldCreateFromJdbcProperties() throws SQLException {
            var jdbcProperties = JdbcProperties.builder()
                    .url(databaseUrl)
                    .username(H2_DATABASE_USERNAME)
                    .password(H2_DATABASE_PASSWORD)
                    .build();

            dataSource = JdbcTests.newTestDataSource(jdbcProperties);
            assertDataSource();
        }

        @Test
        void shouldCreateFromUrlAndUsername() throws SQLException {
            dataSource = JdbcTests.newTestDataSource(databaseUrl, H2_DATABASE_USERNAME);
            assertDataSource();
        }

        @Test
        void shouldCreateFromUrlAndUserCredentials() throws SQLException {
            dataSource = JdbcTests.newTestDataSource(databaseUrl, H2_DATABASE_USERNAME, H2_DATABASE_PASSWORD);
            assertDataSource();
        }

        private void assertDataSource() throws SQLException {
            assertThat(dataSource).isNotNull();
            assertThat(dataSource.getUrl()).isEqualTo(databaseUrl);
            assertThat(dataSource.getUsername()).isEqualTo(H2_DATABASE_USERNAME);
            assertThat(dataSource.getPassword()).isEqualTo(H2_DATABASE_PASSWORD);
            assertThat(dataSource.getConnection()).isNotNull();
            assertThat(dataSource.getConnection().isClosed()).isFalse();
        }
    }
}
