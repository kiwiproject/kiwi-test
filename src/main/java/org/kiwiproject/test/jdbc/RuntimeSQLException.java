package org.kiwiproject.test.jdbc;

import org.kiwiproject.base.KiwiPreconditions;

import java.sql.SQLException;

/**
 * Unchecked exception that wraps a {@link SQLException}.
 */
public class RuntimeSQLException extends RuntimeException {

    /**
     * Constructs an instance of this class.
     *
     * @param message the detail message
     * @param cause   the {@link SQLException} which is the cause
     */
    public RuntimeSQLException(String message, SQLException cause) {
        super(message, KiwiPreconditions.requireNotNull(cause));
    }

    /**
     * Constructs an instance of this class.
     *
     * @param cause the {@link SQLException} which is the cause
     */
    public RuntimeSQLException(SQLException cause) {
        super(KiwiPreconditions.requireNotNull(cause));
    }

    /**
     * Returns the cause of this exception.
     *
     * @return the {@link SQLException} which is the cause of this exception.
     */
    @Override
    public synchronized SQLException getCause() {
        return (SQLException) super.getCause();
    }
}
