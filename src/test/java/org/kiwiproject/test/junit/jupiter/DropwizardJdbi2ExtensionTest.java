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
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.TimeZone;

@DisplayName("DropwizardJdbi2Extension")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class DropwizardJdbi2ExtensionTest {

    private static H2FileBasedDatabase database;

    @RegisterExtension
    final DropwizardJdbi2Extension jdbi2Extension =
            DropwizardJdbi2Extension.builder()
                    .dataSource(database.getDataSource())
                    .databaseTimeZone(TimeZone.getTimeZone("UTC"))
                    .slf4jLoggerName(DropwizardJdbi2ExtensionTest.class.getName())
                    .slfLogLevel(SLF4JLog.Level.INFO)
                    .build();

    private Handle handle;
    private TestTableDao dao;

    @BeforeAll
    static void beforeAll() {
        database = H2DatabaseTestHelper.buildH2FileBasedDatabase();
    }

    @BeforeEach
    void setUp(TestInfo testInfo) {
        LOG.trace("Executing test: {}", testInfo.getDisplayName());
        handle = jdbi2Extension.getHandle();
        dao = new TestTableDao();
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
        var values = dao.findAll(handle);
        assertThat(values).isEmpty();
    }

    @Test
    @Order(3)
    void findAllValues_WithSomeInsertedTestData() {
        handle.insert("insert into test_table values ('Chris', 36)");
        handle.insert("insert into test_table values ('Scott', 44)");
        handle.insert("insert into test_table values ('Han', 45)");
        handle.insert("insert into test_table values ('Tony', 50)");
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
        assertThat(jdbi2Extension.getDbi()).isNotNull();
        assertThat(jdbi2Extension.getHandle()).isNotNull();
    }

    @Value
    private static class TestTableValue {
        String first;
        int second;
    }

    private static class TestTableValueMapper implements ResultSetMapper<TestTableValue> {
        @Override
        public TestTableValue map(int index, ResultSet rs, StatementContext ctx) throws SQLException {
            return new TestTableValue(rs.getString("first"), rs.getInt("second"));
        }
    }

    private static class TestTableDao {

        /**
         * @implNote We need the same exact {@link Handle} to be used in the tests to ensure we get the same
         * SQL {@link java.sql.Connection}, otherwise you run into transaction isolation issues.
         */
        List<TestTableValue> findAll(Handle handle) {
            return handle.createQuery("select * from test_table")
                    .map(new TestTableValueMapper())
                    .list();
        }
    }

}