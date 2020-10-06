package org.kiwiproject.test.h2;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.sql.DataSource;
import java.io.File;

/**
 * Value class representing a file-based H2 database intended for use in tests. It contains the directory where the
 * database files reside and a {@link DataSource} that can be used to connect to the database. It also provides the
 * JDBC URL for the database. The username and password to use with the JDBC URL should be empty strings, e.g.
 * {@code DriverManager.getConnection(h2Database.getUrl(), "", "")}.
 */
@AllArgsConstructor
@Getter
public class H2FileBasedDatabase {
    private final File directory;
    private final String url;
    private final DataSource dataSource;
}
