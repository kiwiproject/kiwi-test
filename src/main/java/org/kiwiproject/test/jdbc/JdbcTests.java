package org.kiwiproject.test.jdbc;

import lombok.experimental.UtilityClass;

/**
 * Utilities for database testing via JDBC.
 */
@UtilityClass
public class JdbcTests {

    /**
     * Creates a new {@link SimpleSingleConnectionDataSource} for the given JDBC credentials.
     *
     * @param jdbcProperties the JDBC properties
     * @return a new instance
     */
    public static SimpleSingleConnectionDataSource newTestDataSource(JdbcProperties jdbcProperties) {
        return newTestDataSource(
                jdbcProperties.getUrl(),
                jdbcProperties.getUsername(),
                jdbcProperties.getPassword()
        );
    }

    /**
     * Creates a new {@link SimpleSingleConnectionDataSource} for the given JDBC credentials. The
     * JDBC database password is set to an empty string.
     *
     * @param url      the JDBC database URL
     * @param username the JDBC database username
     * @return a new instance
     */
    public static SimpleSingleConnectionDataSource newTestDataSource(String url, String username) {
        return new SimpleSingleConnectionDataSource(url, username);
    }

    /**
     * Creates a new {@link SimpleSingleConnectionDataSource} for the given JDBC credentials.
     *
     * @param url      the JDBC database URL
     * @param username the JDBC database username
     * @param password the JDBC database password
     * @return a new instance
     */
    public static SimpleSingleConnectionDataSource newTestDataSource(String url, String username, String password) {
        return new SimpleSingleConnectionDataSource(url, username, password);
    }
}
