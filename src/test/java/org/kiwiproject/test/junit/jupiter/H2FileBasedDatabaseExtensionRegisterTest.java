package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kiwiproject.test.h2.H2FileBasedDatabase;

import java.sql.SQLException;

/**
 * Test of {@link H2FileBasedDatabaseExtension} using {@code RegisterExtension}.
 */
@DisplayName("H2FileBasedDatabaseExtension using @RegisterExtension")
class H2FileBasedDatabaseExtensionRegisterTest {

    @RegisterExtension
    static H2FileBasedDatabaseExtension databaseExtension = new H2FileBasedDatabaseExtension();

    @RegisterExtension
    final H2DatabaseTestExtension testExtension = new H2DatabaseTestExtension(databaseExtension.getDatabase());

    @Test
    void shouldMakeDatabaseAvailableToTests() {
        assertThat(databaseExtension.getDatabase()).isNotNull();
        assertThat(databaseExtension.getUrl()).isNotBlank();
        assertThat(databaseExtension.getDirectory()).isNotNull();
        assertThat(databaseExtension.getDataSource()).isNotNull();
    }

    @Test
    void shouldSupplyDatabaseToTestExtension() {
        assertThat(testExtension.getDatabase()).isSameAs(databaseExtension.getDatabase());
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
}
