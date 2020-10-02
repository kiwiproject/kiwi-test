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
 */
@UtilityClass
@Slf4j
public class H2DatabaseTestHelper {

    private static final String DB_DIR_PREFIX = "h2-test-db-";

    /**
     * Build a file-based H2 database in a subdirectory of the JVM temp directory, as given by the
     * {@code java.io.tmpdir} system property.
     *
     * @return a {@link H2FileBasedDatabase} that represents the new database
     * @throws IllegalStateException if the database directory could not be created
     * @throws RuntimeSQLException   if any other error occurs
     * @see System#getProperty(String)
     */
    public static H2FileBasedDatabase buildH2FileBasedDatabase() {
        var dir = newH2DatabaseDirectory();
        var dataSource = buildH2DataSource(dir);
        return new H2FileBasedDatabase(dir, dataSource);
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
        var url = "jdbc:h2:" + h2DatabaseDirectory.getAbsolutePath() + "/testdb";
        LOG.info("Create test database with URL: {}", url);

        var dataSource = new JdbcDataSource();
        dataSource.setURL(url);

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement("create table test_table (first varchar , second integer)")
        ) {
            ps.execute();
            return dataSource;
        } catch (SQLException e) {
            throw new RuntimeSQLException(e);
        }
    }
}
