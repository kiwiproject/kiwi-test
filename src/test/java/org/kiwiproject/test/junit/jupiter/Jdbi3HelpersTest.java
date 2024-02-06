package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.CloseException;
import org.jdbi.v3.core.ConnectionFactory;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.core.statement.Binding;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.core.transaction.UnableToManipulateTransactionIsolationLevelException;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.kiwiproject.test.h2.H2FileBasedDatabase;
import org.mockito.stubbing.OngoingStubbing;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@DisplayName("Jdbi3Helpers")
@Slf4j
@ExtendWith(H2FileBasedDatabaseExtension.class)
class Jdbi3HelpersTest {

    @Nested
    class DatabaseTypeEnum {

        @Test
        void shouldGetH2PluginInstance() {
            var pluginOptional = Jdbi3Helpers.DatabaseType.pluginFromDatabaseUrl("jdbc:h2:/tmp/h2-test-db-12345/testdb");
            assertThat(pluginOptional).containsInstanceOf(H2DatabasePlugin.class);
        }

        @Test
        void shouldGetPostgresPluginInstance() {
            var pluginOptional = Jdbi3Helpers.DatabaseType.pluginFromDatabaseUrl("jdbc:postgresql://localhost:5432/testdb");
            assertThat(pluginOptional).containsInstanceOf(PostgresPlugin.class);
        }

        @Test
        void shouldReturnEmptyOptionalForUnsupportedDatabase() {
            assertThat(Jdbi3Helpers.DatabaseType.pluginFromDatabaseUrl("jdbc:mysql://localhost:33060/sakila"))
                    .isEmpty();
        }
    }

    @Nested
    class GetPluginInstance {

        @Test
        void shouldReturnOptionalContainingPlugin_WhenClassIsAvailable() {
            assertThat(Jdbi3Helpers.getPluginInstance(PostgresPlugin.class.getName()))
                    .containsInstanceOf(PostgresPlugin.class);

            assertThat(Jdbi3Helpers.getPluginInstance(H2DatabasePlugin.class.getName()))
                    .containsInstanceOf(H2DatabasePlugin.class);
        }

        @Test
        void shouldReturnEmptyOptional_WhenClassIsNotAvailable() {
            assertThat(Jdbi3Helpers.getPluginInstance("org.jdbi.v3.mysql.MysqlPlugin"))
                    .isEmpty();
        }

        @Test
        void shouldReturnEmptyOptional_WhenErrorInstantiatingPluginClass() {
            assertThat(Jdbi3Helpers.getPluginInstance(MisbehavingDatabasePlugin.class.getName()))
                    .isEmpty();
        }
    }

    public static class MisbehavingDatabasePlugin extends JdbiPlugin.Singleton {
        public MisbehavingDatabasePlugin() {
            throw new IllegalStateException("I cannot be created");
        }
    }

    @Nested
    class BuildJdbi {

        @Test
        void shouldThrow_WhenNoArgumentsProvided() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Jdbi3Helpers.buildJdbi(null, null, null, null, null, List.of()));
        }

        @Test
        void shouldAcceptConnectionFactory(@H2Database H2FileBasedDatabase database) {
            var connectionFactory = new DataSourceConnectionFactory(database.getDataSource());
            var jdbi = Jdbi3Helpers.buildJdbi(null, connectionFactory, null, null, null, List.of());
            assertThat(jdbi).isNotNull();
            assertCanExecuteQuery(jdbi);
        }

        @Test
        void shouldAcceptJdbcConnectionProperties(@H2Database H2FileBasedDatabase database) {
            var jdbi = Jdbi3Helpers.buildJdbi(null, null, database.getUrl(), "", "", List.of());
            assertThat(jdbi).isNotNull();
            assertCanExecuteQuery(jdbi);
        }

        @SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
        private void assertCanExecuteQuery(Jdbi jdbi) {
            try (var handle = jdbi.open()) {
                var count = handle.createQuery("select count(*) as cnt from test_table")
                        .mapTo(Integer.class)
                        .findOne()
                        .orElseThrow();
                assertThat(count).isZero();
            }
        }
    }

    @Nested
    class FindDatabasePlugin {

        private Jdbi jdbi;

        @BeforeEach
        void setUp() {
            jdbi = mock(Jdbi.class);
        }

        @Test
        void shouldReturnPluginWhenFound() {
            var h2Plugin = new H2DatabasePlugin();
            whenWithHandleCalled().thenReturn(Optional.of(h2Plugin));

            var pluginOptional = Jdbi3Helpers.findDatabasePlugin(jdbi);
            assertThat(pluginOptional).contains(h2Plugin);
        }

        @Test
        void shouldReturnEmptyWhenExceptionThrownFindingPlugin() {
            whenWithHandleCalled().thenThrow(new SQLException("oops"));

            var pluginOptional = Jdbi3Helpers.findDatabasePlugin(jdbi);
            assertThat(pluginOptional).isEmpty();
        }

        @SuppressWarnings("unchecked")
        private OngoingStubbing<Object> whenWithHandleCalled() {
            try {
                return when(jdbi.withHandle(any(HandleCallback.class)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    class ConfigureSqlLogger {

        private Jdbi jdbi;

        @BeforeEach
        void setUp() {
            jdbi = mock(Jdbi.class);
        }

        @Test
        void shouldUseDefaultLoggerName_WhenGivenBlankLoggerName() {
            var jdbiSqlLogger = Jdbi3Helpers.configureSqlLogger(jdbi, "");
            assertThat(jdbiSqlLogger.getLoggerName()).isEqualTo(Jdbi.class.getName());

            verify(jdbi).setSqlLogger(jdbiSqlLogger);
        }

        @Test
        void shouldUseGivenLoggerName() {
            var loggerName = "My Jdbi Logger";
            var jdbiSqlLogger = Jdbi3Helpers.configureSqlLogger(jdbi, loggerName);
            assertThat(jdbiSqlLogger.getLoggerName()).isEqualTo(loggerName);

            verify(jdbi).setSqlLogger(jdbiSqlLogger);
        }
    }

    @Nested
    class JdbiSqlLogger {

        @Test
        void shouldLogExceptions() {
            var jdbi = mock(Jdbi.class);
            var loggerName = Jdbi3Helpers.class.getName();
            var jdbiSqlLogger = Jdbi3Helpers.configureSqlLogger(jdbi, loggerName);

            //noinspection resource
            var statementContext = mock(StatementContext.class);
            when(statementContext.getRenderedSql()).thenReturn("select * from foo");
            var binding = mock(Binding.class);
            when(binding.toString()).thenReturn("[the binding]");
            when(statementContext.getBinding()).thenReturn(binding);

            var sqlException = new SQLException("bad SQL");

            assertThatCode(() -> jdbiSqlLogger.logException(statementContext, sqlException))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class DescribeTransactionIsolationLevel {

        private Handle handle;

        @BeforeEach
        void setUp() {
            handle = mock(Handle.class);
        }

        @ParameterizedTest
        @EnumSource(TransactionIsolationLevel.class)
        void shouldDescribeWhenSuccessGettingIsolationLevel(TransactionIsolationLevel level) {
            when(handle.getTransactionIsolationLevel()).thenReturn(level);

            var description = Jdbi3Helpers.describeTransactionIsolationLevel(handle);
            assertThat(description)
                    .isEqualTo("%s (java.sql.Connection isolation level: %d)", level.name(), level.intValue());
        }

        @Test
        void shouldDescribeWhenExceptionGettingIsolationLevel() {
            var sqlException = new SQLException("error getting isolation");
            var jdbiException = new UnableToManipulateTransactionIsolationLevelException(
                    "unable to access current setting", sqlException);
            when(handle.getTransactionIsolationLevel()).thenThrow(jdbiException);

            var description = Jdbi3Helpers.describeTransactionIsolationLevel(handle);
            assertThat(description).isEqualTo("ERROR (%s: %s, cause: %s)",
                    jdbiException.getClass().getName(), jdbiException.getMessage(), sqlException);
        }
    }

    @Nested
    class GetTransactionIsolationLevel {

        private Handle handle;

        @BeforeEach
        void setUp() {
            handle = mock(Handle.class);
        }

        @ParameterizedTest
        @EnumSource(TransactionIsolationLevel.class)
        void shouldReturnIsolationLevel(TransactionIsolationLevel level) {
            when(handle.getTransactionIsolationLevel()).thenReturn(level);

            var result = Jdbi3Helpers.getTransactionIsolationLevel(handle);
            assertThat(result.getLeft()).isEqualTo(level);
            assertThat(result.getRight()).isNull();
        }

        @Test
        void shouldReturnException() {
            var sqlException = new SQLException("error getting isolation");
            var jdbiException = new UnableToManipulateTransactionIsolationLevelException(
                    "unable to access current setting", sqlException);
            when(handle.getTransactionIsolationLevel()).thenThrow(jdbiException);

            var result = Jdbi3Helpers.getTransactionIsolationLevel(handle);
            assertThat(result.getLeft()).isNull();
            assertThat(result.getRight()).isSameAs(jdbiException);
        }
    }

    @Nested
    class DescribeAutoCommit {

        private Handle handle;
        private Connection connection;

        @BeforeEach
        void setUp() {
            handle = mock(Handle.class);
            connection = mock(Connection.class);
            when(handle.getConnection()).thenReturn(connection);
        }

        @Test
        void shouldDescribeWhenSuccessGettingAutoCommitValue() throws SQLException {
            when(connection.getAutoCommit()).thenReturn(false);

            var description = Jdbi3Helpers.describeAutoCommit(handle);
            assertThat(description).isEqualTo("false");
        }

        @Test
        void shouldDescribeWhenExceptionGettingAutoCommitValue() throws SQLException {
            var exception = new SQLException("unable to get autoCommit");
            when(connection.getAutoCommit()).thenThrow(exception);

            var description = Jdbi3Helpers.describeAutoCommit(handle);
            assertThat(description).isEqualTo("ERROR (%s: %s, cause: %s)",
                    exception.getClass().getName(), exception.getMessage(), exception.getCause());
        }
    }

    @Nested
    class GetAutoCommit {

        private Handle handle;
        private Connection connection;

        @BeforeEach
        void setUp() {
            handle = mock(Handle.class);
            connection = mock(Connection.class);
            when(handle.getConnection()).thenReturn(connection);
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void shouldReturnAutoCommitValue(boolean value) throws SQLException {
            when(connection.getAutoCommit()).thenReturn(value);

            var result = Jdbi3Helpers.getAutoCommit(handle);
            assertThat(result.getLeft()).isEqualTo(value);
            assertThat(result.getRight()).isNull();
        }

        @Test
        void shouldReturnException() throws SQLException {
            var exception = new SQLException("unable to get autoCommit");
            when(connection.getAutoCommit()).thenThrow(exception);

            var result = Jdbi3Helpers.getAutoCommit(handle);
            assertThat(result.getLeft()).isNull();
            assertThat(result.getRight()).isSameAs(exception);
        }
    }

    @Nested
    class RollbackAndClose {

        private Handle handle;

        @BeforeEach
        void setUp() {
            handle = mock(Handle.class);
        }

        @Test
        void shouldRollbackAndCloseTheHandle() {
            when(handle.getConnection()).thenReturn(mock(Connection.class));

            Jdbi3Helpers.rollbackAndClose(handle, LOG);

            verify(handle, atLeastOnce()).getConnection();  // for logging autoCommit value
            verify(handle).rollback();
            verify(handle).close();
            verifyNoMoreInteractions(handle);
        }

        @Test
        void shouldCatchRollbackFailures() {
            var cause = new SQLException("Cannot rollback when autoCommit is enabled.");
            var exception = new TransactionException("Failed to rollback transaction", cause);
            when(handle.rollback()).thenThrow(exception);
            when(handle.getConnection()).thenReturn(mock(Connection.class));

            assertThatCode(() -> Jdbi3Helpers.rollbackAndClose(handle, LOG)).doesNotThrowAnyException();

            verify(handle).rollback();
            verify(handle, atLeastOnce()).getConnection();
            verify(handle).close();
            verifyNoMoreInteractions(handle);
        }

        @Test
        void shouldCatchCloseFailures() {
            var cause = new SQLException("Cannot close connection");
            var exception = new CloseException("Failed to close connection", cause);
            doThrow(exception).when(handle).close();
            when(handle.getConnection()).thenReturn(mock(Connection.class));

            assertThatCode(() -> Jdbi3Helpers.rollbackAndClose(handle, LOG)).doesNotThrowAnyException();

            verify(handle).rollback();
            verify(handle, atLeastOnce()).getConnection();
            verify(handle).close();
            verifyNoMoreInteractions(handle);
        }

        @Test
        void shouldCatchRollbackAndCloseFailures() {
            var rollbackExceptionCause = new SQLException("Cannot rollback when autoCommit is enabled.");
            var rollbackException = new TransactionException("Failed to rollback transaction", rollbackExceptionCause);
            when(handle.rollback()).thenThrow(rollbackException);

            var closeExceptionCause = new SQLException("Cannot close connection");
            var closeException = new CloseException("Failed to close connection", closeExceptionCause);
            doThrow(closeException).when(handle).close();

            when(handle.getConnection()).thenReturn(mock(Connection.class));

            assertThatCode(() -> Jdbi3Helpers.rollbackAndClose(handle, LOG)).doesNotThrowAnyException();

            verify(handle).rollback();
            verify(handle, atLeast(2)).getConnection();
            verify(handle).close();
            verifyNoMoreInteractions(handle);
        }
    }

    @AllArgsConstructor
    static class DataSourceConnectionFactory implements ConnectionFactory {
        private final DataSource dataSource;

        @Override
        public Connection openConnection() throws SQLException {
            return dataSource.getConnection();
        }
    }
}
