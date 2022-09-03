package org.kiwiproject.test.junit.jupiter;

import static java.util.Objects.isNull;
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
 * A JUnit Jupiter {@link org.junit.jupiter.api.extension.Extension Extension} to easily test JDBI 3 DAOs using the
 * "Fluent API" (as opposed to the SQL Objects API) against any database and using transaction rollback to make sure
 * tests never commit to the database.
 * <p>
 * You must supply one of three methods for obtaining a database {@link Connection}:
 * (1) a {@link DataSource}, (2) a JDBI {@link ConnectionFactory}, or
 * (3) the JDBC URL, username, and password.
 * <p>
 * Before each test, sets up a transaction. After each test completes, rolls the transaction back.
 * <p>
 * <strong>NOTE: If the DAO under test creates its own {@link Handle}s, then you need to make sure that you always use
 * the <em>same connection</em> otherwise there will be transaction isolation issues. Prefer using the {@link Handle}
 * provided by this extension.</strong>
 */
@Slf4j
public class Jdbi3Extension implements BeforeEachCallback, AfterEachCallback {

    /**
     * The {@link Jdbi} instance created by this extension. You can pass this to DAOs that use {@link Jdbi} directly
     * and which are not using SQL Object-style DAOs.
     */
    @Getter
    private final Jdbi jdbi;

    /**
     * The {@link Handle} instance created by this extension. Intended to be used when creating sample data required by
     * unit tests, and to be supplied to DAO methods which require a {@link Handle}. You should generally otherwise not
     * be messing with it.
     */
    @Getter
    private Handle handle;

    /**
     * Exactly one of the following must be supplied:
     * (1) url, username, password, (2) connectionFactory, or (3) dataSource.
     * <p>
     * Optionally, you can specify a custom name for the SLF4J Logger as well as {@link JdbiPlugin} instances to
     * install. The {@link org.jdbi.v3.sqlobject.SqlObjectPlugin SqlObjectPlugin} is always installed.
     *
     * @param url               The JDBC URL; paired with username & password (optional, defaults to null)
     * @param username          The JDBC username; paired with url & password (optional, defaults to null)
     * @param password          The JDBC password; paired with url & username (optional, defaults to null)
     * @param connectionFactory The JDBI {@link ConnectionFactory} (optional, defaults to null)
     * @param dataSource        The JDBC {@link DataSource} (optional, defaults to null)
     * @param slf4jLoggerName   The SLF4J {@link org.slf4j.Logger} name (optional, defaults to the FQCN of {@link Jdbi}
     * @param plugins           a list containing the JDBI 3 plugins to install
     *                          (a {@link org.jdbi.v3.sqlobject.SqlObjectPlugin SqlObjectPlugin} is always installed)
     * @implNote At present the {@link org.jdbi.v3.core.statement.SqlLogger SqlLogger} for the given
     * {@code slf4jLoggerName} logs at TRACE level only.
     */
    @SuppressWarnings("java:S107") // builder-annotated constructors are an exception to the "too many parameters" rule
    @Builder
    private Jdbi3Extension(String url,
                           String username,
                           String password,
                           ConnectionFactory connectionFactory,
                           DataSource dataSource,
                           String slf4jLoggerName,
                           @Singular List<JdbiPlugin> plugins) {

        LOG.trace("A new {} is being instantiated", Jdbi3Extension.class.getSimpleName());

        var nonNullPlugins = isNull(plugins) ? List.<JdbiPlugin>of() : plugins;
        this.jdbi = buildJdbi(dataSource, connectionFactory, url, username, password, nonNullPlugins);

        configureSqlLogger(jdbi, slf4jLoggerName);
    }

    /**
     * Opens a {@link Handle} and begins a transaction.
     *
     * @param context the extension context
     * @see Jdbi#open()
     * @see Handle#begin()
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        LOG.trace("Setting up for JDBI test");

        LOG.trace("Opening handle");
        handle = jdbi.open();

        LOG.trace("Txn isolation level: {}", handle.getTransactionIsolationLevel());

        LOG.trace("Beginning transaction");
        handle.begin();

        LOG.trace("Done setting up for JDBI test");
    }

    /**
     * Rolls back the transaction and closes the {@link Handle}.
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
