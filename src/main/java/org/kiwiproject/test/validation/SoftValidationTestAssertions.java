package org.kiwiproject.test.validation;

import static java.util.stream.Collectors.toUnmodifiableSet;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.assertj.core.api.SoftAssertions;

import java.util.Arrays;
import java.util.Set;

/**
 * A helper class with methods for making AssertJ {@link SoftAssertions} when validating objects using the
 * Jakarta Beans Validation API.
 * <p>
 * This is basically instance-based wrapper around {@link ValidationTestHelper} with preset {@link SoftAssertions}
 * and {@link Validator} objects to avoid repeating the same arguments in successive calls. It does provide a few
 * additional convenience methods to validate multiple properties at the same time.
 */
public class SoftValidationTestAssertions {

    // TODO Consider adding validation group support (which may require breaking API changes and a major version
    //  revision or maybe some creative method naming shenanigans)

    private final SoftAssertions softly;
    private final Validator validator;

    /**
     * Create an instance with a default {@link Validator} instance.
     *
     * @param softly the {@link SoftAssertions} instance to use
     */
    public SoftValidationTestAssertions(SoftAssertions softly) {
        this(softly, ValidationTestHelper.newValidator());
    }

    /**
     * Create an instance.
     *
     * @param softly    the {@link SoftAssertions} instance to use
     * @param validator the {@link Validator} to use when validating objects
     */
    public SoftValidationTestAssertions(SoftAssertions softly, Validator validator) {
        this.softly = softly;
        this.validator = validator;
    }

    /**
     * Softly asserts that there are <strong>no</strong> constraint violations.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may not be empty.
     *
     * @param object the object to validate
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     * @see ValidationTestHelper#assertNoViolations(SoftAssertions, Validator, Object, Class...)
     */
    public <T> Set<ConstraintViolation<T>> assertNoViolations(T object) {
        return ValidationTestHelper.assertNoViolations(softly, validator, object);
    }

    /**
     * Softly asserts that there are exactly {@code numExpectedViolations} constraint violations on the given object.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may or may not have the expected values.
     *
     * @param object                the object to validate
     * @param numExpectedViolations the number of violations that are expected
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     * @see ValidationTestHelper#assertViolations(SoftAssertions, Validator, Object, int, Class...)
     */
    public <T> Set<ConstraintViolation<T>> assertViolations(T object, int numExpectedViolations) {
        return ValidationTestHelper.assertViolations(softly, validator, object, numExpectedViolations);
    }

    /**
     * Softly asserts that there is exactly one constraint violation on the given object for the given
     * {@code propertyName}.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may or may not have the expected values. This is also the reason it returns a
     * set instead of just a single object.
     *
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     * @see ValidationTestHelper#assertOnePropertyViolation(SoftAssertions, Validator, Object, String, Class...)
     */
    public <T> Set<ConstraintViolation<T>> assertOnePropertyViolation(T object, String propertyName) {
        return ValidationTestHelper.assertOnePropertyViolation(softly, validator, object, propertyName);
    }

    /**
     * Softly asserts that each of the specified properties has exactly one constraint violation on the given
     * object.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may or may not have the expected values.
     *
     * @param object        the object to validate
     * @param propertyNames the names of the properties to validate on the given object
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     */
    public <T> Set<ConstraintViolation<T>> assertPropertiesEachHaveOneViolation(T object, String... propertyNames) {
        return Arrays.stream(propertyNames)
                .flatMap(propertyName -> assertOnePropertyViolation(object, propertyName).stream())
                .collect(toUnmodifiableSet());
    }

    /**
     * Softly asserts that there are <strong>no</strong> constraint violations on the given object for the
     * given {@code propertyName}.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may not be empty.
     *
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     * @see ValidationTestHelper#assertNoPropertyViolations(SoftAssertions, Validator, Object, String, Class...)
     */
    public <T> Set<ConstraintViolation<T>> assertNoPropertyViolations(T object, String propertyName) {
        return ValidationTestHelper.assertNoPropertyViolations(softly, validator, object, propertyName);
    }

    /**
     * Softly assert that each of the specified properties has no constraint violations on the given object.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may not be empty.
     *
     * @param object        the object to validate
     * @param propertyNames the names of the properties to validate on the given object
     * @return the set of {@link ConstraintViolation} objects that were found when validating the object
     */
    public <T> Set<ConstraintViolation<T>> assertPropertiesEachHaveNoViolations(T object, String... propertyNames) {
        return Arrays.stream(propertyNames)
                .flatMap(propertyName -> assertNoPropertyViolations(object, propertyName).stream())
                .collect(toUnmodifiableSet());
    }

    /**
     * Softly asserts that there are exactly {@code numExpectedViolations} constraint violations on the given object
     * for the given {@code propertyName}.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may or may not have the expected values.
     *
     * @param object                the object to validate
     * @param propertyName          the name of the property to validate on the given object
     * @param numExpectedViolations the number of violations that are expected
     * @return the set of {@link ConstraintViolation} objects
     * @see ValidationTestHelper#assertPropertyViolations(SoftAssertions, Validator, Object, String, int, Class...)
     */
    public <T> Set<ConstraintViolation<T>> assertPropertyViolations(T object, String propertyName, int numExpectedViolations) {
        return ValidationTestHelper.assertPropertyViolations(softly, validator, object, propertyName, numExpectedViolations);
    }

    /**
     * Softly asserts that the constraint violations match {@code expectedMessages} on the given
     * object for the given {@code propertyName}.
     * <p>
     * Because this uses {@link SoftAssertions} and does not fail immediately, the returned set of
     * constraint violations may or may not have the expected values.
     *
     * @param object           the object to validate
     * @param propertyName     the name of the property to validate on the given object
     * @param expectedMessages the exact validation messages that are expected
     * @return the set of {@link ConstraintViolation} objects
     * @see ValidationTestHelper#assertPropertyViolations(SoftAssertions, Validator, Object, String, String...)
     */
    public <T> Set<ConstraintViolation<T>> assertPropertyViolations(T object, String propertyName, String... expectedMessages) {
        return ValidationTestHelper.assertPropertyViolations(softly, validator, object, propertyName, expectedMessages);
    }
}
