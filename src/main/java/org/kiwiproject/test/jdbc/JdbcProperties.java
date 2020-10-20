package org.kiwiproject.test.jdbc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * A simple value class for holding basic JDBC connection properties. Provides an all-args constructor
 * and a builder for construction. Does not perform any validation.
 */
@Value
@Builder
@AllArgsConstructor
public class JdbcProperties {

    /**
     * The JDBC database URL.
     */
    String url;

    /**
     * The JDBC database username.
     */
    String username;

    /**
     * The JDBC database password.
     */
    String password;
}
