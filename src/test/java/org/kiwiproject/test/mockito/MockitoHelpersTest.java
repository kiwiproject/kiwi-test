package org.kiwiproject.test.mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.kiwiproject.test.mockito.MockitoHelpers.mockNeverCalled;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

@DisplayName("MockitoHelpers")
class MockitoHelpersTest {

    @Nested
    class MockNeverCalled {

        @Nested
        class WithDefaultMessage {

            @Test
            void shouldReturnNonNullMock() {
                assertThat(mockNeverCalled(List.class)).isNotNull();
            }

            @Test
            void shouldThrowAssertionError_WhenAnyMethodIsCalled() {
                var mock = mockNeverCalled(List.class);

                //noinspection ResultOfMethodCallIgnored
                assertThatExceptionOfType(AssertionError.class)
                        .isThrownBy(mock::size)
                        .withMessage("No methods should be called on this List mock, but size() was called");
            }
        }

        @Nested
        class WithCustomMessage {

            @Test
            void shouldReturnNonNullMock() {
                assertThat(mockNeverCalled(List.class, "List should not be called")).isNotNull();
            }

            @Test
            void shouldThrowAssertionError_WithCustomMessage_WhenAnyMethodIsCalled() {
                var mock = mockNeverCalled(List.class, "List should not be called");

                //noinspection ResultOfMethodCallIgnored
                assertThatExceptionOfType(AssertionError.class)
                        .isThrownBy(mock::size)
                        .withMessage("List should not be called");
            }
        }
    }
}
