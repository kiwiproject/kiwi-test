package org.kiwiproject.test.h2;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.h2.jdbcx.JdbcDataSource;
import org.kiwiproject.test.jdbc.RuntimeSQLException;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;

/**
 * Utilities for H2 databases.
 * <p>
 * This requires <a href="https://mvnrepository.com/artifact/com.h2database/h2">h2</a> to be
 * available at runtime when tests are executing.
 */
@UtilityClass
@Slf4j
public class H2DatabaseTestHelper {

    private static final String DB_DIR_PREFIX = "h2-test-db-";

    /**
     * Build a file-based H2 database in a subdirectory of the JVM temp directory, as given by the
     * {@code java.io.tmpdir} system property. The returned {@link H2FileBasedDatabase} provides the
     * directory where the file-based database is located, and provides a {@link DataSource} so that
     * clients can connect. The database is created with one sample table named {@code test_table} having
     * two columns: {@code first}, a {@code varchar}; and {@code second}, an {@code integer}.
     *
     * @return a {@link H2FileBasedDatabase} that represents the new database
     * @throws IllegalStateException if the database directory could not be created
     * @throws RuntimeSQLException   if any other error occurs
     * @see System#getProperty(String)
     */
    public static H2FileBasedDatabase buildH2FileBasedDatabase() {
        var dir = newH2DatabaseDirectory();
        var dataSource = buildH2DataSource(dir);
        return new H2FileBasedDatabase(dir, getDatabaseUrl(dir), dataSource);
    }

    private static File newH2DatabaseDirectory() {
        var javaIoTmpDir = SystemUtils.getJavaIoTmpDir();
        LOG.trace("Java IO temp dir: {}", javaIoTmpDir.getAbsolutePath());

        return new File(javaIoTmpDir, DB_DIR_PREFIX + System.nanoTime());
    }

    /**
     * Build a file-based H2 database in the specified directory.
     *
     * @param h2DatabaseDirectory the directory where the database resides
     * @return a {@link DataSource} that can be used to connect to the H2 database
     * @throws IllegalStateException if the database directory could not be created
     * @throws RuntimeSQLException   if any other error occurs
     */
    public static DataSource buildH2DataSource(File h2DatabaseDirectory) {
        try {
            Files.createDirectories(h2DatabaseDirectory.toPath());
        } catch (IOException e) {
            var message = "Could not create directory for H2 test database: " + h2DatabaseDirectory.getAbsolutePath();
            throw new IllegalStateException(message, e);
        }

        LOG.trace("H2 database dir: {}", h2DatabaseDirectory);
        return buildH2DatabaseWithTestTable(h2DatabaseDirectory);
    }

    @SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
    private static DataSource buildH2DatabaseWithTestTable(File h2DatabaseDirectory) {
        var url = getDatabaseUrl(h2DatabaseDirectory);
        LOG.trace("Create test database with URL: {}", url);

        var dataSource = new JdbcDataSource();
        dataSource.setURL(url);

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement("create table test_table (col_1 varchar , col_2 integer)")
        ) {
            ps.execute();
            LOG.trace("Successfully created test_table");
            return dataSource;
        } catch (SQLException e) {
            throw new RuntimeSQLException(e);
        }
    }

    /**
     * Return a JDBC URL that can be used to connect to the H2 file-based database in the given directory.
     *
     * @param h2DatabaseDirectory the directory where the database resides
     * @return the JDBC database URL
     */
    public static String getDatabaseUrl(File h2DatabaseDirectory) {
        return "jdbc:h2:" + h2DatabaseDirectory.getAbsolutePath() + "/testdb";
    }
}
