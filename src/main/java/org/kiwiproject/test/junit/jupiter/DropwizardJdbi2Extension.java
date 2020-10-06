package org.kiwiproject.test.junit.jupiter;

import static org.kiwiproject.test.junit.jupiter.DropwizardJdbi2Helpers.buildDBI;
import static org.kiwiproject.test.junit.jupiter.DropwizardJdbi2Helpers.configureDBI;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.test.dropwizard.jdbi2.DropwizardJdbi;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.logging.SLF4JLog;
import org.skife.jdbi.v2.tweak.ConnectionFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.TimeZone;

/**
 * A JUnit Jupiter {@link org.junit.jupiter.api.extension.Extension} to easily test JDBI 2 DAOs using the "Fluent API"
 * (as opposed to the SQL Objects API) in a Dropwizard app against any database and using transaction rollback to make
 * sure tests never commit to the database. Uses {@link DropwizardJdbi} to register the  default set of Dropwizard
 * {@link org.skife.jdbi.v2.tweak.ArgumentFactory}, {@link org.skife.jdbi.v2.tweak.ResultColumnMapper}, and
 * {@link org.skife.jdbi.v2.tweak.ContainerFactory} on the {@link DBI} instance used for tests.
 * <p>
 * You must supply one of three methods for obtaining a database {@link Connection}:
 * (1) a {@link DataSource}, (2) a JDBI {@link ConnectionFactory}, or
 * (3) the JDBC URL, username, and password.
 * <p>
 * Before each tests, sets up a transaction. After each test completes, rolls the transaction back.
 * <p>
 * <strong>NOTE: If the DAO under test creates its own {@link Handle}s, then you need to make sure that you always use
 * the <em>same connection</em> otherwise there will be transaction isolation issues. Prefer using the {@link Handle}
 * provided by this extension.</strong>
 */
@Slf4j
public class DropwizardJdbi2Extension implements AfterEachCallback {

    /**
     * The {@link DBI} instance created by this extension. You can pass this to DAOs that use {@link DBI} directly and
     * are not using SQL Object-style DAOs.
     */
    @Getter
    private final DBI dbi;

    /**
     * The {@link Handle} instance created by this extension. Intended to be used when creating sample data required by
     * unit tests, as well as to supply to the data access code (e.g. DAO) that is being tested. You should generally
     * otherwise not be messing with it.
     */
    @Getter
    private final Handle handle;

    /**
     * Exactly one of the following must be supplied:
     * (1) url, username, password, (2) connectionFactory, or (3) dataSource.
     *
     * @param url               The JDBC URL; paired with username & password (optional, defaults to empty)
     * @param username          The JDBC username; paired with url & password (optional, defaults to empty)
     * @param password          The JDBC password; paired with url & username (optional, defaults to empty)
     * @param connectionFactory The JDBI {@link ConnectionFactory} (optional, defaults to empty)
     * @param dataSource        The JDBC {@link DataSource}
     * @param driverClass       The JDBC driver class, which will be supplied to Dropwizard's
     *                          {@link io.dropwizard.jdbi.args.OptionalArgumentFactory}
     *                          and {@link io dropwizard.jdbi.args.GuavaOptionalArgumentFactory}
     *                          (optional, defaults to first driver found using {@link java.sql.DriverManager#getDrivers()})
     * @param databaseTimeZone  The database time zone. Passed to Guava and Java 8 date/time argument factories
     *                          (optional, default to empty)
     * @param slfLogLevel       The SLF4J log level to use for the {@link DBI} instance
     *                          (optional, defaults to {@link SLF4JLog.Level#TRACE})
     * @param slf4jLoggerName   The SLF4J {@link org.slf4j.Logger} name (optional, defaults to the FQCN of {@link DBI})
     * @implNote See {@code io.dropwizard.jdbi.DBIFactory#databaseTimeZone()} which has protected access
     */
    @SuppressWarnings("java:S107") // builder-annotated constructors are an exception to the "too many parameters" rule
    @Builder
    private DropwizardJdbi2Extension(String url,
                                     String username,
                                     String password,
                                     ConnectionFactory connectionFactory,
                                     DataSource dataSource,
                                     String driverClass,
                                     TimeZone databaseTimeZone,
                                     SLF4JLog.Level slfLogLevel,
                                     String slf4jLoggerName) {

        LOG.trace("A new {} is being instantiated", DropwizardJdbi2Extension.class.getSimpleName());
        this.dbi = buildDBI(dataSource, connectionFactory, url, username, password);
        this.handle = configureDBI(dbi, slf4jLoggerName, slfLogLevel, driverClass, databaseTimeZone);
    }

    /**
     * Rolls back the transaction on the {@link Handle}.
     *
     * @param context the extension context
     * @see Handle#rollback()
     */
    @Override
    public void afterEach(ExtensionContext context) {
        LOG.trace("Tearing down after JDBI test");

        LOG.trace("Rollback transaction");
        handle.rollback();

        LOG.trace("Done tearing down after JDBI test");
    }
}
