package org.kiwiproject.test.junit.jupiter;

import static java.util.Objects.isNull;
import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;
import static org.kiwiproject.test.junit.jupiter.Jdbi3Helpers.buildJdbi;
import static org.kiwiproject.test.junit.jupiter.Jdbi3Helpers.configureSqlLogger;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.ConnectionFactory;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

/**
 * A JUnit Jupiter {@link org.junit.jupiter.api.extension.Extension Extension} to easily test JDBI 3-based DAOs in
 * against any database and using transaction rollback to make sure tests never commit to the database.
 * <p>
 * You must supply the {@code daoType} and one of three methods for obtaining a database {@link Connection}:
 * (1) a {@link DataSource}, (2) a JDBI {@link ConnectionFactory}, or
 * (3) the JDBC URL, username, and password.
 * <p>
 * Before each test, sets up a transaction. After each test completes, rolls the transaction back.
 * <p>
 * Using the builder, you can optionally specify {@link JdbiPlugin} instances to install. Note that this extension
 * always installs the {@link org.jdbi.v3.sqlobject.SqlObjectPlugin SqlObjectPlugin}.
 *
 * @param <T> the DAO type
 */
@Slf4j
public class Jdbi3DaoExtension<T> implements BeforeEachCallback, AfterEachCallback {

    /**
     * The type of DAO (<em>required</em>)
     */
    @Getter
    private final Class<T> daoType;

    /**
     * The {@link Jdbi} instance created by this extension. Not intended for you to mess with, but provided "just in
     * case" there is some good reason to do so.
     */
    @Getter
    private final Jdbi jdbi;

    /**
     * The {@link Handle} instance created by this extension. Intended to be used when creating sample data required by
     * unit tests. You should generally otherwise not be messing with it.
     */
    @Getter
    private Handle handle;

    /**
     * The DAO test subject, attached to the {@link Handle} using {@link Handle#attach(Class)} and executing within
     * a transaction.
     * <p>
     * Obviously intended to be used in tests.
     */
    @Getter
    private T dao;

    /**
     * The DAO type is always required.
     * <p>
     * Exactly one of the following must be supplied:
     * (1) url, username, password, (2) connectionFactory, or (3) dataSource.
     * <p>
     * Optionally, you can specify a custom name for the SLF4J Logger as well as {@link JdbiPlugin} instances to
     * install. The {@link org.jdbi.v3.sqlobject.SqlObjectPlugin} is always installed.
     *
     * @param url               The JDBC URL; paired with username and password (optional, defaults to null)
     * @param username          The JDBC username; paired with url and password (optional, defaults to null)
     * @param password          The JDBC password; paired with url and username (optional, defaults to null)
     * @param connectionFactory The JDBI {@link ConnectionFactory} (optional, defaults to null)
     * @param dataSource        The JDBC {@link DataSource} (optional, defaults to null)
     * @param slf4jLoggerName   The SLF4J {@link org.slf4j.Logger} name (optional, defaults to the FQCN of {@link Jdbi}
     * @param daoType           The type of JDBI 3 DAO
     * @param plugins           a list containing the JDBI 3 plugins to install
     *                          (a {@link org.jdbi.v3.sqlobject.SqlObjectPlugin SqlObjectPlugin} is always installed)
     * @implNote At present the {@link org.jdbi.v3.core.statement.SqlLogger} for the given {@code slf4jLoggerName} logs
     * at TRACE level only.
     */
    @SuppressWarnings("java:S107") // builder-annotated constructors are an exception to the "too many parameters" rule
    @Builder
    private Jdbi3DaoExtension(String url,
                              String username,
                              String password,
                              ConnectionFactory connectionFactory,
                              DataSource dataSource,
                              String slf4jLoggerName,
                              Class<T> daoType,
                              @Singular List<JdbiPlugin> plugins) {

        LOG.trace("A new {} is being instantiated", Jdbi3DaoExtension.class.getSimpleName());

        this.daoType = requireNotNull(daoType, "Must specify the DAO type");

        var nonNullPlugins = isNull(plugins) ? List.<JdbiPlugin>of() : plugins;
        this.jdbi = buildJdbi(dataSource, connectionFactory, url, username, password, nonNullPlugins);

        configureSqlLogger(jdbi, slf4jLoggerName);
    }

    /**
     * Create a DAO attached to the {@link Handle} and assigns it; it is accessible to tests via {@code getDao()}.
     * Begins a transaction.
     *
     * @param context the extension context
     * @see Jdbi#open()
     * @see Handle#attach(Class)
     * @see Handle#begin()
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        LOG.trace("Setting up for JDBI DAO test");

        LOG.trace("Opening handle");
        handle = jdbi.open();

        LOG.trace("Original autoCommit: {}", Jdbi3Helpers.describeAutoCommit(handle));

        LOG.trace("Attach type {} to handle", daoType);
        dao = handle.attach(daoType);

        LOG.trace("Beginning transaction");
        handle.begin();

        LOG.trace("Transaction isolation level: {}", Jdbi3Helpers.describeTransactionIsolationLevel(handle));
        LOG.trace("autoCommit in transaction: {}", Jdbi3Helpers.describeAutoCommit(handle));

        LOG.trace("Done setting up for JDBI DAO test");
    }

    /**
     * Rolls back the transaction on the {@link Handle} and closes it.
     *
     * @param context the extension context
     * @see Handle#rollback()
     * @see Handle#close()
     */
    @Override
    public void afterEach(ExtensionContext context) {
        Jdbi3Helpers.rollbackAndClose(handle, LOG);
    }
}
