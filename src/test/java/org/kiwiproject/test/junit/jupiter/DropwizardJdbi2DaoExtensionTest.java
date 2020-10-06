package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kiwiproject.test.h2.H2DatabaseTestHelper;
import org.kiwiproject.test.h2.H2FileBasedDatabase;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.logging.SLF4JLog;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.TimeZone;

@DisplayName("DropwizardJdbi2DaoExtension")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class DropwizardJdbi2DaoExtensionTest {

    private static H2FileBasedDatabase database;

    @RegisterExtension
    final DropwizardJdbi2DaoExtension<TestTableDao> jdbi2DaoExtension =
            DropwizardJdbi2DaoExtension.<TestTableDao>builder()
                    .daoType(TestTableDao.class)
                    .dataSource(database.getDataSource())
                    .databaseTimeZone(TimeZone.getTimeZone("UTC"))
                    .slf4jLoggerName("JDBI 2 DAO Extension Test")
                    .slfLogLevel(SLF4JLog.Level.INFO)
                    .build();

    private Handle handle;
    private TestTableDao dao;

    @BeforeAll
    static void beforeAll() {
        LOG.trace("Create H2 file-based database");
        database = H2DatabaseTestHelper.buildH2FileBasedDatabase();
    }

    @BeforeEach
    void setUp(TestInfo testInfo) {
        LOG.trace("Executing test: {}", testInfo.getDisplayName());
        handle = jdbi2DaoExtension.getHandle();
        dao = jdbi2DaoExtension.getDao();
    }

    @AfterAll
    static void afterAll() throws IOException {
        LOG.trace("Deleting H2 database directory: {}", database.getDirectory());
        FileUtils.deleteDirectory(database.getDirectory());
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
        handle.insert("insert into test_table values ('Chris', 36)");
        handle.insert("insert into test_table values ('Scott', 44)");
        handle.insert("insert into test_table values ('Han', 45)");
        handle.insert("insert into test_table values ('Tony', 50)");
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
        assertThat(jdbi2DaoExtension.getDaoType()).isEqualTo(TestTableDao.class);
        assertThat(jdbi2DaoExtension.getDbi()).isNotNull();
        assertThat(jdbi2DaoExtension.getHandle()).isNotNull();
        assertThat(jdbi2DaoExtension.getDao()).isNotNull();
    }

    @Value
    private static class TestTableValue {
        String first;
        int second;
    }

    // Must be public for JDBI to instantiate
    public static class TestTableValueMapper implements ResultSetMapper<TestTableValue> {
        @Override
        public TestTableValue map(int index, ResultSet rs, StatementContext ctx) throws SQLException {
            return new TestTableValue(rs.getString("first"), rs.getInt("second"));
        }
    }

    @RegisterMapper(TestTableValueMapper.class)
    private interface TestTableDao {

        @SqlQuery("select * from test_table")
        List<TestTableValue> findAll();
    }
}
