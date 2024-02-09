package org.kiwiproject.test.logback;

import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;

import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Wraps a {@link JoranException} with an unchecked exception.
 */
public class UncheckedJoranException extends RuntimeException {

    /**
     * Construct an instance.
     *
     * @param message the message, which can be null
     * @param cause   the cause, which cannot be null
     * @throws IllegalArgumentException if cause is null
     */
    public UncheckedJoranException(String message, JoranException cause) {
        super(message, requireNotNull(cause));
    }

    /**
     * Construct an instance.
     *
     * @param cause the cause, which cannot be null
     * @throws IllegalArgumentException if cause is null
     */
    public UncheckedJoranException(JoranException cause) {
        super(requireNotNull(cause));
    }

    /**
     * Returns the cause of this exception.
     *
     * @return the {@link JoranException} which is the cause of this exception
     */
    @Override
    public synchronized JoranException getCause() {
        return (JoranException) super.getCause();
    }
}
