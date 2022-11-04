package org.kiwiproject.test.junit.jupiter;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.kiwiproject.test.junit.jupiter.Jdbi3Helpers.buildJdbi;
import static org.kiwiproject.test.junit.jupiter.Jdbi3Helpers.configureSqlLogger;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.Map;

/**
 * A JUnit Jupiter {@link org.junit.jupiter.api.extension.Extension Extension} to easily test multiple JDBI 3-based
 * DAOs against any database and using transaction rollback to make sure tests never commit to the database.
 * <p>
 * You must supply one of three methods for obtaining a database {@link Connection}:
 * (1) a {@link DataSource}, (2) a JDBI {@link ConnectionFactory}, or
 * (3) the JDBC URL, username, and password.
 * <p>
 * Before each test, sets up a transaction and attaches the specified DAOs to the JDBI {@link Handle}.
 * After each test completes, rolls the transaction back. <em>Note specifically that all the DAOs are executing within
 * the same transaction.</em>
 * <p>
 * Tests should use the {@link #getDao(Class)} method to obtain the DAOs they want to interact with during each
 * test. Usually this will be called in a method annotated with {@link org.junit.jupiter.api.BeforeEach BeforeEach}
 * but can also be called in individual tests.
 * <p>
 * Using the builder, you can optionally specify {@link JdbiPlugin} instances to install. Note that this extension
 * always installs the {@link org.jdbi.v3.sqlobject.SqlObjectPlugin SqlObjectPlugin}.
 */
@Slf4j
public class Jdbi3MultiDaoExtension implements BeforeEachCallback, AfterEachCallback {

    /**
     * The {@link Jdbi} instance created by this extension. Not intended for you to mess with, but provided "just in
     * case" there is some good reason to do so.
     */
    @Getter
    private final Jdbi jdbi;

    /**
     * The types of DAOs that will be attached to the {@link Handle}.
     */
    @Getter
    private final List<Class<?>> daoTypes;

    /**
     * The DAO instances, attached to the {@link Handle} using {@link Handle#attach(Class)} and executing within
     * a transaction.
     */
    @Getter
    private Map<Class<?>, Object> daos;

    /**
     * The {@link Handle} instance created by this extension. Intended to be used when creating sample data required by
     * unit tests. You should generally otherwise not be messing with it.
     */
    @Getter
    private Handle handle;

    /**
     * Exactly one of the following must be supplied:
     * (1) url, username, password, (2) connectionFactory, or (3) dataSource.
     * <p>
     * Optionally, you can specify a custom name for the SLF4J Logger as well as {@link JdbiPlugin} instances to
     * install. The {@link org.jdbi.v3.sqlobject.SqlObjectPlugin} is always installed.
     *
     * @param url               The JDBC URL; paired with username & password (optional, defaults to null)
     * @param username          The JDBC username; paired with url & password (optional, defaults to null)
     * @param password          The JDBC password; paired with url & username (optional, defaults to null)
     * @param connectionFactory The JDBI {@link ConnectionFactory} (optional, defaults to null)
     * @param dataSource        The JDBC {@link DataSource} (optional, defaults to null)
     * @param slf4jLoggerName   The SLF4J {@link org.slf4j.Logger} name (optional, defaults to the FQCN of {@link Jdbi}
     * @param daoTypes          The types of JDBI 3 DAO to attach
     * @param plugins           a list containing the JDBI 3 plugins to install
     *                          (a {@link org.jdbi.v3.sqlobject.SqlObjectPlugin SqlObjectPlugin} is always installed)
     * @implNote At present the {@link org.jdbi.v3.core.statement.SqlLogger} for the given {@code slf4jLoggerName} logs
     * at TRACE level only.
     */
    @SuppressWarnings("java:S107") // builder-annotated constructors are an exception to the "too many parameters" rule
    @Builder
    private Jdbi3MultiDaoExtension(String url,
                                   String username,
                                   String password,
                                   ConnectionFactory connectionFactory,
                                   DataSource dataSource,
                                   String slf4jLoggerName,
                                   @Singular List<Class<?>> daoTypes,
                                   @Singular List<JdbiPlugin> plugins) {

        LOG.trace("A new {} is being instantiated", Jdbi3MultiDaoExtension.class.getSimpleName());

        this.daoTypes = daoTypes;

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
        LOG.trace("Setting up for JDBI multi-DAO test");

        LOG.trace("Opening handle");
        handle = jdbi.open();

        LOG.trace("Original autoCommit: {}", Jdbi3Helpers.describeAutoCommit(handle));

        LOG.trace("Attach types to handle: {}", daoTypes);

        // NOTE: Handle#attach returns a proxy Class object that implements the given DAO type. For example, calling
        // handle.attach(com.acme.dao.PersonDao.class) returns an object whose class is something like
        // com.acme.dao.$Proxy23 which implements PersonDao. But we need to keep the original DAO types as
        // the map keys, so we need to retain the DAO type in the final map, which is the reason for returning
        // a Pair containing the original DAO class and the attached DAO in the mapping operation. The
        // resulting map will contain a mapping from PersonDao.class to the DAO object that JDBI created and
        // which implements PersonDao.
        daos = daoTypes.stream()
                .map(daoType -> Pair.of(daoType, handle.attach(daoType)))
                .collect(toUnmodifiableMap(Pair::getLeft, Pair::getRight));

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

    /**
     * Tests can use this method to obtain the DAO they require.
     *
     * @param daoType the expected DAO type
     * @param <T>     the type of DAO
     * @return a DAO attached to the JDBI Handle that implements the expected DAO type
     */
    @SuppressWarnings("unchecked")
    public <T> T getDao(Class<T> daoType) {
        return (T) daos.get(daoType);
    }
}
