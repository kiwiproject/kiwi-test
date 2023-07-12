package org.kiwiproject.test.validation;

import lombok.experimental.UtilityClass;
import org.assertj.core.api.SoftAssertions;
import org.kiwiproject.base.KiwiStrings;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.kiwiproject.collect.KiwiLists.isNotNullOrEmpty;

/**
 * Provides helper methods for making assertions on validation of objects using the Bean Validation API.
 * <p>
 * NOTE: Most validation assertions methods have a vararg parameter to allow specification of the group(s)
 * targeted for validation. The only ones that currently do NOT have group support are the methods that
 * already have a vararg to check the actual validation messages. At the present time, we have decided not
 * to add group support as these methods have generally been used much less often.
 */
@UtilityClass
public class ValidationTestHelper {

    private static final Validator VALIDATOR = newValidator();

    /**
     * Returns a singleton {@link Validator} instance.
     *
     * @return a singleton {@link Validator} instance
     */
    public static Validator getValidator() {
        return VALIDATOR;
    }

    /**
     * Creates a new, default {@link Validator} instance using the default validator factory provided by the underlying
     * bean validation implementation, for example the reference implementation Hibernate Validator.
     *
     * @return a new {@link Validator} instance
     */
    public static Validator newValidator() {
        // noinspection resource
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    /**
     * Asserts that there are <strong>no</strong> constraint violations on the given object using a
     * default validator.
     *
     * @param object the object to validate
     * @param groups the optional validation groups to apply
     */
    public static void assertNoViolations(Object object, Class<?>... groups) {
        assertNoViolations(VALIDATOR, object, groups);
    }

    /**
     * Asserts that there are <strong>no</strong> constraint violations on the given object using the
     * specified validator.
     *
     * @param validator the Validator instance to perform validation with
     * @param object    the object to validate
     * @param groups    the optional validation groups to apply
     */
    public static void assertNoViolations(Validator validator, Object object, Class<?>... groups) {
        var violations = validator.validate(object, groups);
        assertThat(violations).isEmpty();
    }

    /**
     * Performs an AssertJ soft assertion that there are <strong>no</strong> constraint violations on the given
     * object using a default validator.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may not be empty.
     *
     * @param softly an AssertJ SoftAssertions instance for collecting multiple errors
     * @param object the object to validate
     * @param groups the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     */
    public static <T> Set<ConstraintViolation<T>> assertNoViolations(SoftAssertions softly, T object, Class<?>... groups) {
        return assertNoViolations(softly, VALIDATOR, object, groups);
    }

    /**
     * Performs an AssertJ soft assertion that there are <strong>no</strong> constraint violations on the given
     * object using the specified validator.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may not be empty.
     *
     * @param softly    an AssertJ SoftAssertions instance for collecting multiple errors
     * @param validator the Validator instance to perform validation with
     * @param object    the object to validate
     * @param groups    the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     */
    public static <T> Set<ConstraintViolation<T>> assertNoViolations(SoftAssertions softly,
                                                                     Validator validator,
                                                                     T object,
                                                                     Class<?>... groups) {
        var violations = validator.validate(object, groups);
        softly.assertThat(violations).isEmpty();
        return violations;
    }

    /**
     * Asserts that there is at least one constraint violation on the given object using a default validator.
     *
     * @param object the object to validate
     * @param groups the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects, when the assertion passes
     */
    public static <T> Set<ConstraintViolation<T>> assertHasViolations(T object, Class<?>... groups) {
        return assertHasViolations(VALIDATOR, object, groups);
    }

    /**
     * Asserts that there is at least one constraint violation on the given object using the given validator.
     *
     * @param validator the Validator instance to perform validation with
     * @param object    the object to validate
     * @param groups    the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects, when the assertion passes
     */
    public static <T> Set<ConstraintViolation<T>> assertHasViolations(Validator validator, T object, Class<?>... groups) {
        var violations = validator.validate(object, groups);
        assertThat(violations)
                .describedAs("Expected at least one constraint violation")
                .isNotEmpty();
        return violations;
    }

    /**
     * Performs an AssertJ soft assertion that is at least one constraint violation
     * on the given object using a default validator.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may or may not have the expected values.
     *
     * @param softly an AssertJ SoftAssertions instance for collecting multiple errors
     * @param object the object to validate
     * @param groups the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     */
    public static <T> Set<ConstraintViolation<T>> assertHasViolations(SoftAssertions softly, T object, Class<?>... groups) {
        return assertHasViolations(softly, VALIDATOR, object, groups);
    }

    /**
     * Performs an AssertJ soft assertion that is at least one constraint violation
     * on the given object using the given validator.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may or may not have the expected values.
     *
     * @param softly    an AssertJ SoftAssertions instance for collecting multiple errors
     * @param validator the Validator instance to perform validation with
     * @param object    the object to validate
     * @param groups    the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     */
    public static <T> Set<ConstraintViolation<T>> assertHasViolations(SoftAssertions softly,
                                                                      Validator validator,
                                                                      T object,
                                                                      Class<?>... groups) {
        var violations = validator.validate(object, groups);
        softly.assertThat(violations)
                .describedAs("Expected at least one constraint violation")
                .isNotEmpty();
        return violations;
    }

    /**
     * Asserts that there are exactly {@code numExpectedViolations} constraint violations on the given object
     * using a default validator.
     *
     * @param object                the object to validate
     * @param numExpectedViolations the number of violations that are expected
     * @param groups                the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects, when the assertion passes
     */
    public static <T> Set<ConstraintViolation<T>> assertViolations(T object, int numExpectedViolations, Class<?>... groups) {
        return assertViolations(VALIDATOR, object, numExpectedViolations, groups);
    }

    /**
     * Asserts that there are exactly {@code numExpectedViolations} constraint violations on the given object
     * using the specified validator.
     *
     * @param validator             the Validator instance to perform validation with
     * @param object                the object to validate
     * @param numExpectedViolations the number of violations that are expected
     * @param groups                the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects, when the assertion passes
     */
    public static <T> Set<ConstraintViolation<T>> assertViolations(Validator validator,
                                                                   T object,
                                                                   int numExpectedViolations,
                                                                   Class<?>... groups) {
        var violations = validator.validate(object, groups);
        assertThat(violations).hasSize(numExpectedViolations);
        return violations;
    }

    /**
     * Performs an AssertJ soft assertion that there are exactly {@code numExpectedViolations} constraint violations
     * on the given object using a default validator.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may or may not have the expected values.
     *
     * @param softly                an AssertJ SoftAssertions instance for collecting multiple errors
     * @param object                the object to validate
     * @param numExpectedViolations the number of violations that are expected
     * @param groups                the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     */
    public static <T> Set<ConstraintViolation<T>> assertViolations(SoftAssertions softly,
                                                                   T object,
                                                                   int numExpectedViolations,
                                                                   Class<?>... groups) {
        return assertViolations(softly, VALIDATOR, object, numExpectedViolations, groups);
    }

    /**
     * Performs an AssertJ soft assertion that there are exactly {@code numExpectedViolations} constraint violations
     * on the given object using the specified validator.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may or may not have the expected values.
     *
     * @param softly                an AssertJ SoftAssertions instance for collecting multiple errors
     * @param validator             the Validator instance to perform validation with
     * @param object                the object to validate
     * @param numExpectedViolations the number of violations that are expected
     * @param groups                the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     */
    public static <T> Set<ConstraintViolation<T>> assertViolations(SoftAssertions softly,
                                                                   Validator validator,
                                                                   T object,
                                                                   int numExpectedViolations,
                                                                   Class<?>... groups) {
        var violations = validator.validate(object, groups);
        softly.assertThat(violations).hasSize(numExpectedViolations);
        return violations;
    }

    /**
     * Asserts that there is exactly one constraint violation on the given object for the given {@code propertyName}
     * using a default validator.
     *
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @param groups       the optional validation groups to apply
     * @return the single {@link ConstraintViolation} object, when the assertion passes
     */
    public static <T> ConstraintViolation<T> assertOnePropertyViolation(T object, String propertyName, Class<?>... groups) {
        return assertOnePropertyViolation(VALIDATOR, object, propertyName, groups);
    }

    /**
     * Asserts that there is exactly one constraint violation on the given object for the given {@code propertyName}
     * using the specified validator.
     *
     * @param validator    the Validator instance to perform validation with
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @param groups       the optional validation groups to apply
     * @return the single {@link ConstraintViolation} object, when the assertion passes
     */
    public static <T> ConstraintViolation<T> assertOnePropertyViolation(Validator validator,
                                                                        T object,
                                                                        String propertyName,
                                                                        Class<?>... groups) {
        var constraintViolations = assertPropertyViolations(validator, object, propertyName, 1, groups);
        return constraintViolations.iterator().next();
    }

    /**
     * Performs an AssertJ soft assertion that there is exactly one constraint violation on the given object for the
     * given {@code propertyName} using a default validator.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may or may not have the expected values. This is also the reason it returns a
     * set instead of just a single object.
     *
     * @param softly       an AssertJ SoftAssertions instance for collecting multiple errors
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @param groups       the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     */
    public static <T> Set<ConstraintViolation<T>> assertOnePropertyViolation(SoftAssertions softly,
                                                                             T object,
                                                                             String propertyName,
                                                                             Class<?>... groups) {
        return assertOnePropertyViolation(softly, VALIDATOR, object, propertyName, groups);
    }

    /**
     * Performs an AssertJ soft assertion that there is exactly one constraint violation on the given object for the
     * given {@code propertyName} using the specified validator.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may or may not have the expected values. This is also the reason it returns a
     * set instead of just a single object.
     *
     * @param softly       an AssertJ SoftAssertions instance for collecting multiple errors
     * @param validator    the Validator instance to perform validation with
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @param groups       the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     */
    public static <T> Set<ConstraintViolation<T>> assertOnePropertyViolation(SoftAssertions softly,
                                                                             Validator validator,
                                                                             T object,
                                                                             String propertyName,
                                                                             Class<?>... groups) {
        return assertPropertyViolations(softly, validator, object, propertyName, 1, groups);
    }

    /**
     * Asserts that there are <strong>no</strong> constraint violations on the given object for the
     * given {@code propertyName} using a default validator.
     *
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @param groups       the optional validation groups to apply
     */
    public static void assertNoPropertyViolations(Object object, String propertyName, Class<?>... groups) {
        assertNoPropertyViolations(VALIDATOR, object, propertyName, groups);
    }

    /**
     * Asserts that there are <strong>no</strong> constraint violations on the given object for the
     * given {@code propertyName} using the specified validator.
     *
     * @param validator    the Validator instance to perform validation with
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @param groups       the optional validation groups to apply
     */
    public static void assertNoPropertyViolations(Validator validator,
                                                  Object object,
                                                  String propertyName,
                                                  Class<?>... groups) {
        assertPropertyViolations(validator, object, propertyName, 0, groups);
    }

    /**
     * Performs an AssertJ soft assertion that there are <strong>no</strong> constraint violations on the given
     * object for the given {@code propertyName} using a default validator.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may not be empty.
     *
     * @param softly       an AssertJ SoftAssertions instance for collecting multiple errors
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @param groups       the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     */
    public static <T> Set<ConstraintViolation<T>> assertNoPropertyViolations(SoftAssertions softly,
                                                                             T object,
                                                                             String propertyName,
                                                                             Class<?>... groups) {
        return assertNoPropertyViolations(softly, VALIDATOR, object, propertyName, groups);
    }

    /**
     * Performs an AssertJ soft assertion that there are <strong>no</strong> constraint violations on the given
     * object for the given {@code propertyName} using the specified validator.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may not be empty.
     *
     * @param softly       an AssertJ SoftAssertions instance for collecting multiple errors
     * @param validator    the Validator instance to perform validation with
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @param groups       the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     */
    public static <T> Set<ConstraintViolation<T>> assertNoPropertyViolations(SoftAssertions softly,
                                                                             Validator validator,
                                                                             T object,
                                                                             String propertyName,
                                                                             Class<?>... groups) {
        return assertPropertyViolations(softly, validator, object, propertyName, 0, groups);
    }

    /**
     * Asserts that there are exactly {@code numExpectedViolations} constraint violations on the given object
     * for the given {@code propertyName} using a default validator.
     *
     * @param object                the object to validate
     * @param propertyName          the name of the property to validate on the given object
     * @param numExpectedViolations the number of violations that are expected
     * @param groups                the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects, when the assertion passes
     */
    public static <T> Set<ConstraintViolation<T>> assertPropertyViolations(T object,
                                                                           String propertyName,
                                                                           int numExpectedViolations,
                                                                           Class<?>... groups) {
        return assertPropertyViolations(VALIDATOR, object, propertyName, numExpectedViolations, groups);
    }

    /**
     * Asserts that there are exactly {@code numExpectedViolations} constraint violations on the given object
     * for the given {@code propertyName} using the specified validator.
     *
     * @param validator             the Validator instance to perform validation with
     * @param object                the object to validate
     * @param propertyName          the name of the property to validate on the given object
     * @param numExpectedViolations the number of violations that are expected
     * @param groups                the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects, when the assertion passes
     */
    public static <T> Set<ConstraintViolation<T>> assertPropertyViolations(Validator validator,
                                                                           T object,
                                                                           String propertyName,
                                                                           int numExpectedViolations,
                                                                           Class<?>... groups) {
        var propertyViolations = validator.validateProperty(object, propertyName, groups);
        assertThat(propertyViolations).describedAs(propertyName).hasSize(numExpectedViolations);
        return propertyViolations;
    }

    /**
     * Performs an AssertJ soft assertion that there are exactly {@code numExpectedViolations} constraint violations
     * on the given object for the given {@code propertyName} using a default validator.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may or may not have the expected values.
     *
     * @param softly                an AssertJ SoftAssertions instance for collecting multiple errors
     * @param object                the object to validate
     * @param propertyName          the name of the property to validate on the given object
     * @param numExpectedViolations the number of violations that are expected
     * @param groups                the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     */
    public static <T> Set<ConstraintViolation<T>> assertPropertyViolations(SoftAssertions softly,
                                                                           T object,
                                                                           String propertyName,
                                                                           int numExpectedViolations,
                                                                           Class<?>... groups) {
        return assertPropertyViolations(softly, VALIDATOR, object, propertyName, numExpectedViolations, groups);
    }

    /**
     * Performs an AssertJ soft assertion that there are exactly {@code numExpectedViolations} constraint violations
     * on the given object for the given {@code propertyName} using the specified validator.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may or may not have the expected values.
     *
     * @param softly                an AssertJ SoftAssertions instance for collecting multiple errors
     * @param validator             the Validator instance to perform validation with
     * @param object                the object to validate
     * @param propertyName          the name of the property to validate on the given object
     * @param numExpectedViolations the number of violations that are expected
     * @param groups                the optional validation groups to apply
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     */
    public static <T> Set<ConstraintViolation<T>> assertPropertyViolations(SoftAssertions softly,
                                                                           Validator validator,
                                                                           T object,
                                                                           String propertyName,
                                                                           int numExpectedViolations,
                                                                           Class<?>... groups) {
        var propertyViolations = validator.validateProperty(object, propertyName, groups);
        softly.assertThat(propertyViolations).describedAs(propertyName).hasSize(numExpectedViolations);
        return propertyViolations;
    }

    /**
     * Asserts that the constraint violations match {@code expectedMessages} on the given object
     * for the given {@code propertyName} using a default validator.
     *
     * @param object           the object to validate
     * @param propertyName     the name of the property to validate on the given object
     * @param expectedMessages the exact validation messages that are expected
     * @return the set of {@link ConstraintViolation} objects, when the assertion passes
     */
    public static <T> Set<ConstraintViolation<T>> assertPropertyViolations(T object,
                                                                           String propertyName,
                                                                           String... expectedMessages) {
        return assertPropertyViolations(VALIDATOR, object, propertyName, expectedMessages);
    }

    /**
     * Asserts that the constraint violations match {@code expectedMessages} on the given object
     * for the given {@code propertyName} using the specified validator.
     *
     * @param validator        the Validator instance to perform validation with
     * @param object           the object to validate
     * @param propertyName     the name of the property to validate on the given object
     * @param expectedMessages the exact validation messages that are expected
     * @return the set of {@link ConstraintViolation} objects, when the assertion passes
     */
    public static <T> Set<ConstraintViolation<T>> assertPropertyViolations(Validator validator,
                                                                           T object,
                                                                           String propertyName,
                                                                           String... expectedMessages) {
        var propertyViolations = validator.validateProperty(object, propertyName);
        var actualMessages = collectActualMessages(propertyViolations);
        var missingMessages = collectMissingMessages(actualMessages, expectedMessages);

        if (isNotNullOrEmpty(missingMessages)) {
            fail(buildFailureMessageForMissingMessages(actualMessages, missingMessages));
        }

        return propertyViolations;
    }

    /**
     * Performs an AssertJ soft assertion that the constraint violations match {@code expectedMessages} on the given
     * object for the given {@code propertyName} using a default validator.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may or may not have the expected values.
     *
     * @param softly           an AssertJ SoftAssertions instance for collecting multiple errors
     * @param object           the object to validate
     * @param propertyName     the name of the property to validate on the given object
     * @param expectedMessages the exact validation messages that are expected
     * @return the set of {@link ConstraintViolation} objects
     */
    public static <T> Set<ConstraintViolation<T>> assertPropertyViolations(SoftAssertions softly,
                                                                           T object,
                                                                           String propertyName,
                                                                           String... expectedMessages) {
        return assertPropertyViolations(softly, VALIDATOR, object, propertyName, expectedMessages);
    }

    /**
     * Performs an AssertJ soft assertion that the constraint violations match {@code expectedMessages} on the given
     * object for the given {@code propertyName} using the specified validator.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may or may not have the expected values.
     *
     * @param softly           an AssertJ SoftAssertions instance for collecting multiple errors
     * @param validator        the Validator instance to perform validation with
     * @param object           the object to validate
     * @param propertyName     the name of the property to validate on the given object
     * @param expectedMessages the exact validation messages that are expected
     * @return the set of {@link ConstraintViolation} objects
     */
    public static <T> Set<ConstraintViolation<T>> assertPropertyViolations(SoftAssertions softly,
                                                                           Validator validator,
                                                                           T object,
                                                                           String propertyName,
                                                                           String... expectedMessages) {
        var propertyViolations = validator.validateProperty(object, propertyName);
        var actualMessages = collectActualMessages(propertyViolations);
        var missingMessages = collectMissingMessages(actualMessages, expectedMessages);

        if (isNotNullOrEmpty(missingMessages)) {
            softly.fail(buildFailureMessageForMissingMessages(actualMessages, missingMessages));
        }

        return propertyViolations;
    }

    private static <T> List<String> collectActualMessages(Set<ConstraintViolation<T>> violations) {
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(toList());
    }

    private static List<String> collectMissingMessages(List<String> actualMessages, String... expectedMessages) {
        return Arrays.stream(expectedMessages)
                .filter(value -> !actualMessages.contains(value))
                .collect(toList());
    }

    private static String buildFailureMessageForMissingMessages(List<String> actualMessages,
                                                                List<String> missingMessages) {
        return KiwiStrings.format("Messages [%s] not found in actual messages %s",
                String.join(", ", missingMessages), actualMessages);
    }
}
