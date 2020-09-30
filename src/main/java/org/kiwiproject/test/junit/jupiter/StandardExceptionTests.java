package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.kiwiproject.base.KiwiStrings.f;

import lombok.experimental.UtilityClass;
import org.junit.jupiter.api.DynamicTest;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Utilities to generate {@link DynamicTest}s for testing "standard" exceptions, where "standard" is defined as
 * an exception class that is a subclass of {@link Exception} and contains the same four public constructors that
 * do nothing but delegate to the superclass implementations. Any custom logic cannot be known in advanced, so only
 * this "standard" behavior is tested here.
 * <p>
 * Example usage:
 * <pre>
 *    {@literal @}TestFactory
 *     Collection&lt;DynamicTest&gt; shouldHaveStandardConstructors() {
 *         return standardConstructorTestsFor(YourCustomException.class);
 *     }
 * </pre>
 * JUnit will then execute all of the dynamic tests.
 * <p>
 * You can also use the methods that return a single {@link DynamicTest}.
 */
@UtilityClass
public class StandardExceptionTests {

    /**
     * Generate a collection of dynamic tests that asserts a given class conforms to a "standard" exception
     * containing the same four public constructor API as defined by {@link Exception}.
     *
     * @param exceptionClass the exception class
     * @return dynamic tests for each of the four standard constructors
     */
    public static Collection<DynamicTest> standardConstructorTestsFor(Class<? extends Exception> exceptionClass) {
        return List.of(
                noArgConstructorDynamicTest(exceptionClass),
                messageConstructorDynamicTest(exceptionClass),
                messageAndCauseConstructorDynamicTest(exceptionClass),
                causeConstructorDynamicTest(exceptionClass)
        );
    }

    /**
     * Generate a {@link DynamicTest} for a no-argument exception constructor.
     *
     * @param exceptionClass the exception class
     * @return a dynamic test for a no-argument constructor
     * @see Exception#Exception()
     */
    public static DynamicTest noArgConstructorDynamicTest(Class<? extends Exception> exceptionClass) {
        var displayName = displayName(exceptionClass, "()");
        return dynamicTest(displayName, () -> {
            var exception = newInstanceUsingNoArgCtor(displayName, exceptionClass);

            assertThat(exception)
                    .isExactlyInstanceOf(exceptionClass)
                    .hasMessage(null)
                    .hasNoCause();
        });
    }

    private static Exception newInstanceUsingNoArgCtor(String displayName,
                                                       Class<? extends Exception> exceptionClass) {
        try {
            var constructor = exceptionClass.getConstructor();
            return constructor.newInstance();
        } catch (Exception e) {
            throw newAssertionError(displayName, e);
        }
    }

    /**
     * Generate a {@link DynamicTest} for an exception constructor accepting a String message.
     *
     * @param exceptionClass the exception class
     * @return a dynamic test for a constructor containing a message
     * @see Exception#Exception(String)
     */
    public static DynamicTest messageConstructorDynamicTest(Class<? extends Exception> exceptionClass) {
        var displayName = displayName(exceptionClass, "(String message)");
        return dynamicTest(displayName, () -> {
            var message = "An error occurred";
            var exception = newInstanceUsingMessageCtor(displayName, exceptionClass, message);

            assertThat(exception)
                    .isExactlyInstanceOf(exceptionClass)
                    .hasMessage(message)
                    .hasNoCause();
        });
    }

    private static Exception newInstanceUsingMessageCtor(String displayName,
                                                         Class<? extends Exception> exceptionClass,
                                                         String message) {
        try {
            var constructor = exceptionClass.getConstructor(String.class);
            return constructor.newInstance(message);
        } catch (Exception e) {
            throw newAssertionError(displayName, e);
        }
    }

    /**
     * Generate a {@link DynamicTest} for an exception constructor accepting a String message and Throwable cause.
     *
     * @param exceptionClass the exception class
     * @return a dynamic test for a constructor containing a message and cause
     * @see Exception#Exception(String, Throwable)
     */
    public static DynamicTest messageAndCauseConstructorDynamicTest(Class<? extends Exception> exceptionClass) {
        var displayName = displayName(exceptionClass, "(String message, Throwable cause)");
        return dynamicTest(displayName, () -> {
            var message = "An I/O related error occurred";
            var cause = new IOException("I/O error");
            var exception = newInstanceUsingMessageAndCauseCtor(displayName, exceptionClass, message, cause);

            assertThat(exception)
                    .isExactlyInstanceOf(exceptionClass)
                    .hasMessage(message)
                    .hasCause(cause);
        });
    }

    private static Exception newInstanceUsingMessageAndCauseCtor(String displayName,
                                                                 Class<? extends Exception> exceptionClass,
                                                                 String message,
                                                                 IOException cause) {
        try {
            var constructor = exceptionClass.getConstructor(String.class, Throwable.class);
            return constructor.newInstance(message, cause);
        } catch (Exception e) {
            throw newAssertionError(displayName, e);
        }
    }

    /**
     * Generate a {@link DynamicTest} for an exception constructor accepting a Throwable cause.
     *
     * @param exceptionClass the exception class
     * @return a dynamic test for a constructor containing a cause
     * @see Exception#Exception(Throwable)
     */
    public static DynamicTest causeConstructorDynamicTest(Class<? extends Exception> exceptionClass) {
        var displayName = displayName(exceptionClass, "(Throwable cause)");
        return dynamicTest(displayName, () -> {
            var cause = new IOException("An unexpected I/O error occurred");
            var exception = newInstanceUsingCauseCtor(displayName, exceptionClass, cause);

            assertThat(exception)
                    .isExactlyInstanceOf(exceptionClass)
                    .hasMessageContaining("IOException")
                    .hasMessageContaining("An unexpected I/O error occurred")
                    .hasCause(cause);
        });
    }

    private static Exception newInstanceUsingCauseCtor(String displayName,
                                                       Class<? extends Exception> exceptionClass,
                                                       IOException cause) {
        Exception exception;
        try {
            var constructor = exceptionClass.getConstructor(Throwable.class);
            exception = constructor.newInstance(cause);
        } catch (Exception e) {
            throw newAssertionError(displayName, e);
        }
        return exception;
    }

    private static AssertionError newAssertionError(String displayName, Exception e) {
        var errorMessage = f("Constructor '{}' failed. Cause: {}", displayName, e.getClass().getName());
        return new AssertionError(errorMessage, e);
    }

    private static String displayName(Class<? extends Exception> exceptionClass, String parameterSpec) {
        return exceptionClass.getSimpleName() + parameterSpec;
    }
}
