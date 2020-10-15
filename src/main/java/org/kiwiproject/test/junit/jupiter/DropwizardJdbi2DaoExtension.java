package org.kiwiproject.test.junit.jupiter;

import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;
import static org.kiwiproject.test.junit.jupiter.DropwizardJdbi2Helpers.buildDBI;
import static org.kiwiproject.test.junit.jupiter.DropwizardJdbi2Helpers.configureDBI;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
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
 * A JUnit Jupiter {@link org.junit.jupiter.api.extension.Extension} to easily test JDBI 2-based DAOs in a Dropwizard
 * app against any database and using transaction rollback to make sure tests never commit to the database. Uses
 * {@link DropwizardJdbi} to register the default set of Dropwizard {@link org.skife.jdbi.v2.tweak.ArgumentFactory},
 * {@link org.skife.jdbi.v2.tweak.ResultColumnMapper}, and {@link org.skife.jdbi.v2.tweak.ContainerFactory} on the
 * {@link DBI} instance used for tests.
 * <p>
 * You must supply the {@code daoType} and one of three methods for obtaining a database {@link Connection}:
 * (1) a {@link DataSource}, (2) a JDBI 2 {@link ConnectionFactory}, or
 * (3) the JDBC URL, username, and password.
 * <p>
 * Before each tests, sets up a transaction. After each test completes, rolls the transaction back.
 *
 * @param <T> the DAO type
 */
@Slf4j
public class DropwizardJdbi2DaoExtension<T> implements BeforeEachCallback, AfterEachCallback {

    /**
     * The type of DAO (<em>required</em>)
     */
    @Getter
    private final Class<T> daoType;

    /**
     * The {@link DBI} instance created by this extension. Not intended for you to mess with, but provided
     * "just in case" there is some good reason to do so.
     */
    @Getter
    private final DBI dbi;

    /**
     * The {@link Handle} instance created by this extension. Intended to be used when creating sample data required by
     * unit tests. You should generally otherwise not be messing with it.
     */
    @Getter
    private final Handle handle;

    /**
     * The DAO test subject, attached to the {@link Handle} using {@link Handle#attach(Class)} and executing within
     * a transaction.
     * <p>
     * Obviously intended to be used in tests.
     */
    @Getter
    private T dao;

    /**
     * Exactly one of the following must be supplied to the builder:
     * (1) url, username, password, (2) connectionFactory, or (3) dataSource.
     *
     * @param url               The JDBC URL; paired with username & password (optional, defaults to null)
     * @param username          The JDBC username; paired with url & password (optional, defaults to null)
     * @param password          The JDBC password; paired with url & username (optional, defaults to null)
     * @param connectionFactory The JDBI {@link ConnectionFactory} (optional, defaults to null)
     * @param dataSource        The JDBC {@link DataSource} (optional, defaults to null)
     * @param driverClass       The JDBC driver class, which will be supplied
     *                          to Dropwizard's {@link io.dropwizard.jdbi.args.OptionalArgumentFactory}
     *                          and {@link io.dropwizard.jdbi.args.GuavaOptionalArgumentFactory}
     *                          (optional, defaults to first driver found using {@link java.sql.DriverManager#getDrivers()})
     * @param databaseTimeZone  The database time zone. Passed to Guava and Java 8 date/time argument factories
     *                          (optional, default to empty)
     * @param slfLogLevel       The SLF4J log level to use for the {@link DBI} instance
     *                          (optional, defaults to {@link SLF4JLog.Level#TRACE})
     * @param slf4jLoggerName   The SLF4J {@link org.slf4j.Logger} name (optional, defaults to the FQCN of {@link DBI})
     * @param daoType           The type of JDBI 2 DAO
     * @implNote See {@code io.dropwizard.jdbi.DBIFactory#databaseTimeZone()} which has protected access
     */
    @SuppressWarnings("java:S107") // builder-annotated constructors are an exception to the "too many parameters" rule
    @Builder
    private DropwizardJdbi2DaoExtension(String url,
                                        String username,
                                        String password,
                                        ConnectionFactory connectionFactory,
                                        DataSource dataSource,
                                        String driverClass,
                                        TimeZone databaseTimeZone,
                                        SLF4JLog.Level slfLogLevel,
                                        String slf4jLoggerName,
                                        Class<T> daoType) {

        LOG.trace("A new {} is being instantiated", DropwizardJdbi2DaoExtension.class.getSimpleName());
        this.daoType = requireNotNull(daoType, "Must specify the DAO type");
        this.dbi = buildDBI(dataSource, connectionFactory, url, username, password);
        this.handle = configureDBI(dbi, slf4jLoggerName, slfLogLevel, driverClass, databaseTimeZone);
    }

    /**
     * Create a DAO attached to the {@link Handle} and assigns it; it is accessible to tests via {@code getDao()}.
     *
     * @param context the extension context
     * @see Handle#attach(Class)
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        LOG.trace("Setting up for JDBI DAO test");

        LOG.trace("Attach type {} to handle", daoType);
        dao = handle.attach(daoType);

        LOG.trace("Done setting up for JDBI DAO test");
    }

    /**
     * Rolls back the transaction on the {@link Handle} and closes the DAO.
     *
     * @param context the extension context
     * @see Handle#rollback()
     * @see DBI#close(Object)
     */
    @Override
    public void afterEach(ExtensionContext context) {
        LOG.trace("Tearing down after JDBI test");

        LOG.trace("Rollback transaction");
        handle.rollback();

        LOG.trace("Close DAO {}", dao);
        dbi.close(dao);

        LOG.trace("Done tearing down after JDBI test");
    }
}
