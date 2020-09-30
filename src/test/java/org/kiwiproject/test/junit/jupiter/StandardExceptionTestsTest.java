package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.kiwiproject.test.junit.jupiter.StandardExceptionTests.causeConstructorDynamicTest;
import static org.kiwiproject.test.junit.jupiter.StandardExceptionTests.messageAndCauseConstructorDynamicTest;
import static org.kiwiproject.test.junit.jupiter.StandardExceptionTests.messageConstructorDynamicTest;
import static org.kiwiproject.test.junit.jupiter.StandardExceptionTests.noArgConstructorDynamicTest;
import static org.kiwiproject.test.junit.jupiter.StandardExceptionTests.standardConstructorTestsFor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collection;

@DisplayName("StandardExceptionTests")
class StandardExceptionTestsTest {

    /**
     * This is actual usage as opposed to testing the {@link DynamicTest} instances directly, and we can only
     * use an exception class that is "standard" otherwise the entire test fails.
     */
    @TestFactory
    Collection<DynamicTest> shouldHaveStandardConstructors() {
        return standardConstructorTestsFor(StandardException.class);
    }

    /**
     * Actual usage. See explanation in {@link #shouldHaveStandardConstructors()}
     */
    @TestFactory
    DynamicTest shouldHaveNoArgConstructor() {
        return noArgConstructorDynamicTest(StandardException.class);
    }

    /**
     * Actual usage. See explanation in {@link #shouldHaveStandardConstructors()}
     */
    @TestFactory
    DynamicTest shouldHaveMessageConstructor() {
        return messageConstructorDynamicTest(StandardException.class);
    }

    /**
     * Actual usage. See explanation in {@link #shouldHaveStandardConstructors()}
     */
    @TestFactory
    DynamicTest shouldHaveMessageAndCauseConstructor() {
        return messageAndCauseConstructorDynamicTest(StandardException.class);
    }

    /**
     * Actual usage. See explanation in {@link #shouldHaveStandardConstructors()}
     */
    @TestFactory
    DynamicTest shouldHaveCauseConstructor() {
        return causeConstructorDynamicTest(StandardException.class);
    }

    @Nested
    class StandardConstructorTestsFor {

        @ParameterizedTest
        @ValueSource(strings = {
                "StandardException()",
                "StandardException(String message)",
                "StandardException(String message, Throwable cause)",
                "StandardException(Throwable cause)"
        })
        void shouldGenerateDynamicTests(String displayName) {
            var dynamicTests = standardConstructorTestsFor(StandardException.class);
            var test = findDynamicTest(dynamicTests, displayName);

            assertThatCode(() -> execute(test)).doesNotThrowAnyException();
        }

        private DynamicTest findDynamicTest(Collection<DynamicTest> dynamicTests, String displayName) {
            return dynamicTests.stream()
                    .filter(dynamicTest -> dynamicTest.getDisplayName().equals(displayName))
                    .findFirst()
                    .orElseThrow();
        }
    }

    @Nested
    class NoArgConstructorDynamicTest {

        @Test
        void shouldPassWhenConstructorExists() {
            var test = StandardExceptionTests.noArgConstructorDynamicTest(NonStandardException1.class);

            assertThatCode(() -> execute(test)).doesNotThrowAnyException();
        }

        @Test
        void shouldFailWhenConstructorDoesNotExist() {
            var test = StandardExceptionTests.noArgConstructorDynamicTest(NonStandardException2.class);
            var executable = test.getExecutable();

            assertThatThrownBy(executable::execute)
                    .isExactlyInstanceOf(AssertionError.class)
                    .hasMessage("Constructor 'NonStandardException2()' failed. Cause: java.lang.NoSuchMethodException")
                    .hasCauseExactlyInstanceOf(NoSuchMethodException.class);
        }
    }

    @Nested
    class MessageConstructorDynamicTest {

        @Test
        void shouldPassWhenConstructorExists() {
            var test = messageConstructorDynamicTest(NonStandardException2.class);

            assertThatCode(() -> execute(test)).doesNotThrowAnyException();
        }

        @Test
        void shouldFailWhenConstructorDoesNotExist() {
            var test = messageConstructorDynamicTest(NonStandardException1.class);
            var executable = test.getExecutable();

            assertThatThrownBy(executable::execute)
                    .isExactlyInstanceOf(AssertionError.class)
                    .hasMessage("Constructor 'NonStandardException1(String message)' failed. Cause: java.lang.NoSuchMethodException")
                    .hasCauseExactlyInstanceOf(NoSuchMethodException.class);
        }
    }

    @Nested
    class MessageAndCauseConstructorDynamicTest {

        @Test
        void shouldPassWhenConstructorExists() {
            var test = StandardExceptionTests.messageAndCauseConstructorDynamicTest(NonStandardException3.class);

            assertThatCode(() -> execute(test)).doesNotThrowAnyException();
        }

        @Test
        void shouldFailWhenConstructorDoesNotExist() {
            var test = StandardExceptionTests.messageAndCauseConstructorDynamicTest(NonStandardException2.class);
            var executable = test.getExecutable();

            assertThatThrownBy(executable::execute)
                    .isExactlyInstanceOf(AssertionError.class)
                    .hasMessage("Constructor 'NonStandardException2(String message, Throwable cause)' failed. Cause: java.lang.NoSuchMethodException")
                    .hasCauseExactlyInstanceOf(NoSuchMethodException.class);
        }
    }

    @Nested
    class CauseConstructorDynamicTest {

        @Test
        void shouldPassWhenConstructorExists() {
            var test = StandardExceptionTests.causeConstructorDynamicTest(NonStandardException4.class);

            assertThatCode(() -> execute(test)).doesNotThrowAnyException();
        }

        @Test
        void shouldFailWhenConstructorDoesNotExist() {
            var test = StandardExceptionTests.causeConstructorDynamicTest(NonStandardException2.class);
            var executable = test.getExecutable();

            assertThatThrownBy(executable::execute)
                    .isExactlyInstanceOf(AssertionError.class)
                    .hasMessage("Constructor 'NonStandardException2(Throwable cause)' failed. Cause: java.lang.NoSuchMethodException")
                    .hasCauseExactlyInstanceOf(NoSuchMethodException.class);
        }
    }

    private void execute(DynamicTest dynamicTest) {
        try {
            dynamicTest.getExecutable().execute();
        } catch (Throwable throwable) {
            throw new RuntimeException(dynamicTest.getDisplayName(), throwable);
        }
    }

    public static class StandardException extends RuntimeException {
        public StandardException() {
        }

        public StandardException(String message) {
            super(message);
        }

        public StandardException(String message, Throwable cause) {
            super(message, cause);
        }

        public StandardException(Throwable cause) {
            super(cause);
        }
    }

    public static class NonStandardException1 extends Exception {
    }

    public static class NonStandardException2 extends Exception {
        public NonStandardException2(String message) {
            super(message);
        }
    }

    public static final class NonStandardException3 extends Exception {
        public NonStandardException3(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final class NonStandardException4 extends Exception {
        public NonStandardException4(Throwable cause) {
            super(cause);
        }
    }
}
