package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;

import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kiwiproject.test.PidLogger;
import org.kiwiproject.test.h2.H2FileBasedDatabase;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

@SuppressWarnings({ "SqlResolve", "SqlNoDataSourceInspection" })
@DisplayName("H2LiquibaseExtension")
class H2LiquibaseExtensionTest {

    @RegisterExtension
    static final H2LiquibaseExtension H2_LIQUIBASE_EXTENSION =
            new H2LiquibaseExtension("H2LiquibaseExtensionTest/test-migrations.xml");

    private Connection conn;

    @BeforeEach
    void setUp() throws SQLException {
        PidLogger.logCurrentPid();
        conn = H2_LIQUIBASE_EXTENSION.getDataSource().getConnection();
        conn.setAutoCommit(false);
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.rollback();
    }

    @Test
    void shouldMakeDatabaseAvailableToTests() {
        assertThat(H2_LIQUIBASE_EXTENSION.getDatabase()).isNotNull();
        assertThat(H2_LIQUIBASE_EXTENSION.getUrl()).isNotBlank();
        assertThat(H2_LIQUIBASE_EXTENSION.getDirectory()).isNotNull();
        assertThat(H2_LIQUIBASE_EXTENSION.getDataSource()).isNotNull();
    }

    @Test
    void shouldProvideH2FileBaseDatabaseInParameter(@H2Database H2FileBasedDatabase database) {
        assertThat(database).isSameAs(H2_LIQUIBASE_EXTENSION.getDatabase());
    }

    @Test
    void shouldAllowDirectAccessToH2FileBasedDatabaseExtension() {
        assertThat(H2_LIQUIBASE_EXTENSION.getH2Extension()).isNotNull();

        assertThat(H2_LIQUIBASE_EXTENSION.getH2Extension().getDatabase())
                .isSameAs(H2_LIQUIBASE_EXTENSION.getDatabase());
    }

    @Test
    void shouldRunMigrationsBeforeAllTests() throws SQLException {
        try (var stmt = conn.createStatement();
             var rs = stmt.executeQuery("select * from databasechangelog order by orderexecuted")) {

            var ids = new ArrayList<String>();
            while (rs.next()) {
                ids.add(rs.getString("id"));
            }

            assertThat(ids).containsExactly("0001_create_sample_table", "0002_create_another_table");
        }
    }

    @Test
    void shouldUseConnectionToInsertData() throws SQLException {
        try (var stmt = conn.createStatement()) {
            var count = stmt.executeUpdate("insert into sample_table (col_1, col_2) values ('a', 'b')");
            assertThat(count).isOne();
        }
    }

    @Test
    void shouldUseConnectionToQueryData() throws SQLException {
        try (var ps = conn.prepareStatement("insert into sample_table (col_1, col_2) values (?, ?)")) {
            ps.setString(1, "value 1");
            ps.setString(2, "value 2");
            var updateCount = ps.executeUpdate();
            assertThat(updateCount).isOne();
        }

        try (var stmt = conn.createStatement(); var rs = stmt.executeQuery("select * from sample_table")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("col_1")).isEqualTo("value 1");
            assertThat(rs.getString("col_2")).isEqualTo("value 2");
        }
    }

    @Nested
    class ParameterResolutionInNestedTests {

        @Test
        void shouldProvideH2FileBaseDatabaseInParameter(@H2Database H2FileBasedDatabase database) {
            assertThat(database).isSameAs(H2_LIQUIBASE_EXTENSION.getDatabase());
        }
    }

    @Nested
    class UsingJdbi {

        private Jdbi jdbi;

        @BeforeEach
        void setUp() {
            jdbi = Jdbi.create(conn);
        }

        @Test
        void shouldInsertData() {
            jdbi.useHandle(handle -> {
                var updateCount1 = handle.createUpdate("insert into sample_table (col_1, col_2) values (?, ?)")
                        .bind(0, "value 1")
                        .bind(1, "value 2")
                        .execute();
                assertThat(updateCount1).isOne();

                var updateCount2 = handle.createUpdate("insert into another_table (another_col_1, another_col_2) values (?, ?)")
                        .bind(0, "another value 1")
                        .bind(1, 42)
                        .execute();
                assertThat(updateCount2).isOne();
            });
        }

        @Test
        void shouldQueryData() {
            jdbi.useHandle(handle -> {
                var sql = "insert into sample_table (col_1, col_2) values (?, ?)";
                handle.createUpdate(sql).bind(0, "val 1").bind(1, "val 2").execute();
                handle.createUpdate(sql).bind(0, "val 1").bind(1, "val 2").execute();
                handle.createUpdate(sql).bind(0, "val 1").bind(1, "val 2").execute();
            });

            var count = jdbi.withHandle(handle ->
                    handle.createQuery("select count(*) from sample_table").mapTo(Long.TYPE).one());

            assertThat(count).isEqualTo(3);
        }
    }
}
