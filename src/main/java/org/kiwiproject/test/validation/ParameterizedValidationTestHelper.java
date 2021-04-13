package org.kiwiproject.test.validation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static org.kiwiproject.base.KiwiPreconditions.checkArgumentNotNull;
import static org.kiwiproject.test.junit.ParameterizedTests.inputs;
import static org.kiwiproject.test.util.IntStreams.indicesOf;

import org.assertj.core.api.SoftAssertions;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Test helper for running parameterized validation tests. Uses AssertJ's {@link SoftAssertions} to allow tests
 * to gather all assertion failures across a range of inputs, rather than failing on the first assertion failure.
 * <p>
 * <strong>NOTE:</strong> When writing new tests, consider instead using JUnit Jupiter's {@code @ParameterizedTest}.
 * This class was created years before JUnit Jupiter existed, and we still have code that uses it. Depending
 * on the specific context, the methods here might end up creating very readable tests.
 */
public class ParameterizedValidationTestHelper {

    private static final String A_STRING_VALUE = "a value";

    private final Validator validator;
    private final SoftAssertions softly;

    /**
     * Create a new helper with a default {@link Validator} instance.
     *
     * @param softly the {@link SoftAssertions} instance to use
     */
    public ParameterizedValidationTestHelper(SoftAssertions softly) {
        this(softly, ValidationTestHelper.newValidator());
    }

    /**
     * Create a new helper.
     *
     * @param softly    the {@link SoftAssertions} instance to use
     * @param validator the {@link Validator} to use when validating objects
     */
    public ParameterizedValidationTestHelper(SoftAssertions softly, Validator validator) {
        this.validator = validator;
        this.softly = softly;
    }

    /**
     * Creates a list of the expected number of constraint violations. This is mainly for readability in tests.
     *
     * @param values the values to use as the number of expected constraint violations in a parameterized test
     * @return a mutable list containing the given values
     */
    public static List<Integer> expectedViolations(Integer... values) {
        return newArrayList(values);
    }

    /**
     * Creates a list of String lists for expected constraint violation messages. This is mainly for readability in
     * tests.
     *
     * @param values the values to use as the expected constraint violation messages in a parameterized test
     * @return a mutable list containing the given values
     */
    @SafeVarargs
    public static List<List<String>> expectedMessagesLists(List<String>... values) {
        return newArrayList(values);
    }

    /**
     * Creates a list of expected constraint violation messages. This is mainly for readability in tests.
     *
     * @param values the values to use as the expected constraint violation messages in a parameterized test
     * @return a mutable list containing the given values
     */
    public static List<String> expectedMessages(String... values) {
        return newArrayList(values);
    }

    /**
     * Creates a an empty list that can be used to indicate there are no expected constraint violation messages.
     * This is mainly for readability in tests.
     *
     * @return an immutable empty list
     */
    public static List<String> noExpectedMessages() {
        return Collections.emptyList();
    }

    /**
     * Creates an array of classes that represent validation groups. This is mainly for readability in tests.
     *
     * @param groups the validation group classes
     * @return an array of the group {@link Class} objects
     * @implNote Aside from the readability aspect, this is a nifty little "trick" to "convert" a vararg into an
     * array of objects of the same type
     */
    public static Class<?>[] validationGroups(Class<?>... groups) {
        return groups;
    }

    /**
     * Given a list of input values, supply each one to {@code mutator}, and softly assert that the number of
     * constraint violations matches the numbers in {@code expectedViolations}. The {@code inputValues} and
     * {@code expectedViolations} are expected to be the same length and to match at each index, i.e.
     * that {@code expectedViolations[N]} is the expected number of errors when applying {@code inputValues[N]}
     * as the input.
     * <p>
     * Example: Suppose you have a {@code Person} class that has a {@code lastName} property with
     * {@code @NotBlank} and {@code @Length(min = 2, max = 100)} validation annotations. You can then write a
     * test like this:
     * <pre>
     * {@literal @Test}
     *  void shouldValidatePersonLastName(SoftAssertions softly) {
     *      var p = new Person();
     *      List&lt;String&gt; inputs = inputs("Smith", "Ng", "X", "", " ", null);
     *      List&lt;Integer&gt; expected = expectedViolations(0, 0, 1, 2, 2, 1);
     *      var helper = new ParameterizedValidationTestHelper(softly);
     *      helper.assertPropertyViolationCounts("lastName", inputs, expected, p, p::setLastName);
     *  }
     * </pre>
     *
     * @param propertyName       the property to validate
     * @param inputValues        the inputs
     * @param expectedViolations the expected number of violations corresponding to the inputs
     * @param object             the object to validate
     * @param mutator            the mutator function, e.g. a setter method
     * @param groups             the group or list of groups targeted for validation (defaults to Default)
     * @param <T>                the input type
     * @param <U>                the object type
     */
    public <T, U> void assertPropertyViolationCounts(String propertyName,
                                                     List<T> inputValues,
                                                     List<Integer> expectedViolations,
                                                     U object,
                                                     Consumer<T> mutator,
                                                     Class<?>... groups) {

        checkArgumentNotNull(propertyName);
        checkInputAndExpectedValues(inputValues, expectedViolations, "expectedViolations");

        // TODO: When kiwi 0.23.0 is released, switch this to the kiwi version
        indicesOf(inputValues).forEach(index -> {
            T input = inputValues.get(index);
            mutator.accept(input);
            var violations = validator.validateProperty(object, propertyName, groups);
            softly.assertThat(violations)
                    .describedAs("input: [%s]", input)
                    .hasSize(expectedViolations.get(index));
        });
    }

    /**
     * Given a list of input values, supply each one to {@code mutator}, and softly assert that the constraint
     * violation messages match those in {@code expectedViolationMessages}. The {@code inputValues} and
     * {@code expectedViolations} are expected to be the same length and to match at each index, i.e.
     * that {@code expectedViolations[N]} contains the expected violation messages when applying
     * {@code inputValues[N]} as the input.
     * <p>
     * Example: Suppose you have a {@code Person} class that has a {@code lastName} property with
     * {@code @NotBlank} and {@code @Length(min = 2, max = 100)} validation annotations. You can then write a
     * test like this:
     * <pre>
     * {@literal @Test}
     *  void shouldValidatePersonLastName(SoftAssertions softly) {
     *      var p = new Person();
     *      List&lt;String&gt; inputs = inputs("Smith", "Ng", "X", "", " ", null);
     *      List&lt;List&lt;String&gt;&gt; expected = expectedMessagesLists(
     *          noExpectedMessages(),
     *          noExpectedMessages(),
     *          expectedMessages("length must be between 2 and 100"),
     *          expectedMessages("must not be blank", "length must be between 2 and 100"),
     *          expectedMessages("must not be blank", "length must be between 2 and 100"),
     *          expectedMessages("must not be blank")
     *      );
     *      var helper = new ParameterizedValidationTestHelper(softly);
     *      helper.assertPropertyViolationCounts("lastName", inputs, expected, p, p::setLastName);
     *  }
     * </pre>
     *
     * @param propertyName              the property to validate
     * @param inputValues               the inputs
     * @param expectedViolationMessages the expected violation error messages corresponding to the inputs
     * @param object                    the object to validate
     * @param mutator                   the mutator function, e.g. a setter method
     * @param groups                    the group or list of groups targeted for validation (defaults to Default)
     * @param <T>                       the input type
     * @param <U>                       the object type
     */
    public <T, U> void assertPropertyViolationMessages(String propertyName,
                                                       List<T> inputValues,
                                                       List<List<String>> expectedViolationMessages,
                                                       U object,
                                                       Consumer<T> mutator,
                                                       Class<?>... groups) {

        checkArgumentNotNull(propertyName);
        checkInputAndExpectedValues(inputValues, expectedViolationMessages, "expectedViolationMessages");

        // TODO: When kiwi 0.23.0 is released, switch this to the kiwi version
        indicesOf(inputValues).forEach(index -> {
            T input = inputValues.get(index);
            mutator.accept(input);
            var violations = validator.validateProperty(object, propertyName, groups);
            softly.assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .describedAs("input: [%s]", input)
                    .hasSameElementsAs(expectedViolationMessages.get(index));
        });
    }

    private static <T, R> void checkInputAndExpectedValues(List<T> inputValues,
                                                           List<R> expectedValues,
                                                           String expectedDescription) {
        checkArgumentNotNull(inputValues);
        checkArgumentNotNull(expectedValues);
        checkArgument(inputValues.size() == expectedValues.size(),
                "inputValues and %s must have the same size", expectedDescription);
    }

    /**
     * Convenience wrapper around {@link #assertPropertyViolationCounts(String, List, List, Object, Consumer, Class[])}
     * to check that a String property <em>cannot be blank</em>.
     * <p>
     * <em>Note this assumes the property doesn't have other validations, otherwise it would be unable to know the expected
     * violation count.</em>
     *
     * @param propertyName the property to validate
     * @param object       the object to validate
     * @param mutator      the mutator function, e.g. a setter method
     * @param groups       the group or list of groups targeted for validation (defaults to Default)
     * @param <T>          the object type
     */
    public <T> void assertStringPropertyCannotBeBlank(String propertyName,
                                                      T object,
                                                      Consumer<String> mutator,
                                                      Class<?>... groups) {
        var inputs = inputs(A_STRING_VALUE, "", " ", null);
        var expectedViolations = expectedViolations(0, 1, 1, 1);
        assertPropertyViolationCounts(propertyName,
                inputs,
                expectedViolations,
                object, mutator, groups);
    }

    /**
     * Convenience wrapper around {@link #assertPropertyViolationCounts(String, List, List, Object, Consumer, Class[])}
     * to check that a String property <em>must be null</em>.
     * <p>
     * <em>Note this assumes the property doesn't have other validations, otherwise it would be unable to know the expected
     * violation count.</em>
     *
     * @param propertyName the property to validate
     * @param object       the object to validate
     * @param mutator      the mutator function, e.g. a setter method
     * @param groups       the group or list of groups targeted for validation (defaults to Default)
     * @param <T>          the object type
     */
    public <T> void assertStringPropertyMustBeNull(String propertyName,
                                                   T object,
                                                   Consumer<String> mutator,
                                                   Class<?>... groups) {
        var inputs = inputs(A_STRING_VALUE, "", " ", null);
        var expectedViolations = expectedViolations(1, 1, 1, 0);
        assertPropertyViolationCounts(propertyName, inputs, expectedViolations, object, mutator, groups);
    }

    /**
     * Convenience wrapper around {@link #assertPropertyViolationCounts(String, List, List, Object, Consumer, Class[])}
     * to check that a String property <em>has no violations</em>.
     * <p>
     * <em>Note this assumes the property doesn't have other validations, otherwise it would be unable to know the expected
     * violation count.</em>
     *
     * @param propertyName the property to validate
     * @param object       the object to validate
     * @param mutator      the mutator function, e.g. a setter method
     * @param groups       the group or list of groups targeted for validation (defaults to Default)
     * @param <T>          the object type
     */
    public <T> void assertStringPropertyHasNoViolations(String propertyName,
                                                        T object,
                                                        Consumer<String> mutator,
                                                        Class<?>... groups) {
        var inputs = inputs(A_STRING_VALUE, "", " ", null);
        var expectedViolations = expectedViolations(0, 0, 0, 0);
        assertPropertyViolationCounts(propertyName, inputs, expectedViolations, object, mutator, groups);
    }

    /**
     * Convenience wrapper around {@link #assertPropertyViolationCounts(String, List, List, Object, Consumer, Class[])}
     * to check that an Instant property <em>cannot be null</em>.
     * <p>
     * <em>Note this assumes the property doesn't have other validations, otherwise it would be unable to know the expected
     * violation count.</em>
     *
     * @param propertyName the property to validate
     * @param object       the object to validate
     * @param mutator      the mutator function, e.g. a setter method
     * @param groups       the group or list of groups targeted for validation (defaults to Default)
     * @param <T>          the object type
     */
    public <T> void assertInstantPropertyCannotBeNull(String propertyName,
                                                      T object,
                                                      Consumer<Instant> mutator,
                                                      Class<?>... groups) {
        var inputs = inputs(Instant.now(), null);
        var expectedViolations = expectedViolations(0, 1);
        assertPropertyViolationCounts(propertyName, inputs, expectedViolations, object, mutator, groups);
    }

    /**
     * Convenience wrapper around {@link #assertPropertyViolationCounts(String, List, List, Object, Consumer, Class[])}
     * to check that an Instant property <em>must be null</em>.
     * <p>
     * <em>Note this assumes the property doesn't have other validations, otherwise it would be unable to know the expected
     * violation count.</em>
     *
     * @param propertyName the property to validate
     * @param object       the object to validate
     * @param mutator      the mutator function, e.g. a setter method
     * @param groups       the group or list of groups targeted for validation (defaults to Default)
     * @param <T>          the object type
     */
    public <T> void assertInstantPropertyMustBeNull(String propertyName,
                                                    T object,
                                                    Consumer<Instant> mutator,
                                                    Class<?>... groups) {
        var inputs = inputs(Instant.now(), null);
        var expectedViolations = expectedViolations(1, 0);
        assertPropertyViolationCounts(propertyName, inputs, expectedViolations, object, mutator, groups);
    }

    /**
     * Convenience wrapper around {@link #assertPropertyViolationCounts(String, List, List, Object, Consumer, Class[])}
     * to check that a ZonedDateTime property <em>cannot be null</em>.
     * <p>
     * <em>Note this assumes the property doesn't have other validations, otherwise it would be unable to know the expected
     * violation count.</em>
     *
     * @param propertyName the property to validate
     * @param object       the object to validate
     * @param mutator      the mutator function, e.g. a setter method
     * @param groups       the group or list of groups targeted for validation (defaults to Default)
     * @param <T>          the object type
     */
    public <T> void assertZonedDateTimePropertyCannotBeNull(String propertyName,
                                                            T object,
                                                            Consumer<ZonedDateTime> mutator,
                                                            Class<?>... groups) {
        var inputs = inputs(ZonedDateTime.now(), null);
        var expectedViolations = expectedViolations(0, 1);
        assertPropertyViolationCounts(propertyName, inputs, expectedViolations, object, mutator, groups);
    }

    /**
     * Convenience wrapper around {@link #assertPropertyViolationCounts(String, List, List, Object, Consumer, Class[])}
     * to check that a ZonedDateTime property <em>must be null</em>.
     * <p>
     * <em>Note this assumes the property doesn't have other validations, otherwise it would be unable to know the expected
     * violation count.</em>
     *
     * @param propertyName the property to validate
     * @param object       the object to validate
     * @param mutator      the mutator function, e.g. a setter method
     * @param groups       the group or list of groups targeted for validation (defaults to Default)
     * @param <T>          the object type
     */
    public <T> void assertZonedDateTimePropertyMustBeNull(String propertyName,
                                                          T object,
                                                          Consumer<ZonedDateTime> mutator,
                                                          Class<?>... groups) {
        var inputs = inputs(ZonedDateTime.now(), null);
        var expectedViolations = expectedViolations(1, 0);
        assertPropertyViolationCounts(propertyName, inputs, expectedViolations, object, mutator, groups);
    }

    /**
     * Convenience wrapper around {@link #assertPropertyViolationCounts(String, List, List, Object, Consumer, Class[])}
     * to check that a Long property <em>cannot be null</em>.
     * <p>
     * <em>Note this assumes the property doesn't have other validations, otherwise it would be unable to know the expected
     * violation count.</em>
     *
     * @param propertyName the property to validate
     * @param object       the object to validate
     * @param mutator      the mutator function, e.g. a setter method
     * @param groups       the group or list of groups targeted for validation (defaults to Default)
     * @param <T>          the object type
     */
    public <T> void assertLongPropertyCannotBeNull(String propertyName,
                                                   T object,
                                                   Consumer<Long> mutator,
                                                   Class<?>... groups) {
        var inputs = inputs(42L, null);
        var expectedViolations = expectedViolations(0, 1);
        assertPropertyViolationCounts(propertyName, inputs, expectedViolations, object, mutator, groups);
    }

    /**
     * Convenience wrapper around {@link #assertPropertyViolationCounts(String, List, List, Object, Consumer, Class[])}
     * to check that a Long property <em>must be null</em>.
     * <p>
     * <em>Note this assumes the property doesn't have other validations, otherwise it would be unable to know the expected
     * violation count.</em>
     *
     * @param propertyName the property to validate
     * @param object       the object to validate
     * @param mutator      the mutator function, e.g. a setter method
     * @param groups       the group or list of groups targeted for validation (defaults to Default)
     * @param <T>          the object type
     */
    public <T> void assertLongPropertyMustBeNull(String propertyName,
                                                 T object,
                                                 Consumer<Long> mutator,
                                                 Class<?>... groups) {
        var inputs = inputs(42L, null);
        var expectedViolations = expectedViolations(1, 0);
        assertPropertyViolationCounts(propertyName, inputs, expectedViolations, object, mutator, groups);
    }

    /**
     * Convenience wrapper around {@link #assertPropertyViolationCounts(String, List, List, Object, Consumer, Class[])}
     * to check that an Integer property <em>cannot be null</em>.
     * <p>
     * <em>Note this assumes the property doesn't have other validations, otherwise it would be unable to know the expected
     * violation count.</em>
     *
     * @param propertyName the property to validate
     * @param object       the object to validate
     * @param mutator      the mutator function, e.g. a setter method
     * @param groups       the group or list of groups targeted for validation (defaults to Default)
     * @param <T>          the object type
     */
    public <T> void assertIntegerPropertyCannotBeNull(String propertyName,
                                                      T object,
                                                      Consumer<Integer> mutator,
                                                      Class<?>... groups) {
        var inputs = inputs(84, null);
        var expectedViolations = expectedViolations(0, 1);
        assertPropertyViolationCounts(propertyName, inputs, expectedViolations, object, mutator, groups);
    }

    /**
     * Convenience wrapper around {@link #assertPropertyViolationCounts(String, List, List, Object, Consumer, Class[])}
     * to check that an Integer property <em>must be null</em>.
     * <p>
     * <em>Note this assumes the property doesn't have other validations, otherwise it would be unable to know the expected
     * violation count.</em>
     *
     * @param propertyName the property to validate
     * @param object       the object to validate
     * @param mutator      the mutator function, e.g. a setter method
     * @param groups       the group or list of groups targeted for validation (defaults to Default)
     * @param <T>          the object type
     */
    public <T> void assertIntegerPropertyMustBeNull(String propertyName,
                                                    T object,
                                                    Consumer<Integer> mutator,
                                                    Class<?>... groups) {
        var inputs = inputs(84, null);
        var expectedViolations = expectedViolations(1, 0);
        assertPropertyViolationCounts(propertyName, inputs, expectedViolations, object, mutator, groups);
    }

    /**
     * Convenience wrapper around {@link #assertPropertyViolationCounts(String, List, List, Object, Consumer, Class[])}
     * to check that an {@code enum} property <em>cannot be null</em>. Tests all the values in the specified
     * {@code enumClass} as well as a {@code null} value.
     * <p>
     * <em>Note this assumes the property doesn't have other validations, otherwise it would be unable to know the expected
     * violation count.</em>
     *
     * @param propertyName the property to validate
     * @param object       the object to validate
     * @param enumClass    the type of the input {@link Enum}
     * @param mutator      the mutator function, e.g. a setter method
     * @param groups       the group or list of groups targeted for validation (defaults to Default)
     * @param <T>          the object type
     * @param <E>          the enum type
     */
    public <T, E extends Enum<?>> void assertEnumPropertyCannotBeNull(String propertyName,
                                                                      T object,
                                                                      Class<E> enumClass,
                                                                      Consumer<E> mutator,
                                                                      Class<?>... groups) {

        E[] enumConstants = enumClass.getEnumConstants();
        List<E> inputs = inputs(enumConstants);
        inputs.add(null);

        List<Integer> expectedViolations = new ArrayList<>(Collections.nCopies(enumConstants.length, 0));
        expectedViolations.add(1);

        assertPropertyViolationCounts(propertyName, inputs, expectedViolations, object, mutator, groups);
    }
}
