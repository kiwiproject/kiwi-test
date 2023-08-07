package org.kiwiproject.test.junit.jupiter;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.kiwiproject.base.KiwiStrings.f;
import static org.kiwiproject.logging.LazyLogParameterSupplier.lazy;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jdbi.v3.core.ConnectionFactory;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.kiwiproject.base.KiwiThrowables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Shared code for use by the JDBI 3 extension classes.
 */
@UtilityClass
@Slf4j
class Jdbi3Helpers {

    /**
     * Currently supports only Postgres and H2.
     *
     * @implNote This uses {@link H2DatabasePlugin} directly in order to get the H2 plugin class name because
     * it is in <a href="https://mvnrepository.com/artifact/org.jdbi/jdbi3-core">jdbi3-core</a>, whereas
     * other plugins are outside core and require a separate dependency. For example, the Postgres plugin resides
     * in <a href="https://mvnrepository.com/artifact/org.jdbi/jdbi3-postgres">jdbi3-postgres</a>, so we use
     * a String for the plugin class name to avoid a hard dependency.
     */
    enum DatabaseType {

        H2(H2DatabasePlugin.class.getName()),

        POSTGRES("org.jdbi.v3.postgres.PostgresPlugin");

        @Getter
        private final String pluginClassName;

        DatabaseType(String pluginClassName) {
            this.pluginClassName = pluginClassName;
        }

        /**
         * Given a JDBC database URL, attempt to find and instantiate a plugin.
         * <p>
         * Currently, supports only H2 and Postgres.
         *
         * @param databaseUrl the JDBC database URL
         * @return an Optional with a plugin instance or an empty Optional
         */
        static Optional<JdbiPlugin> pluginFromDatabaseUrl(String databaseUrl) {
            return databaseTypeFromDatabaseUrl(databaseUrl)
                    .flatMap(databaseType -> getPluginInstance(databaseType.getPluginClassName()));
        }

        /**
         * Determine the database type from the given JDBC database URL.
         * <p>
         * Currently, supports only H2 and Postgres.
         *
         * @param databaseUrl the JDBC database URL
         * @return an Optional containing the database type if found, otherwise an empty Optional
         */
        static Optional<DatabaseType> databaseTypeFromDatabaseUrl(String databaseUrl) {
            if (databaseUrl.startsWith("jdbc:postgresql:")) {
                return Optional.of(POSTGRES);
            } else if (databaseUrl.startsWith("jdbc:h2:")) {
                return Optional.of(H2);
            }

            return Optional.empty();
        }
    }

    /**
     * Get a plugin instance for the given class name.
     *
     * @param pluginClassName the plugin class name
     * @return an Optional containing a plugin instance, or empty Optional if plugin class not available or an error
     * occurs. If an error occurs, that fact is logged at WARN level.
     */
    static Optional<JdbiPlugin> getPluginInstance(String pluginClassName) {
        var result = isPluginAvailable(pluginClassName);

        if (isTrue(result.getLeft())) {
            try {
                var pluginClass = result.getRight();
                var pluginInstance = (JdbiPlugin) pluginClass.getDeclaredConstructor().newInstance();
                return Optional.of(pluginInstance);
            } catch (Exception e) {
                LOG.warn("Error instantiating plugin for class: {}", pluginClassName);
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private static Pair<Boolean, Class<?>> isPluginAvailable(String pluginClassName) {
        try {
            var pluginClass = Class.forName(pluginClassName);
            return Pair.of(true, pluginClass);
        } catch (ClassNotFoundException e) {
            return Pair.of(false, null);
        }
    }

    static Jdbi buildJdbi(DataSource dataSource,
                          ConnectionFactory connectionFactory,
                          String url, String username, String password,
                          List<JdbiPlugin> plugins) {

        var jdbi = buildJdbi(dataSource, connectionFactory, url, username, password);

        jdbi.installPlugin(new SqlObjectPlugin());

        findDatabasePlugin(jdbi).ifPresent(plugin -> {
            LOG.trace("Installing database plugin {}", plugin.getClass().getName());
            jdbi.installPlugin(plugin);
        });

        plugins.stream()
                .filter(not(Jdbi3Helpers::isSqlObjectPlugin))
                .forEach(jdbi::installPlugin);

        return jdbi;
    }

    static Optional<JdbiPlugin> findDatabasePlugin(Jdbi jdbi) {
        try {
            return jdbi.withHandle(handle -> {
                var connection = handle.getConnection();
                var metaData = connection.getMetaData();
                var databaseUrl = metaData.getURL();
                return DatabaseType.pluginFromDatabaseUrl(databaseUrl);
            });
        } catch (SQLException e) {
            LOG.warn("Error finding database plugin", e);
            return Optional.empty();
        }
    }

    private static boolean isSqlObjectPlugin(JdbiPlugin jdbiPlugin) {
        return jdbiPlugin instanceof SqlObjectPlugin;
    }

    private static Jdbi buildJdbi(DataSource dataSource,
                                  ConnectionFactory connectionFactory,
                                  String url, String username, String password) {

        if (nonNull(dataSource)) {
            LOG.trace("Create Jdbi from DataSource");
            return Jdbi.create(dataSource);
        }
        if (nonNull(connectionFactory)) {
            LOG.trace("Create Jdbi from ConnectionFactory");
            return Jdbi.create(connectionFactory);
        }
        if (nonNull(url) && nonNull(username) && nonNull(password)) {
            LOG.trace("Create Jdbi from URL and credentials");
            return Jdbi.create(url, username, password);
        }
        throw new IllegalArgumentException(
                "Must specify one of: (1) DataSource, (2) ConnectionFactory, or (3) URL and credentials!");
    }

    static JdbiSqlLogger configureSqlLogger(Jdbi jdbi, String slf4jLoggerName) {
        var log = new JdbiSqlLogger(slf4jLoggerName);
        jdbi.setSqlLogger(log);
        return log;
    }

    static class JdbiSqlLogger implements SqlLogger {

        private final Logger logger;

        @Getter(AccessLevel.PACKAGE)
        private final String loggerName;

        JdbiSqlLogger(String slf4jLoggerName) {
            loggerName = slf4jLoggerName(slf4jLoggerName);
            logger = LoggerFactory.getLogger(loggerName);
        }

        private static String slf4jLoggerName(String slf4jLoggerName) {
            if (isBlank(slf4jLoggerName)) {
                LOG.trace("Default SqlLogger name");
                return Jdbi.class.getName();
            }
            return slf4jLoggerName;
        }

        @Override
        public void logBeforeExecution(StatementContext context) {
            logger.trace("calling sql: {}, {}", context.getRenderedSql(),
                    lazy(() -> context.getBinding().toString()));
        }

        @Override
        public void logAfterExecution(StatementContext context) {
            logger.trace("called sql: {}, {} returned in {} ms",
                    context.getRenderedSql(),
                    lazy(() -> context.getBinding().toString()),
                    context.getElapsedTime(MILLIS));
        }

        @Override
        public void logException(StatementContext context, SQLException ex) {
            logger.trace("sql: {}, {} caused {}",
                    context.getRenderedSql(),
                    lazy(() -> context.getBinding().toString()),
                    ex.getClass().getName(),
                    ex);
        }
    }

    static String describeTransactionIsolationLevel(Handle handle) {
        var result = getTransactionIsolationLevel(handle);

        var level = result.getLeft();
        if (nonNull(level)) {
            return f("{} (java.sql.Connection isolation level: {})", level.name(), level.intValue());
        }

        var exception = result.getRight();
        var throwableInfo = KiwiThrowables.throwableInfoOfNonNull(exception);
        return f("ERROR ({}: {}, cause: {})", throwableInfo.type, throwableInfo.message, throwableInfo.cause);
    }

    /**
     * Attempt to get the transaction isolation level for the given Handle.
     *
     * @return a Pair with the transaction isolation level or the Exception that occurred
     * trying to obtain the isolation level (exactly one will be null, the other non-null)
     */
    static Pair<TransactionIsolationLevel, Exception> getTransactionIsolationLevel(Handle handle) {
        try {
            return Pair.of(handle.getTransactionIsolationLevel(), null);
        } catch (Exception e) {
            LOG.warn("Unable to get transaction isolation level", e);
            return Pair.of(null, e);
        }
    }

    static String describeAutoCommit(Handle handle) {
        var result = getAutoCommit(handle);

        var autoCommit = result.getLeft();
        if (nonNull(autoCommit)) {
            return autoCommit.toString();
        }

        var exception = result.getRight();
        var throwableInfo = KiwiThrowables.throwableInfoOfNonNull(exception);
        return f("ERROR ({}: {}, cause: {})", throwableInfo.type, throwableInfo.message, throwableInfo.cause);
    }

    /**
     * Attempt to get the autoCommit setting of the given Handle's JDBC Connection.
     *
     * @return a Pair with the autoCommit value or the Exception that occurred
     * trying to obtain the autoCommit value (exactly one will be null, the other non-null)
     */
    static Pair<Boolean, Exception> getAutoCommit(Handle handle) {
        try {
            return Pair.of(handle.getConnection().getAutoCommit(), null);
        } catch (Exception e) {
            LOG.warn("Unable to get autoCommit", e);
            return Pair.of(null, e);
        }
    }

    /**
     * Rollback and close the given Handle, suppressing exceptions that occur during rollback or close.
     * <p>
     * Exceptions are logged at WARN level to help diagnose problems. Note that if errors occur rolling
     * back and/or closing the Handle (and thus the underlying JDBC Connection), other tests may
     * fail in unexpected ways. For example, if a single Connection is used for all tests (e.g. for
     * performance reasons) and a test (accidentally?) enables autoCommit when using Postgres, the rollback
     * will fail. In this situation, data that has been auto-committed will be seen by the remaining tests
     * which may cause unexpected assertion failures, e.g. a test sees more data than it expects to see.
     */
    static void rollbackAndClose(Handle handle, Logger logger) {
        logger.trace("Tearing down after JDBI test");

        var autoCommit = Jdbi3Helpers.describeAutoCommit(handle);
        logger.trace("autoCommit before rollback: {}", autoCommit);

        logger.trace("Rollback transaction");
        tryRollback(handle, logger);

        var newAutoCommit = Jdbi3Helpers.describeAutoCommit(handle);
        logger.trace("autoCommit after rollback: {}", newAutoCommit);

        logger.trace("Close handle");
        tryClose(handle, logger);

        logger.trace("Done tearing down after JDBI test");
    }

    private static void tryRollback(Handle handle, Logger logger) {
        try {
            handle.rollback();
        } catch (Exception e) {
            logger.warn("Error in transaction rollback for Handle {} with Connection {}",
                    handle, handle.getConnection(), e);
        }
    }

    private static void tryClose(Handle handle, Logger logger) {
        try {
            handle.close();
        } catch (Exception e) {
            logger.warn("Error closing Handle {} with Connection {}",
                    handle, handle.getConnection(), e);
        }
    }
}
