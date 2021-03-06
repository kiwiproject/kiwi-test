package org.kiwiproject.test.validation;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.kiwiproject.collect.KiwiLists.isNotNullOrEmpty;

import lombok.experimental.UtilityClass;
import org.assertj.core.api.SoftAssertions;
import org.kiwiproject.base.KiwiStrings;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
     *
     * @param softly an AssertJ SoftAssertions instance for collecting multiple errors
     * @param object the object to validate
     * @param groups the optional validation groups to apply
     */
    public static void assertNoViolations(SoftAssertions softly, Object object, Class<?>... groups) {
        assertNoViolations(softly, VALIDATOR, object, groups);
    }

    /**
     * Performs an AssertJ soft assertion that there are <strong>no</strong> constraint violations on the given
     * object using the specified validator.
     *
     * @param softly    an AssertJ SoftAssertions instance for collecting multiple errors
     * @param validator the Validator instance to perform validation with
     * @param object    the object to validate
     * @param groups    the optional validation groups to apply
     */
    public static void assertNoViolations(SoftAssertions softly,
                                          Validator validator,
                                          Object object,
                                          Class<?>... groups) {
        var violations = validator.validate(object, groups);
        softly.assertThat(violations).isEmpty();
    }

    /**
     * Asserts that there is at least one constraint violation on the given object using a default validator.
     *
     * @param object the object to validate
     * @param groups the optional validation groups to apply
     */
    public static void assertHasViolations(Object object, Class<?>... groups) {
        assertHasViolations(VALIDATOR, object, groups);
    }

    /**
     * Asserts that there is at least one constraint violation on the given object using the given validator.
     *
     * @param validator the Validator instance to perform validation with
     * @param object    the object to validate
     * @param groups    the optional validation groups to apply
     */
    public static void assertHasViolations(Validator validator, Object object, Class<?>... groups) {
        var violations = validator.validate(object, groups);
        assertThat(violations)
                .describedAs("Expected at least one constraint violation")
                .isNotEmpty();
    }

    /**
     * Performs an AssertJ soft assertion that is at least one constraint violation
     * on the given object using a default validator.
     *
     * @param softly an AssertJ SoftAssertions instance for collecting multiple errors
     * @param object the object to validate
     * @param groups the optional validation groups to apply
     */
    public static void assertHasViolations(SoftAssertions softly, Object object, Class<?>... groups) {
        assertHasViolations(softly, VALIDATOR, object, groups);
    }

    /**
     * Performs an AssertJ soft assertion that is at least one constraint violation
     * on the given object using the given validator.
     *
     * @param softly    an AssertJ SoftAssertions instance for collecting multiple errors
     * @param validator the Validator instance to perform validation with
     * @param object    the object to validate
     * @param groups    the optional validation groups to apply
     */
    public static void assertHasViolations(SoftAssertions softly,
                                           Validator validator,
                                           Object object,
                                           Class<?>... groups) {
        var violations = validator.validate(object, groups);
        softly.assertThat(violations)
                .describedAs("Expected at least one constraint violation")
                .isNotEmpty();
    }

    /**
     * Asserts that there are exactly {@code numExpectedViolations} constraint violations on the given object
     * using a default validator.
     *
     * @param object                the object to validate
     * @param numExpectedViolations the number of violations that are expected
     * @param groups                the optional validation groups to apply
     */
    public static void assertViolations(Object object, int numExpectedViolations, Class<?>... groups) {
        assertViolations(VALIDATOR, object, numExpectedViolations, groups);
    }

    /**
     * Asserts that there are exactly {@code numExpectedViolations} constraint violations on the given object
     * using the specified validator.
     *
     * @param validator             the Validator instance to perform validation with
     * @param object                the object to validate
     * @param numExpectedViolations the number of violations that are expected
     * @param groups                the optional validation groups to apply
     */
    public static void assertViolations(Validator validator,
                                        Object object,
                                        int numExpectedViolations,
                                        Class<?>... groups) {
        var violations = validator.validate(object, groups);
        assertThat(violations).hasSize(numExpectedViolations);
    }

    /**
     * Performs an AssertJ soft assertion that there are exactly {@code numExpectedViolations} constraint violations
     * on the given object using a default validator.
     *
     * @param softly                an AssertJ SoftAssertions instance for collecting multiple errors
     * @param object                the object to validate
     * @param numExpectedViolations the number of violations that are expected
     * @param groups                the optional validation groups to apply
     */
    public static void assertViolations(SoftAssertions softly,
                                        Object object,
                                        int numExpectedViolations,
                                        Class<?>... groups) {
        assertViolations(softly, VALIDATOR, object, numExpectedViolations, groups);
    }

    /**
     * Performs an AssertJ soft assertion that there are exactly {@code numExpectedViolations} constraint violations
     * on the given object using the specified validator.
     *
     * @param softly                an AssertJ SoftAssertions instance for collecting multiple errors
     * @param validator             the Validator instance to perform validation with
     * @param object                the object to validate
     * @param numExpectedViolations the number of violations that are expected
     * @param groups                the optional validation groups to apply
     */
    public static void assertViolations(SoftAssertions softly,
                                        Validator validator,
                                        Object object,
                                        int numExpectedViolations,
                                        Class<?>... groups) {
        var violations = validator.validate(object, groups);
        softly.assertThat(violations).hasSize(numExpectedViolations);
    }

    /**
     * Asserts that there is exactly one constraint violation on the given object for the given {@code propertyName}
     * using a default validator.
     *
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @param groups       the optional validation groups to apply
     */
    public static void assertOnePropertyViolation(Object object, String propertyName, Class<?>... groups) {
        assertOnePropertyViolation(VALIDATOR, object, propertyName, groups);
    }

    /**
     * Asserts that there is exactly one constraint violation on the given object for the given {@code propertyName}
     * using the specified validator.
     *
     * @param validator    the Validator instance to perform validation with
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @param groups       the optional validation groups to apply
     */
    public static void assertOnePropertyViolation(Validator validator,
                                                  Object object,
                                                  String propertyName,
                                                  Class<?>... groups) {
        assertPropertyViolations(validator, object, propertyName, 1, groups);
    }

    /**
     * Performs an AssertJ soft assertion that there is exactly one constraint violation on the given object for the
     * given {@code propertyName} using a default validator.
     *
     * @param softly       an AssertJ SoftAssertions instance for collecting multiple errors
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @param groups       the optional validation groups to apply
     */
    public static void assertOnePropertyViolation(SoftAssertions softly,
                                                  Object object,
                                                  String propertyName,
                                                  Class<?>... groups) {
        assertOnePropertyViolation(softly, VALIDATOR, object, propertyName, groups);
    }

    /**
     * Performs an AssertJ soft assertion that there is exactly one constraint violation on the given object for the
     * given {@code propertyName} using the specified validator.
     *
     * @param softly       an AssertJ SoftAssertions instance for collecting multiple errors
     * @param validator    the Validator instance to perform validation with
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @param groups       the optional validation groups to apply
     */
    public static void assertOnePropertyViolation(SoftAssertions softly,
                                                  Validator validator,
                                                  Object object,
                                                  String propertyName,
                                                  Class<?>... groups) {
        assertPropertyViolations(softly, validator, object, propertyName, 1, groups);
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
     *
     * @param softly       an AssertJ SoftAssertions instance for collecting multiple errors
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @param groups       the optional validation groups to apply
     */
    public static void assertNoPropertyViolations(SoftAssertions softly,
                                                  Object object,
                                                  String propertyName,
                                                  Class<?>... groups) {
        assertNoPropertyViolations(softly, VALIDATOR, object, propertyName, groups);
    }

    /**
     * Performs an AssertJ soft assertion that there are <strong>no</strong> constraint violations on the given
     * object for the given {@code propertyName} using the specified validator.
     *
     * @param softly       an AssertJ SoftAssertions instance for collecting multiple errors
     * @param validator    the Validator instance to perform validation with
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @param groups       the optional validation groups to apply
     */
    public static void assertNoPropertyViolations(SoftAssertions softly,
                                                  Validator validator,
                                                  Object object,
                                                  String propertyName,
                                                  Class<?>... groups) {
        assertPropertyViolations(softly, validator, object, propertyName, 0, groups);
    }

    /**
     * Asserts that there are exactly {@code numExpectedViolations} constraint violations on the given object
     * for the given {@code propertyName} using a default validator.
     *
     * @param object                the object to validate
     * @param propertyName          the name of the property to validate on the given object
     * @param numExpectedViolations the number of violations that are expected
     * @param groups                the optional validation groups to apply
     */
    public static void assertPropertyViolations(Object object,
                                                String propertyName,
                                                int numExpectedViolations,
                                                Class<?>... groups) {
        assertPropertyViolations(VALIDATOR, object, propertyName, numExpectedViolations, groups);
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
     */
    public static void assertPropertyViolations(Validator validator,
                                                Object object,
                                                String propertyName,
                                                int numExpectedViolations,
                                                Class<?>... groups) {
        var propertyViolations = validator.validateProperty(object, propertyName, groups);
        assertThat(propertyViolations).describedAs(propertyName).hasSize(numExpectedViolations);
    }

    /**
     * Performs an AssertJ soft assertion that there are exactly {@code numExpectedViolations} constraint violations
     * on the given object for the given {@code propertyName} using a default validator.
     *
     * @param softly                an AssertJ SoftAssertions instance for collecting multiple errors
     * @param object                the object to validate
     * @param propertyName          the name of the property to validate on the given object
     * @param numExpectedViolations the number of violations that are expected
     * @param groups                the optional validation groups to apply
     */
    public static void assertPropertyViolations(SoftAssertions softly,
                                                Object object,
                                                String propertyName,
                                                int numExpectedViolations,
                                                Class<?>... groups) {
        assertPropertyViolations(softly, VALIDATOR, object, propertyName, numExpectedViolations, groups);
    }

    /**
     * Performs an AssertJ soft assertion that there are exactly {@code numExpectedViolations} constraint violations
     * on the given object for the given {@code propertyName} using the specified validator.
     *
     * @param softly                an AssertJ SoftAssertions instance for collecting multiple errors
     * @param validator             the Validator instance to perform validation with
     * @param object                the object to validate
     * @param propertyName          the name of the property to validate on the given object
     * @param numExpectedViolations the number of violations that are expected
     * @param groups                the optional validation groups to apply
     */
    public static void assertPropertyViolations(SoftAssertions softly,
                                                Validator validator,
                                                Object object,
                                                String propertyName,
                                                int numExpectedViolations,
                                                Class<?>... groups) {
        var propertyViolations = validator.validateProperty(object, propertyName, groups);
        softly.assertThat(propertyViolations).describedAs(propertyName).hasSize(numExpectedViolations);
    }

    /**
     * Asserts that the constraint violations match {@code expectedMessages} on the given object
     * for the given {@code propertyName} using a default validator.
     *
     * @param object           the object to validate
     * @param propertyName     the name of the property to validate on the given object
     * @param expectedMessages the exact validation messages that are expected
     */
    public static void assertPropertyViolations(Object object, String propertyName, String... expectedMessages) {
        assertPropertyViolations(VALIDATOR, object, propertyName, expectedMessages);
    }

    /**
     * Asserts that the constraint violations match {@code expectedMessages} on the given object
     * for the given {@code propertyName} using the specified validator.
     *
     * @param validator        the Validator instance to perform validation with
     * @param object           the object to validate
     * @param propertyName     the name of the property to validate on the given object
     * @param expectedMessages the exact validation messages that are expected
     */
    public static void assertPropertyViolations(Validator validator,
                                                Object object,
                                                String propertyName,
                                                String... expectedMessages) {
        var propertyViolations = validator.validateProperty(object, propertyName);
        var actualMessages = collectActualMessages(propertyViolations);
        var missingMessages = collectMissingMessages(actualMessages, expectedMessages);

        if (isNotNullOrEmpty(missingMessages)) {
            fail(buildFailureMessageForMissingMessages(actualMessages, missingMessages));
        }
    }

    /**
     * Performs an AssertJ soft assertion that the constraint violations match {@code expectedMessages} on the given
     * object for the given {@code propertyName} using a default validator.
     *
     * @param softly           an AssertJ SoftAssertions instance for collecting multiple errors
     * @param object           the object to validate
     * @param propertyName     the name of the property to validate on the given object
     * @param expectedMessages the exact validation messages that are expected
     */
    public static void assertPropertyViolations(SoftAssertions softly,
                                                Object object,
                                                String propertyName,
                                                String... expectedMessages) {
        assertPropertyViolations(softly, VALIDATOR, object, propertyName, expectedMessages);
    }

    /**
     * Performs an AssertJ soft assertion that the constraint violations match {@code expectedMessages} on the given
     * object for the given {@code propertyName} using the specified validator.
     *
     * @param softly           an AssertJ SoftAssertions instance for collecting multiple errors
     * @param validator        the Validator instance to perform validation with
     * @param object           the object to validate
     * @param propertyName     the name of the property to validate on the given object
     * @param expectedMessages the exact validation messages that are expected
     */
    public static void assertPropertyViolations(SoftAssertions softly,
                                                Validator validator,
                                                Object object,
                                                String propertyName,
                                                String... expectedMessages) {
        var propertyViolations = validator.validateProperty(object, propertyName);
        var actualMessages = collectActualMessages(propertyViolations);
        var missingMessages = collectMissingMessages(actualMessages, expectedMessages);

        if (isNotNullOrEmpty(missingMessages)) {
            softly.fail(buildFailureMessageForMissingMessages(actualMessages, missingMessages));
        }
    }

    private static List<String> collectActualMessages(Set<ConstraintViolation<Object>> violations) {
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
