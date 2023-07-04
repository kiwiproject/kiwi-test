package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import lombok.Value;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kiwiproject.test.PidLogger;
import org.kiwiproject.test.jdbi.Jdbi3GeneratedKeys;

import java.sql.ResultSet;
import java.sql.SQLException;

@DisplayName("PostgresLiquibaseTestExtension")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings({"SqlNoDataSourceInspection", "SqlDialectInspection"})
class PostgresLiquibaseTestExtensionTest {

    @RegisterExtension
    static final PostgresLiquibaseTestExtension POSTGRES =
            new PostgresLiquibaseTestExtension("PostgresLiquibaseTestExtensionTest/test-migrations.xml");

    @RegisterExtension final Jdbi3Extension jdbi3Extension = Jdbi3Extension.builder()
            .dataSource(POSTGRES.getTestDataSource())
            .plugin(new PostgresPlugin())
            .build();

    private Jdbi jdbi;
    private Handle handle;

    @BeforeEach
    void setUp() {
        PidLogger.logCurrentPid();
        jdbi = jdbi3Extension.getJdbi();
        handle = jdbi3Extension.getHandle();
    }

    @Test
    @Order(1)
    void shouldSetProperties() {
        assertThat(POSTGRES.getTestDataSource()).isNotNull();
        assertThat(POSTGRES.getPostgres()).isNotNull();
    }

    @Test
    @Order(2)
    void shouldAlwaysReturnSameConnection() {
        var handle1 = jdbi.open();
        var handle2 = jdbi.open();

        assertThat(handle1.getConnection())
                .isSameAs(handle2.getConnection())
                .isSameAs(handle.getConnection());
    }

    @Test
    @Order(3)
    void shouldUseSameConnection() {
        handle.createUpdate("create table test_table (name text, age int)").execute();

        jdbi.withHandle(h -> h.createUpdate("insert into test_table (name, age) values ('Bob', 42)")).execute();
        jdbi.withHandle(h -> h.createUpdate("insert into test_table (name, age) values ('Alice', 38)")).execute();
        jdbi.useHandle(h -> {
            var names = h.createQuery("select name from test_table order by name asc").mapTo(String.class).list();
            assertThat(names).containsExactly("Alice", "Bob");
        });
    }

    @Test
    @Order(4)
    void shouldInsertData() {
        var update1 = handle.createUpdate("insert into sample_table (col_1, col_2) values ('A1', 'A2')");
        var id1 = Jdbi3GeneratedKeys.executeAndGenerateId(update1);

        var update2 = handle.createUpdate("insert into sample_table (col_1, col_2) values ('B1', 'B2')");
        var id2 = Jdbi3GeneratedKeys.executeAndGenerateId(update2);

        var samples = handle.createQuery("select * from sample_table order by col_1")
                .map(new SampleMapper())
                .list();

        assertThat(samples).extracting("id", "col1", "col2").containsExactly(
                tuple(id1, "A1", "A2"),
                tuple(id2, "B1", "B2")
        );
    }

    @Test
    @Order(5)
    void shouldNotSeeDataFromPreviousTest() {
        var count = handle.createQuery("select count(*) as sample_count from sample_table")
                .mapTo(Integer.class)
                .first();

        assertThat(count).isZero();
    }

    @Value
    private static class Sample {
        Long id;
        String col1;
        String col2;
    }

    private static class SampleMapper implements RowMapper<Sample> {
        @Override
        public Sample map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Sample(
                    rs.getLong("id"),
                    rs.getString("col_1"),
                    rs.getString("col_2")
            );
        }
    }
}
