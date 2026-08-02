package org.kiwiproject.test.liquibase;

import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;

import liquibase.exception.LiquibaseException;

/**
 * Wraps a {@link LiquibaseException} with an unchecked exception.
 */
public class UncheckedLiquibaseException extends RuntimeException {

    /**
     * Construct an instance.
     *
     * @param message the message, which can be null
     * @param cause   the cause, which cannot be null
     * @throws IllegalArgumentException if {@code cause} is null
     */
    public UncheckedLiquibaseException(String message, LiquibaseException cause) {
        super(message, requireNotNull(cause));
    }

    /**
     * Construct an instance.
     *
     * @param cause the cause, which cannot be null
     * @throws IllegalArgumentException if {@code cause} is null
     */
    public UncheckedLiquibaseException(LiquibaseException cause) {
        super(requireNotNull(cause));
    }

    /**
     * Returns the cause of this exception.
     *
     * @return the {@link LiquibaseException} which is the cause of this exception
     */
    @Override
    public synchronized LiquibaseException getCause() {
        return (LiquibaseException) super.getCause();
    }
}
