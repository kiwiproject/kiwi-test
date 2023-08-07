package org.kiwiproject.test.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kiwiproject.test.junit.jupiter.H2FileBasedDatabaseExtension;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import java.util.stream.IntStream;

@DisplayName("SimpleSingleConnectionDataSource")
class SimpleSingleConnectionDataSourceTest {

    @RegisterExtension
    static final H2FileBasedDatabaseExtension DATABASE_EXTENSION = new H2FileBasedDatabaseExtension();

    private static SimpleSingleConnectionDataSource dataSource;

    @BeforeAll
    static void beforeAll() {
        dataSource = new SimpleSingleConnectionDataSource(DATABASE_EXTENSION.getUrl(), "");
    }

    @AfterAll
    static void afterAll() {
        dataSource.close();
    }

    @Test
    void shouldThrow_GivenInvalidCredentials() {
        var url = DATABASE_EXTENSION.getUrl();

        var thrown = catchThrowable(() -> new SimpleSingleConnectionDataSource(url, "bad_user"));

        assertThat(thrown).isExactlyInstanceOf(RuntimeSQLException.class);

        // The actual cause (as of the time I write this) is a org.h2.jdbc.JdbcSQLInvalidAuthorizationSpecException.
        // Since I do not want to be so specific to a vendor implementation, just check that there is a non-null cause.
        assertThat(thrown.getCause()).isNotNull();
    }

    @Test
    void shouldSetProperties() {
        assertThat(dataSource.getUrl()).isEqualTo(DATABASE_EXTENSION.getUrl());
        assertThat(dataSource.getUsername()).isEmpty();
        assertThat(dataSource.getPassword()).isEmpty();
    }

    @Nested
    class GetConnection {

        @Test
        void shouldReturnTheSameConnection() throws SQLException {
            var connection = dataSource.getConnection();

            IntStream.rangeClosed(1, 25).forEach(value -> assertThat(tryGetConnection()).isSameAs(connection));
        }

        private Connection tryGetConnection() {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                throw new RuntimeSQLException(e);
            }
        }

        @Test
        void shouldIgnoreAttemptsToCloseConnection() throws SQLException {
            var connection = dataSource.getConnection();

            connection.close();

            assertThat(connection.isClosed()).isFalse();
        }

        @Test
        void shouldReThrowTargetExceptionWhenInvokingRealMethodOnConnectionProxy() throws SQLException {
            var connection = dataSource.getConnection();

            //noinspection SqlDialectInspection,SqlNoDataSourceInspection
            assertThatThrownBy(() -> connection.prepareStatement("select * from does_not_exist"))
                    .describedAs("Expected SQLException to be thrown since 'does_not_exist' is not a table'")
                    .isInstanceOf(SQLException.class);
        }
    }

    @Nested
    class GetConnectionByUsernameAndPassword {

        @Test
        void shouldGetConnection_WhenTheyMatch() throws SQLException {
            assertThat(dataSource.getConnection("", "")).isSameAs(dataSource.getConnection());
        }

        @Test
        void shouldThrowException_WhenTheyDoNotMatch() {
            assertThatThrownBy(() -> dataSource.getConnection("bob", "password"))
                    .isExactlyInstanceOf(SQLException.class)
                    .hasMessage("SimpleSingleConnectionDataSource does not support custom username and password");
        }

        @Test
        void shouldReturnTheSameConnection() throws SQLException {
            var connection = dataSource.getConnection();

            IntStream.rangeClosed(1, 25).forEach(value -> assertThat(tryGetConnection("", "")).isSameAs(connection));
        }

        @SuppressWarnings("SameParameterValue")
        private Connection tryGetConnection(String username, String password) {
            try {
                return dataSource.getConnection(username, password);
            } catch (SQLException e) {
                throw new RuntimeSQLException(e);
            }
        }
    }

    @RepeatedTest(5)
    void shouldGetLoginTimeout() throws SQLException {
        assertThat(dataSource.getLoginTimeout()).isZero();
    }

    @RepeatedTest(5)
    void shouldGetParentLogger() throws SQLFeatureNotSupportedException {
        var logger = dataSource.getParentLogger();
        assertThat(logger.getName()).isEqualTo(Logger.GLOBAL_LOGGER_NAME);
    }

    @Nested
    class Unwrapping {

        @Test
        void shouldUnwrapWhenInterfaceIsAssignmentCompatible() throws SQLException {
            assertThat(dataSource.isWrapperFor(DataSource.class)).isTrue();
            assertThat(dataSource.unwrap(DataSource.class)).isSameAs(dataSource);
        }

        @Test
        void shouldThrowWhenInterfaceIsNotAssignmentCompatible() throws SQLException {
            assertThat(dataSource.isWrapperFor(RandomInterface.class)).isFalse();
            assertThatThrownBy(() -> dataSource.unwrap(RandomInterface.class))
                    .isExactlyInstanceOf(SQLException.class)
                    .hasMessage("DataSource of type %s cannot be unwrapped as %s",
                            SimpleSingleConnectionDataSource.class.getName(), RandomInterface.class.getName());
        }
    }

    interface RandomInterface {
    }

    @Nested
    class ShouldNotSupport {

        @Test
        void getLogWriter() {
            assertThrowsUnsupportedOperationException(() -> dataSource.getLogWriter());
        }

        @Test
        void setLogWriter() {
            assertThrowsUnsupportedOperationException(() -> dataSource.setLogWriter(new PrintWriter(new StringWriter())));
        }

        @Test
        void setLoginTimeout() {
            assertThrowsUnsupportedOperationException(() -> dataSource.setLoginTimeout(0));
        }

        private void assertThrowsUnsupportedOperationException(ThrowableAssert.ThrowingCallable shouldRaiseThrowable) {
            assertThatThrownBy(shouldRaiseThrowable).isExactlyInstanceOf(UnsupportedOperationException.class);
        }
    }
}
