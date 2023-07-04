package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import lombok.Value;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kiwiproject.test.jdbi.Jdbi3GeneratedKeys;

import java.sql.ResultSet;
import java.sql.SQLException;

@DisplayName("H2LiquibaseExtension with JdbiExtension")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings({ "SqlNoDataSourceInspection", "SqlResolve" })
class H2LiquibaseExtensionWithJdbiExtensionTest {

    @RegisterExtension
    static final H2LiquibaseExtension H2_LIQUIBASE_EXTENSION =
            new H2LiquibaseExtension("H2LiquibaseExtensionTest/test-migrations.xml");

    @RegisterExtension final Jdbi3Extension jdbi3Extension = Jdbi3Extension.builder()
            .dataSource(H2_LIQUIBASE_EXTENSION.getDataSource())
            .build();

    private Handle handle;

    @BeforeEach
    void setUp() {
        handle = jdbi3Extension.getHandle();
    }

    @Test
    @Order(1)
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
    @Order(2)
    void shouldNotSeeDataFromPreviousTest() {
        var count = handle.createQuery("select count(*) as sample_count from sample_table")
                .mapTo(Integer.class)
                .first();

        assertThat(count).isZero();
    }

    @Nested
    class NestedTests {

        @Test
        @Order(3)
        void shouldQueryAnotherTable() {
            var samples = handle.createQuery("select * from another_table")
                    .map(new AnotherSampleMapper())
                    .list();

            assertThat(samples).isEmpty();
        }

        @Test
        @Order(4)
        void shouldInsertAndQueryAnotherTable() {
            var update1 = handle.createUpdate("insert into another_table (another_col_1, another_col_2) values ('C1', 42)");
            var id1 = Jdbi3GeneratedKeys.executeAndGenerateId(update1);

            var update2 = handle.createUpdate("insert into another_table (another_col_1, another_col_2) values ('D1', 84)");
            var id2 = Jdbi3GeneratedKeys.executeAndGenerateId(update2);

            var update3 = handle.createUpdate("insert into another_table (another_col_1, another_col_2) values ('E1', 126)");
            var id3 = Jdbi3GeneratedKeys.executeAndGenerateId(update3);

            var samples = handle.createQuery("select * from another_table order by another_col_1 desc")
                    .map(new AnotherSampleMapper())
                    .list();

            assertThat(samples).extracting("id", "anotherCol1", "anotherCol2").containsExactly(
                    tuple(id3, "E1", 126),
                    tuple(id2, "D1", 84),
                    tuple(id1, "C1", 42)
            );
        }
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

    @Value
    private static class AnotherSample {
        Long id;
        String anotherCol1;
        int anotherCol2;
    }

    private static class AnotherSampleMapper implements RowMapper<AnotherSample> {
        @Override
        public AnotherSample map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new AnotherSample(
                    rs.getLong("id"),
                    rs.getString("another_col_1"),
                    rs.getInt("another_col_2")
            );
        }
    }

}
