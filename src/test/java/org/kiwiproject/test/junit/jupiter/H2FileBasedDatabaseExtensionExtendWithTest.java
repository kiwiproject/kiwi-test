package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kiwiproject.test.h2.H2FileBasedDatabase;

import java.sql.SQLException;

/**
 * Test of {@link H2FileBasedDatabaseExtension} using {@code ExtendWith}.
 */
@DisplayName("H2FileBasedDatabaseExtension using @ExtendWith")
@ExtendWith(H2FileBasedDatabaseExtension.class)
class H2FileBasedDatabaseExtensionExtendWithTest {

    private H2FileBasedDatabase databaseFromSetup;

    // This exists so that we can test custom database setup
    @SuppressWarnings({"SqlNoDataSourceInspection", "SqlDialectInspection"})
    @BeforeAll
    static void beforeAll(@H2Database H2FileBasedDatabase database) {
        try (var conn = database.getDataSource().getConnection()) {
            var createTableStmt = conn.createStatement();
            createTableStmt.execute("create table people (name varchar , age integer)");

            var insertDataStmt = conn.createStatement();
            insertDataStmt.addBatch("insert into people values ('Bob', 42)");
            insertDataStmt.addBatch("insert into people values ('Alice', 24)");
            insertDataStmt.addBatch("insert into people values ('Zack', 12)");
            insertDataStmt.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp(@H2Database H2FileBasedDatabase database) {
        this.databaseFromSetup = database;
    }

    @Test
    void shouldProvideDatabaseInTestMethod(@H2Database H2FileBasedDatabase database) {
        assertThat(database).isNotNull();
    }

    @Test
    void shouldProvideDatabaseInLifecycleMethod(@H2Database H2FileBasedDatabase database) {
        assertThat(databaseFromSetup)
                .isNotNull()
                .isSameAs(database);
    }

    @SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
    @Test
    void shouldBeAbleToConnectToDatabase(@H2Database H2FileBasedDatabase database) throws SQLException {
        try (var conn = database.getDataSource().getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("select count(*) as count from test_table")) {

            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("count")).isZero();
        }
    }

    @Test
    void shouldHaveCreatedPeopleTableInCustomBeforeAll(@H2Database H2FileBasedDatabase database) throws SQLException {
        try (var conn = database.getDataSource().getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("select count(*) as count from people")) {

            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("count")).isEqualTo(3);
        }
    }

    @Nested
    class NestedTestClass {

        private H2FileBasedDatabase databaseFromNestedSetup;

        @BeforeEach
        void setUp(@H2Database H2FileBasedDatabase database) {
            this.databaseFromNestedSetup = database;
        }

        @Test
        void shouldProvideDatabaseInNestedTestMethod(@H2Database H2FileBasedDatabase database) {
            assertThat(database).isNotNull();
        }

        @Test
        void shouldProvideDatabaseInNestedClassLifecycleMethod(@H2Database H2FileBasedDatabase database) {
            assertThat(databaseFromNestedSetup)
                    .isNotNull()
                    .isSameAs(database);
        }

        @Test
        void shouldHaveSameDatabaseForNestedAndParentSetup() {
            assertThat(databaseFromNestedSetup).isSameAs(databaseFromSetup);
        }
    }

    @DisplayName("Another Nested Test Class")
    @Nested
    class AnotherNestedTestClass {

        @Test
        void shouldReceiveSameDatabaseAsParent(@H2Database H2FileBasedDatabase database) {
            assertThat(database).isSameAs(databaseFromSetup);
        }

        @Nested
        class NestedInsideAnotherNestedTestClass {

            @Test
            void shouldReceiveSameDatabaseAsParent(@H2Database H2FileBasedDatabase database) {
                assertThat(database).isSameAs(databaseFromSetup);
            }
        }
    }
}
