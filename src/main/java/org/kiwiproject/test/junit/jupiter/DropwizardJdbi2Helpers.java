package org.kiwiproject.test.junit.jupiter;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.annotations.VisibleForTesting;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.kiwiproject.test.dropwizard.jdbi2.DropwizardJdbi;
import org.kiwiproject.test.jdbc.RuntimeSQLException;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.logging.SLF4JLog;
import org.skife.jdbi.v2.tweak.ConnectionFactory;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.TimeZone;

/**
 * Shared code for use by the JDBI 2 extension classes.
 */
@UtilityClass
@Slf4j
class DropwizardJdbi2Helpers {

    static DBI buildDBI(DataSource dataSource,
                        ConnectionFactory connectionFactory,
                        String url, String username, String password) {

        if (nonNull(dataSource)) {
            LOG.trace("Create DBI from DataSource");
            return new DBI(dataSource);
        }
        if (nonNull(connectionFactory)) {
            LOG.trace("Create DBI from ConnectionFactory");
            return new DBI(connectionFactory);
        }
        if (nonNull(url) && nonNull(username) && nonNull(password)) {
            LOG.trace("Create DBI from URL and credentials");
            return new DBI(url, username, password);
        }

        throw new IllegalArgumentException(
                "Must specify one of: (1) DataSource, (2) ConnectionFactory, or (3) URL and credentials!");
    }

    static Handle configureDBI(DBI dbi,
                               String slf4jLoggerName,
                               SLF4JLog.Level slfLogLevel,
                               String driverClass,
                               TimeZone databaseTimeZone) {

        var logger = LoggerFactory.getLogger(slf4jLoggerName(slf4jLoggerName));
        var logLevel = slf4jLogLevel(slfLogLevel);
        var log = new SLF4JLog(logger, logLevel);
        dbi.setSQLLog(log);

        DropwizardJdbi.registerDefaultDropwizardJdbiFeatures(dbi, driverClass(driverClass), databaseTimeZone);

        LOG.trace("Get a Handle on it!");
        var handle = dbi.open();
        try {
            var connection = handle.getConnection();
            LOG.trace("Set auto-commit to false on handle's underlying connection: {}", connection);
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeSQLException(e);
        }

        LOG.trace("Txn isolation level: {}", handle.getTransactionIsolationLevel());
        LOG.trace("Done one-time JDBI initialization");

        return handle;
    }

    @VisibleForTesting
    static String slf4jLoggerName(String slf4jLoggerName) {
        if (isBlank(slf4jLoggerName)) {
            LOG.trace("Default SQLLog name");
            return DBI.class.getName();
        }

        return slf4jLoggerName;
    }

    @VisibleForTesting
    static SLF4JLog.Level slf4jLogLevel(SLF4JLog.Level slfLogLevel) {
        if (isNull(slfLogLevel)) {
            LOG.trace("Default to TRACE level for SLF4JLog");
            return SLF4JLog.Level.TRACE;
        }

        return slfLogLevel;
    }

    private static String driverClass(String driverClass) {
        if (isBlank(driverClass)) {
            return findAvailableDriver();
        }
        return driverClass;
    }

    private static String findAvailableDriver() {
        LOG.trace("No driver class specified; using the first one returned by DriverManager");
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        String driverCls = null;
        if (drivers.hasMoreElements()) {
            driverCls = drivers.nextElement().getClass().getName();
        }

        if (isBlank(driverCls)) {
            LOG.warn("Did not find any available drivers; Dropwizard's OptionalArgumentFactory and" +
                    " GuavaOptionalArgumentFactory both need a driver for SQLServer and Oracle databases, which you" +
                    " may or may not care about.");
        } else {
            LOG.debug("Using {} as the driver class. If this is not correct (b/c you have multiple drivers available), " +
                    " then you should explicitly specify one when building this extension", driverCls);
        }

        return driverCls;
    }
}
