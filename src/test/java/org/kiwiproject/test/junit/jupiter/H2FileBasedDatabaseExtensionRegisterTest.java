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
    static final H2FileBasedDatabaseExtension DATABASE_EXTENSION = new H2FileBasedDatabaseExtension();

    @RegisterExtension
    final H2DatabaseTestExtension testExtension = new H2DatabaseTestExtension(DATABASE_EXTENSION.getDatabase());

    @Test
    void shouldMakeDatabaseAvailableToTests() {
        assertThat(DATABASE_EXTENSION.getDatabase()).isNotNull();
        assertThat(DATABASE_EXTENSION.getUrl()).isNotBlank();
        assertThat(DATABASE_EXTENSION.getDirectory()).isNotNull();
        assertThat(DATABASE_EXTENSION.getDataSource()).isNotNull();
    }

    @Test
    void shouldSupplyDatabaseToTestExtension() {
        assertThat(testExtension.getDatabase()).isSameAs(DATABASE_EXTENSION.getDatabase());
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
