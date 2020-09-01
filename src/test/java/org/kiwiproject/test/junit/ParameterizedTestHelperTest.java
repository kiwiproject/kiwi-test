package org.kiwiproject.test.junit;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.kiwiproject.test.junit.ParameterizedTestHelper.expectedResults;
import static org.kiwiproject.test.junit.ParameterizedTests.inputs;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentest4j.MultipleFailuresError;

import java.util.List;

/**
 * @implNote Suppressing Sonar "Assertions should be complete" warning b/c it doesn't
 * know that {@code assertStateChangeResult} does perform assertions.
 */
@DisplayName("ParameterizedTestHelper")
@SuppressWarnings("java:S2970")
class ParameterizedTestHelperTest {

    private ParameterizedTestHelper testHelper;
    private SoftAssertions softly;

    @BeforeEach
    void setUp() {
        softly = new SoftAssertions();
        testHelper = new ParameterizedTestHelper(softly);
    }

    @Nested
    class AssertStateChangeResult {

        @Test
        void shouldEnsureEqualSizeInputAndExpectedLists() {
            var request = new Request();
            var inputs = inputs("start", "START", "Start", "StArT", "NotStart");
            var expected = expectedResults(true, true, true);

            assertThatIllegalArgumentException().isThrownBy(() ->
                    testHelper.assertStateChangeResult(inputs, expected, request::setAction, request::isStartAction))
                    .withMessage("inputValues and expectedResults must have the same size");
        }

        @Test
        void shouldPass_WhenExpectedResults_MatchActualResults() {
            var request = new Request();
            var inputs = inputs("start", "START", "Start", "StArT", "NotStart");
            var expected = expectedResults(true, true, true, true, false);

            assertThatCode(() -> {
                testHelper.assertStateChangeResult(inputs, expected, request::setAction, request::isStartAction);
                softly.assertAll();
            }).doesNotThrowAnyException();
        }

        @Test
        void shouldThrow_WhenExpectedAndActualResultsDiffer() {
            var strictRequest = new StrictRequest();
            var inputs = inputs("start", "START", "Start", "StArT");
            var expected = expectedResults(true, true, true, true);

            var thrown = catchThrowable(() -> {
                testHelper.assertStateChangeResult(
                        inputs, expected, strictRequest::setAction, strictRequest::isStartAction);
                softly.assertAll();
            });
            assertThat(thrown).isInstanceOf(MultipleFailuresError.class);

            var errors = getFirstLineOfEachFailureMessage((MultipleFailuresError) thrown);

            assertThat(errors)
                    .describedAs("First line of error message should be our 'describedAs' message")
                    .containsExactly(
                            "[input: [START]]",
                            "[input: [Start]]",
                            "[input: [StArT]]"
                    );
        }
    }

    @Nested
    class AssertExpectedResult {

        @Test
        void shouldEnsureEqualSizeInputAndExpectedLists() {
            var inputs = inputs(1, 2, 3, 4, 5);
            var expected = expectedResults(1, 4, 9, 16);

            assertThatIllegalArgumentException().isThrownBy(() ->
                    testHelper.assertExpectedResult(inputs, expected, SimpleMath::square))
                    .withMessage("inputValues and expectedResults must have the same size");
        }

        @Test
        void shouldPass_WhenExpectedResults_MatchActualResults() {
            var inputs = inputs(1, 2, 3, 4, 5);
            var expected = expectedResults(1, 4, 9, 16, 25);

            assertThatCode(() -> {
                testHelper.assertExpectedResult(inputs, expected, SimpleMath::square);
                softly.assertAll();
            }).doesNotThrowAnyException();
        }

        @Test
        void shouldThrow_WhenExpectedAndActualResultsDiffer() {
            var inputs = inputs(1, 2, 3, 4, 5);
            var expected = expectedResults(1, 6, 9, 18, 25);  // 6 and 18 are wrong

            var thrown = catchThrowable(() -> {
                testHelper.assertExpectedResult(inputs, expected, SimpleMath::square);
                softly.assertAll();
            });
            assertThat(thrown).isInstanceOf(MultipleFailuresError.class);

            var errors = getFirstLineOfEachFailureMessage((MultipleFailuresError) thrown);

            assertThat(errors)
                    .describedAs("First line of error message should be our 'describedAs' message")
                    .containsExactly(
                            "[input: [2]]",
                            "[input: [4]]"
                    );
        }
    }

    private static List<String> getFirstLineOfEachFailureMessage(MultipleFailuresError thrown) {
        var lineSeparator = System.lineSeparator();
        return thrown.getFailures().stream()
                .map(Throwable::getMessage)
                .map(message -> message.split(lineSeparator))
                .map(lines -> lines[0])
                .map(String::trim)
                .collect(toList());
    }

    private static class Request {

        @Getter
        @Setter
        private String action;

        boolean isStartAction() {
            return "start".equalsIgnoreCase(action);
        }
    }

    private static class StrictRequest {
        @Getter
        @Setter
        private String action;

        boolean isStartAction() {
            return "start".equals(action);
        }
    }

    @UtilityClass
    private static class SimpleMath {
        int square(int x) {
            return x * x;
        }
    }
}