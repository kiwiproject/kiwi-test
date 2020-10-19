package org.kiwiproject.test.jdbc;

import static org.kiwiproject.base.KiwiPreconditions.requireNotBlank;
import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;
import static org.kiwiproject.base.KiwiStrings.f;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * A very simple implementation of {@link DataSource} intended to be used only during tests. As its name
 * suggests, this implementation stores a single {@link Connection} which is always returned by the
 * {@code getConnection} methods.
 * <p>
 * Note specifically that the single connection ignores calls to {@code close()}, since we do not expect the
 * code under test to close connections. To close the single connection, close this instance, which implements
 * {@link Closeable} in addition to {@link DataSource}.
 * <p>
 * The single Connection is eagerly initialized during construction since the expected usage pattern is to create an
 * object before all tests run (e.g. using a JUnit {@code @BeforeAll}). Therefore, we keep things simple by eagerly
 * initializing instead of waiting for the first {@code getConnection} call to occur.
 * <p>
 * To be very clear, this is intended to be used only during tests, and specifically in tests that execute within
 * a transaction that is rolled back once each test has executed. This ensures no data is actually stored in the
 * database making for faster tests (due to no commit overhead) and also permits testing simultaneously against
 * a shared database, for example a database setup for multiple developers or continuous integration servers to
 * run tests against.
 * <p>
 * This simple implementation does not support all of the {@link DataSource} methods. See the docs for each method.
 *
 * @implNote This is heavily influenced from and some code copied from Spring's {@code SingleConnectionDataSource}
 * but is much simpler since we do not need it to be as generic as Spring's version. We mostly used the code
 * relating to the {@link InvocationHandler} that ignores calls to close a {@code Connection}, but are only handling
 * one method {@code close} that we want to intercept.
 */
@SuppressWarnings("RedundantThrows")
@Slf4j
public class SimpleSingleConnectionDataSource implements DataSource, Closeable {

    /**
     * The test database URL to be passed to {@link DriverManager}.
     *
     * @see DriverManager#getConnection(String, String, String)
     */
    @Getter
    private final String url;

    /**
     * The test database username to be passed to {@link DriverManager}.
     *
     * @see DriverManager#getConnection(String, String, String)
     */
    @Getter
    private final String username;

    /**
     * The test database password to be passed to {@link DriverManager}.
     *
     * @see DriverManager#getConnection(String, String, String)
     */
    @Getter
    private final String password;

    /**
     * The single Connection held by this instance.
     */
    private final Connection connection;

    /**
     * A Connection proxy around {@code connection} that ignores calls to close it.
     */
    private final Connection closeSuppressingConnection;

    /**
     * Create a new SingleConnectionDataSource with the given database URL and username.
     * An empty password is supplied to {@link DriverManager}.
     * <p>
     * The single Connection is eagerly initialized in this constructor.
     *
     * @param url      the database URL
     * @param username the database username
     */
    public SimpleSingleConnectionDataSource(String url, String username) {
        this(url, username, "");
    }

    /**
     * Create a new SingleConnectionDataSource with the given database URL, username, and password
     * to be supplied to {@link DriverManager}.
     * <p>
     * The single Connection is eagerly initialized in this constructor.
     *
     * @param url      the database URL
     * @param username the database username
     * @param password the database password
     */
    public SimpleSingleConnectionDataSource(String url, String username, String password) {
        this.url = requireNotBlank(url);
        this.username = requireNotNull(username);
        this.password = requireNotNull(password);
        this.connection = getConnectionFromDriverManager(url, username, password);
        this.closeSuppressingConnection = closeSuppressingProxyFor(connection);
    }

    private static Connection getConnectionFromDriverManager(String url, String username, String password) {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new RuntimeSQLException(f("Error getting Connection for URL {} and username {}", url, username), e);
        }
    }

    /**
     * Closes the underlying single {@link Connection}. If any errors occur they are logged at WARN level but
     * no exceptions are thrown.
     */
    @Override
    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            LOG.warn("Error closing single Connection {}", connection, e);
        }
    }

    /**
     * Always returns the single Connection stored in this DataSource.
     * <p>
     * Attempts to close it will be ignored.
     *
     * @return the single Connection of this DataSource
     * @implNote Ignoring Sonar warning about getters and setters using the "expected" fields. While named like
     * a getter method, it isn't really a getter method and this rule doesn't apply.
     */
    @Override
    @SuppressWarnings("java:S4275")
    public Connection getConnection() throws SQLException {
        return closeSuppressingConnection;
    }

    /**
     * Returns the single Connection if and only if the given username and password match the ones
     * provided when this instance was constructed. It does not make sense to allow different username
     * and password for the same exact Connection.
     *
     * @param username the database username
     * @param password the database password
     * @return the single Connection of this DataSource
     * @throws SQLException if the given username and password don't match the ones assigned to this instance
     * @implNote Adapted from from Spring's {@code SingleConnectionDataSource}
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        if (Objects.equals(username, this.username) && Objects.equals(password, this.password)) {
            return getConnection();
        } else {
            throw new SQLException("SimpleSingleConnectionDataSource does not support custom username and password");
        }
    }

    /**
     * Getting a log writer is not supported.
     * <p>
     * Always throws an {@link UnsupportedOperationException}.
     */
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new UnsupportedOperationException("getLogWriter");
    }

    /**
     * Setting a log writer is not supported.
     * <p>
     * Always throws an {@link UnsupportedOperationException}.
     */
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new UnsupportedOperationException("setLogWriter");
    }

    /**
     * Setting a login timeout is not supported.
     */
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        throw new UnsupportedOperationException("setLoginTimeout");
    }

    /**
     * Returns 0, indicating the default system timeout is to be used.
     */
    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    /**
     * Return a parent logger with the JUL global logger name.
     *
     * @return a Logger with the global logger name
     * @see Logger#GLOBAL_LOGGER_NAME
     */
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }

    /**
     * Returns {@code this} when the specified class is assignment-compatible with this instance.
     *
     * @throws SQLException if the specified class is not assignment-compatible with this instance
     * @implNote This was copied from Spring's {@code SingleConnectionDataSource}.
     * @see Class#isInstance(Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return (T) this;
        }

        throw new SQLException(f("DataSource of type {} cannot be unwrapped as {}",
                getClass().getName(), iface.getName()));
    }

    /**
     * Return true when the specified class is assignment-compatible with this instance.
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    /**
     * Define a dynamic {@link Proxy} that suppresses any calls to {@link Connection#close()} on the
     * single connection. We do not want anything actually closing this connection during a test's
     * execution.
     *
     * @implNote Adapted from Spring's {@code SingleConnectionDataSource}.
     */
    private static Connection closeSuppressingProxyFor(Connection target) {
        requireNotNull(target, "Connection for proxy cannot be null");

        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new CloseSuppressingInvocationHandler(target));
    }

    /**
     * Invocation handler that suppresses close calls on JDBC Connections.
     *
     * @implNote This was adapted from Spring's {@code SingleConnectionDataSource}.
     */
    @Slf4j
    private static class CloseSuppressingInvocationHandler implements InvocationHandler {

        private final Connection target;

        CloseSuppressingInvocationHandler(Connection target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            var methodName = method.getName();
            if ("close".equals(methodName)) {
                LOG.trace("Suppressing call to close on target Connection {}", target);
                return null;
            }

            LOG.trace("Invoke method {} on target Connection {}", methodName, target);
            try {
                return method.invoke(this.target, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }
}