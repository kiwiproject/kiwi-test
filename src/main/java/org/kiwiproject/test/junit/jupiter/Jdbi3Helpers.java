package org.kiwiproject.test.junit.jupiter;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.isBlank;
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
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
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
         * Currently supports only H2 and Postgres.
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
         * Currently supports only H2 and Postgres.
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

    private static Optional<JdbiPlugin> findDatabasePlugin(Jdbi jdbi) {
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
                    ex);
        }
    }

    static void rollbackAndClose(Handle handle, Logger logger) {
        logger.trace("Tearing down after JDBI test");

        logger.trace("Rollback transaction");
        handle.rollback();

        logger.trace("Close handle");
        handle.close();

        logger.trace("Done tearing down after JDBI test");
    }
}
