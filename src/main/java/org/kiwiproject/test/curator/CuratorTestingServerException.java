package org.kiwiproject.test.curator;

/**
 * Exception thrown if there are any problems encountered by {@link CuratorTestingServerExtension}.
 */
public class CuratorTestingServerException extends RuntimeException {

    public CuratorTestingServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
