package org.kiwiproject.test.mockito;

import static org.mockito.Mockito.mock;

import lombok.experimental.UtilityClass;

/**
 * Static utility methods for creating Mockito mocks with specific behaviors.
 */
@UtilityClass
public class MockitoHelpers {

    /**
     * Creates a {@link org.mockito.Mock mock} of the given class that throws
     * {@link AssertionError} if any method is invoked.
     * <p>
     * Use this when the mock should never be called during the test. The error message will
     * include the class name and the name of the method that was unexpectedly called.
     *
     * @param classToMock the class to mock
     * @param <T>         the type to mock
     * @return a new mock instance
     */
    public static <T> T mockNeverCalled(Class<T> classToMock) {
        return mock(classToMock, invocation -> {
            throw new AssertionError("No methods should be called on this " +
                    classToMock.getSimpleName() + " mock, but " +
                    invocation.getMethod().getName() + "() was called");
        });
    }

    /**
     * Creates a {@link org.mockito.Mock mock} of the given class that throws
     * {@link AssertionError} with the given message if any method is invoked.
     *
     * @param classToMock the class to mock
     * @param message     the AssertionError message
     * @param <T>         the type to mock
     * @return a new mock instance
     */
    public static <T> T mockNeverCalled(Class<T> classToMock, String message) {
        return mock(classToMock, invocation -> {
            throw new AssertionError(message);
        });
    }
}
