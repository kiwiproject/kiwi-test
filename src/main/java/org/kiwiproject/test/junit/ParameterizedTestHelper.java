package org.kiwiproject.test.junit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static org.kiwiproject.base.KiwiPreconditions.checkArgumentNotNull;
import static org.kiwiproject.stream.IntStreams.indicesOf;

import org.assertj.core.api.SoftAssertions;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Test helper for running parameterized tests. Uses AssertJ's {@link SoftAssertions} so that all assertion failures
 * can be gathered across a range of inputs (as opposed to failing on the first assertion failure).
 * <p>
 * <strong>NOTE:</strong> When writing new tests, consider instead using JUnit Jupiter's {@code @ParameterizedTest}.
 * This class was created from long ago, before JUnit Jupiter existed, and we still have code that uses it. Depending
 * on the specific context, the methods here might end up creating very readable tests.
 *
 * @see ParameterizedTests
 * @see <a href="https://junit.org/junit5/docs/current/api/org.junit.jupiter.params/org/junit/jupiter/params/ParameterizedTest.html">JUnit 5 @ParameterizedTest</a>
 * @see <a href="https://junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests">JUnit 5 Writing Parameterized Tests</a>
 */
public class ParameterizedTestHelper {

    private final SoftAssertions softly;

    /**
     * Create a new helper instance.
     *
     * @param softly AssertJ soft assertions instance
     */
    public ParameterizedTestHelper(SoftAssertions softly) {
        this.softly = softly;
    }

    /**
     * Helper method that creates a list of expected results; this is mainly for readability in tests.
     *
     * @param results the values to use as expected results in a parameterized test
     * @param <R>     the result object type
     * @return a mutable list containing the given values
     * @see ParameterizedTests#inputs(Object[])
     */
    @SafeVarargs
    public static <R> List<R> expectedResults(R... results) {
        return newArrayList(results);
    }

    /**
     * Given a list of input values, supply each one to the {@code mutator}, and (softly) assert that the result of
     * invoking the {@code resultSupplier} matches the expected value in {@code expectedResults}. The
     * {@code inputValues} and {@code expectedResults} are expected to be the same length and that they match at
     * each index, i.e., that {@code expectedResults[N]} is the expected result when applying {@code inputValues[N]}
     * as the input.
     * <p>
     * Example: Suppose you have a {@code Person} class that has a {@code setActiveFromString(String)} method and a
     * {@code isActive()} method; the former is the input mutator function that accepts a String, and the latter is
     * the result function that returns a boolean. You can then write a test like:
     * <pre>
     * {@literal @Test}
     *  void shouldAcceptActiveAsString(SoftAssertions softly) {
     *      var p = new Person();
     *      List&lt;String&gt; inputs = inputs("true", "yes", "YES", "false", "no", "NO", "foo", "bar");
     *      List&lt;Boolean&gt; expected = expectedResults(true, true, true, false, false, false, false, false);
     *      var testHelper = new ParameterizedTestHelper(softly);
     *      testHelper.assertStateChangeResult(inputs, expected, p::setActiveFromString, p::isActive);
     *  }
     * </pre>
     * Note the examples use the {@code input} and {@code expectedResults} static factory methods for readability, but
     * you can use anything to create the lists.
     *
     * @param inputValues     the inputs
     * @param expectedResults the expected results
     * @param mutator         the mutator function, e.g., a setter method
     * @param resultSupplier  result function
     * @param <T>             the input type
     * @param <R>             the result type
     * @see ParameterizedTests#inputs(Object[])
     * @see ParameterizedTestHelper#expectedResults(Object[])
     */
    public <T, R> void assertStateChangeResult(List<T> inputValues,
                                               List<R> expectedResults,
                                               Consumer<T> mutator,
                                               Supplier<R> resultSupplier) {

        checkInputsAndExpectedResults(inputValues, expectedResults);
        checkArgumentNotNull(mutator);
        checkArgumentNotNull(resultSupplier);

        indicesOf(inputValues).forEach(index -> {
            var input = inputValues.get(index);
            mutator.accept(input);
            var result = resultSupplier.get();
            softly.assertThat(result)
                    .describedAs("input: [%s]", input)
                    .isEqualTo(expectedResults.get(index));
        });
    }

    /**
     * Given a list of input values, supply each one to the {@code function}, and (softly) assert that the result of
     * invoking the function matches the expected value in {@code expectedResults}. The {@code inputValues} and
     * {@code expectedResults} are expected to be the same length and that they match at each index, i.e., that
     * {@code expectedResults[N]} is the expected result when applying {@code inputValues[N]} as the input.
     * <p>
     * Example: Assuming there is a {@code SimpleMath} utility class with a {@code square} function that accepts an
     * integer, a test might look like:
     * <pre>
     * {@literal @Test}
     *  void shouldCalculateSquares(SoftAssertions softly) {
     *      var inputs = inputs(1, 2, 3, 4, 5);
     *      var expected = expectedResults(1, 4, 9, 16, 25);
     *      new ParameterizedTestHelper(softly).assertExpectedResult(inputs, expected, SimpleMath::square);
     *  }
     * </pre>
     * Note the examples use the {@code input} and {@code expectedResults} static factory methods for readability, but
     * you can use anything to create the lists.
     *
     * @param inputValues     the inputs
     * @param expectedResults the expected results
     * @param function        function that accepts a T and produces an R
     * @param <T>             the input type
     * @param <R>             the result type
     * @see ParameterizedTests#inputs(Object[])
     * @see ParameterizedTestHelper#expectedResults(Object[])
     */
    public <T, R> void assertExpectedResult(List<T> inputValues,
                                            List<R> expectedResults,
                                            Function<T, R> function) {

        checkInputsAndExpectedResults(inputValues, expectedResults);
        checkArgumentNotNull(function);

        indicesOf(inputValues).forEach(index -> {
            var input = inputValues.get(index);
            var result = function.apply(input);
            softly.assertThat(result)
                    .describedAs("input: [%s]", input)
                    .isEqualTo(expectedResults.get(index));
        });
    }

    private static <T, R> void checkInputsAndExpectedResults(List<T> inputValues, List<R> expectedResults) {
        checkArgumentNotNull(inputValues);
        checkArgumentNotNull(expectedResults);
        checkArgument(inputValues.size() == expectedResults.size(),
                "inputValues and expectedResults must have the same size");
    }
}
