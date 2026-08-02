package org.kiwiproject.test.liquibase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kiwiproject.jdbc.UncheckedSQLException;
import org.kiwiproject.test.jdbc.JdbcTests;

import javax.sql.DataSource;
import java.sql.DriverManager;
import java.sql.SQLException;

@DisplayName("Liquibase")
@SuppressWarnings("SqlNoDataSourceInspection")
class LiquibaseTestHelpersTest {

    @Nested
    class RunMigrations {

        @Nested
        class WithDataSource {

            @Test
            void shouldRejectNullDataSource() {
                assertThatThrownBy(() -> LiquibaseTestHelpers.runMigrations((DataSource) null, "migrations.xml"))
                        .isInstanceOf(IllegalArgumentException.class);
            }

            @Test
            void shouldRejectBlankChangelogPath() {
                var dataSource = mock(DataSource.class);
                assertThatThrownBy(() -> LiquibaseTestHelpers.runMigrations(dataSource, " "))
                        .isInstanceOf(IllegalArgumentException.class);
            }

            @Test
            void shouldWrapSQLException() throws SQLException {
                var dataSource = mock(DataSource.class);
                when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));

                assertThatThrownBy(() -> LiquibaseTestHelpers.runMigrations(dataSource, "migrations.xml"))
                        .isInstanceOf(UncheckedSQLException.class)
                        .hasCauseInstanceOf(SQLException.class);
            }

            @Test
            void shouldRunMigrationsSuccessfully() {
                var jdbcUrl = "jdbc:h2:mem:liquibase-test-helpers-happy-ds-" + System.currentTimeMillis();
                var dataSource = JdbcTests.newTestDataSource(jdbcUrl, "sa", "");

                var results = LiquibaseTestHelpers.runMigrations(dataSource, "LiquibaseTestHelpersTest/test-migrations.xml");

                assertThat(results).isNotNull();
            }
        }

        @Nested
        class WithLiquibaseDatabase {

            @Test
            void shouldRejectNullDatabase() {
                assertThatThrownBy(() -> LiquibaseTestHelpers.runMigrations((Database) null, "migrations.xml"))
                        .isInstanceOf(IllegalArgumentException.class);
            }

            @SuppressWarnings("resource")
            @Test
            void shouldRejectBlankChangelogPath() {
                var database = mock(Database.class);
                assertThatThrownBy(() -> LiquibaseTestHelpers.runMigrations(database, " "))
                        .isInstanceOf(IllegalArgumentException.class);
            }

            @Test
            void shouldWrapLiquibaseException() throws SQLException, DatabaseException {
                var jdbcUrl = "jdbc:h2:mem:liquibase-test-helpers-" + System.currentTimeMillis();
                try (var connection = DriverManager.getConnection(jdbcUrl)) {
                    var database = DatabaseFactory.getInstance()
                            .findCorrectDatabaseImplementation(new JdbcConnection(connection));

                    assertThatThrownBy(() ->
                            LiquibaseTestHelpers.runMigrations(database, "nonexistent-changelog.xml"))
                            .isInstanceOf(UncheckedLiquibaseException.class)
                            .hasCauseInstanceOf(LiquibaseException.class)
                            .hasMessageContaining("nonexistent-changelog.xml");
                }
            }

            @Test
            void shouldRunMigrationsSuccessfully() throws SQLException, DatabaseException {
                var jdbcUrl = "jdbc:h2:mem:liquibase-test-helpers-happy-" + System.currentTimeMillis();

                try (var connection = DriverManager.getConnection(jdbcUrl)) {
                    var database = DatabaseFactory.getInstance()
                            .findCorrectDatabaseImplementation(new JdbcConnection(connection));

                    var results = LiquibaseTestHelpers.runMigrations(database, "LiquibaseTestHelpersTest/test-migrations.xml");

                    assertThat(results).isNotNull();

                    // prove the table actually got created
                    try (var statement = connection.createStatement()) {
                        var resultSet = statement.executeQuery("select count(*) from sample_table");
                        assertThat(resultSet.next()).isTrue();
                        assertThat(resultSet.getInt(1)).isZero();
                    }
                }
            }
        }
    }
}
