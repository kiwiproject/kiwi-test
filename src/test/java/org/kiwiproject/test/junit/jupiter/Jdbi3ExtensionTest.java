package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@DisplayName("Jdbi3Extension")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
class Jdbi3ExtensionTest {

    @RegisterExtension
    static final H2FileBasedDatabaseExtension DATABASE_EXTENSION = new H2FileBasedDatabaseExtension();

    @RegisterExtension
    final Jdbi3Extension jdbi3Extension = Jdbi3Extension.builder()
            .dataSource(DATABASE_EXTENSION.getDataSource())
            .slf4jLoggerName(Jdbi3ExtensionTest.class.getName())
            .plugin(new H2DatabasePlugin())
            .build();

    private Handle handle;
    private TestTableDao dao;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        LOG.trace("Executing test: {}", testInfo.getDisplayName());
        handle = jdbi3Extension.getHandle();
        dao = new TestTableDao();
    }

    @Test
    @Order(1)
    void shouldBeginTransactionResultingInDisabledAutoCommit() throws SQLException {
        assertThat(handle.getConnection().getAutoCommit()).isFalse();
    }

    @Test
    @Order(2)
    void findAllValues_ShouldNotSeeAnyDataBeforeTestDataInserted() {
        var values = dao.findAll(handle);
        assertThat(values).isEmpty();
    }

    @Test
    @Order(3)
    void findAllValues_WithSomeInsertedTestData() {
        handle.execute("insert into test_table values ('Chris', 36)");
        handle.execute("insert into test_table values ('Scott', 44)");
        handle.execute("insert into test_table values ('Han', 45)");
        handle.execute("insert into test_table values ('Tony', 50)");
        var values = dao.findAll(handle);
        assertThat(values).hasSize(4);
    }

    @Test
    @Order(4)
    void findAllValues_ShouldNotSeeAnyDataFromPreviousTest() {
        var values = dao.findAll(handle);
        assertThat(values).isEmpty();
    }

    @Test
    @Order(5)
    void shouldSetProperties() {
        assertThat(jdbi3Extension.getJdbi()).isNotNull();
        assertThat(jdbi3Extension.getHandle()).isNotNull();
    }

    @Test
    @Order(6)
    void shouldHandleExceptionsInPersistenceCode() {
        assertThatThrownBy(() -> dao.findAllInvalidSql(handle))
                .isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    @Order(7)
    void shouldNotBeAffectedByPreviousFailure() {
        assertThat(dao.insert(handle, "hello", 42)).isOne();
        assertThat(dao.insert(handle, "world", 84)).isOne();
        assertThat(dao.findAll(handle)).hasSize(2);
    }

    @Value
    private static class TestTableValue {
        String col1;
        int col2;
    }

    private static class TestTableValueMapper implements RowMapper<TestTableValue> {
        @Override
        public TestTableValue map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new TestTableValue(rs.getString("col_1"), rs.getInt("col_2"));
        }
    }

    /**
     * @implNote We need the same exact {@link Handle} to be used in the tests to ensure we get the same
     * SQL {@code Connection}, otherwise you run into transaction isolation issues.
     */
    private static class TestTableDao {

        int insert(Handle handle, String value1, int value2) {
            return handle.createUpdate("insert into test_table values (:col1, :col2)")
                    .bind("col1", value1)
                    .bind("col2", value2)
                    .execute();
        }

        List<TestTableValue> findAll(Handle handle) {
            return handle.createQuery("select * from test_table")
                    .map(new TestTableValueMapper())
                    .list();
        }

        @SuppressWarnings("UnusedReturnValue")
        List<TestTableValue> findAllInvalidSql(Handle handle) {
            return handle.createQuery("select does_not_exist from test_table")
                    .map(new TestTableValueMapper())
                    .list();
        }
    }
}
