package org.kiwiproject.test.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

@SuppressWarnings("removal")
@DisplayName("RuntimeSQLException")
class RuntimeSQLExceptionTest {

    @Test
    void shouldConstructWithMessageAndSQLException() {
        var sqlEx = new SQLException("Illegal syntax or something like that...");
        var runtimeSQLEx = new RuntimeSQLException(sqlEx);

        assertThat(runtimeSQLEx)
                .hasMessage("java.sql.SQLException: Illegal syntax or something like that...")
                .hasCauseReference(sqlEx);
    }

    @Test
    void shouldConstructWithSQLException() {
        var sqlEx = new SQLException("Unknown column 'foo'");
        var runtimeSQLEx = new RuntimeSQLException("Statement error", sqlEx);

        assertThat(runtimeSQLEx)
                .hasMessage("Statement error")
                .hasCauseReference(sqlEx);
    }
}
