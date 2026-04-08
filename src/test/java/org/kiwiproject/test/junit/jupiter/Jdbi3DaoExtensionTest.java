package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@DisplayName("Jdbi3DaoExtension")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
class Jdbi3DaoExtensionTest {

    @RegisterExtension
    static final H2FileBasedDatabaseExtension DATABASE_EXTENSION = new H2FileBasedDatabaseExtension();

    @RegisterExtension
    final Jdbi3DaoExtension<TestTableDao> jdbi3DaoExtension = Jdbi3DaoExtension.<TestTableDao>builder()
            .daoType(TestTableDao.class)
            .dataSource(DATABASE_EXTENSION.getDataSource())
            .slf4jLoggerName(Jdbi3DaoExtensionTest.class.getName())
            .plugin(new H2DatabasePlugin())
            .build();

    private Handle handle;
    private TestTableDao dao;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        LOG.trace("Executing test: {}", testInfo.getDisplayName());
        handle = jdbi3DaoExtension.getHandle();
        dao = jdbi3DaoExtension.getDao();
    }

    @Test
    @Order(1)
    void shouldBeginTransactionResultingInDisabledAutoCommit() throws SQLException {
        assertThat(handle.getConnection().getAutoCommit()).isFalse();
    }

    @Test
    @Order(2)
    void findAllValues_ShouldNotSeeAnyDataBeforeTestDataInserted() {
        var values = dao.findAll();
        assertThat(values).isEmpty();
    }

    @Test
    @Order(3)
    void findAllValues_WithSomeInsertedTestData() {
        handle.execute("insert into test_table values ('Chris', 36)");
        handle.execute("insert into test_table values ('Scott', 44)");
        handle.execute("insert into test_table values ('Han', 45)");
        handle.execute("insert into test_table values ('Tony', 50)");
        var values = dao.findAll();
        assertThat(values).hasSize(4);
    }

    @Test
    @Order(4)
    void findAllValues_ShouldNotSeeAnyDataFromPreviousTest() {
        var values = dao.findAll();
        assertThat(values).isEmpty();
    }

    @Test
    @Order(5)
    void shouldSetProperties() {
        assertThat(jdbi3DaoExtension.getDaoType()).isEqualTo(TestTableDao.class);
        assertThat(jdbi3DaoExtension.getJdbi()).isNotNull();
        assertThat(jdbi3DaoExtension.getHandle()).isNotNull();
        assertThat(jdbi3DaoExtension.getDao()).isNotNull();
    }

    @Test
    @Order(6)
    void shouldHandleExceptionsInPersistenceCode() {
        assertThatThrownBy(() -> dao.findAllInvalidSql())
                .isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    @Order(7)
    void shouldNotBeAffectedByPreviousFailure() {
        assertThat(dao.insert("hello", 42)).isOne();
        assertThat(dao.insert("world", 84)).isOne();
        assertThat(dao.findAll()).hasSize(2);
    }

    @Test
    @Order(8)
    @SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
    void shouldRegisterArgumentFactory() {
        var extensionWithFactory = Jdbi3DaoExtension.<TestTableDao>builder()
                .daoType(TestTableDao.class)
                .dataSource(DATABASE_EXTENSION.getDataSource())
                .argumentFactory(new NameValueArgumentFactory())
                .build();
        try (var testHandle = extensionWithFactory.getJdbi().open()) {
            testHandle.begin();
            var count = testHandle.createUpdate("insert into test_table values (:col1, :col2)")
                    .bindByType("col1", new NameValue("test-value"), NameValue.class)
                    .bind("col2", 1)
                    .execute();
            assertThat(count).isOne();
            testHandle.rollback();
        }
    }

    private record NameValue(String value) {}

    private static class NameValueArgumentFactory implements ArgumentFactory {
        @Override
        public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
            if (type == NameValue.class) {
                var nameValue = (NameValue) value;
                return Optional.of((pos, stmt, ctx) -> stmt.setString(pos, nameValue.value()));
            }
            return Optional.empty();
        }
    }

    @Value
    public static class TestTableValue {
        String col1;
        int col2;
    }

    // Must be public for JDBI to instantiate
    public static class TestTableValueMapper implements RowMapper<TestTableValue> {
        @Override
        public TestTableValue map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new TestTableValue(rs.getString("col_1"), rs.getInt("col_2"));
        }
    }

    @RegisterRowMapper(TestTableValueMapper.class)
    private interface TestTableDao {

        @SqlUpdate("insert into test_table values (:col1, :col2)")
        int insert(@Bind("col1") String value1, @Bind("col2") int value2);

        @SqlQuery("select * from test_table")
        List<TestTableValue> findAll();

        @SuppressWarnings("UnusedReturnValue")
        @SqlQuery("select does_not_exist from test_table")
        List<TestTableValue> findAllInvalidSql();
    }
}
