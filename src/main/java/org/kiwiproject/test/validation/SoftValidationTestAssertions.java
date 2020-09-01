package org.kiwiproject.test.validation;

import org.assertj.core.api.SoftAssertions;

import javax.validation.Validator;
import java.util.Arrays;

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
     *
     * @param object the object to validate
     * @see ValidationTestHelper#assertNoViolations(SoftAssertions, Validator, Object, Class...)
     */
    public void assertNoViolations(Object object) {
        ValidationTestHelper.assertNoViolations(softly, validator, object);
    }

    /**
     * Softly asserts that there are exactly {@code numExpectedViolations} constraint violations on the given object.
     *
     * @param object                the object to validate
     * @param numExpectedViolations the number of violations that are expected
     * @see ValidationTestHelper#assertViolations(SoftAssertions, Validator, Object, int, Class...)
     */
    public void assertViolations(Object object, int numExpectedViolations) {
        ValidationTestHelper.assertViolations(softly, validator, object, numExpectedViolations);
    }

    /**
     * Softly asserts that there is exactly one constraint violation on the given object for the given
     * {@code propertyName}.
     *
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @see ValidationTestHelper#assertOnePropertyViolation(SoftAssertions, Validator, Object, String, Class...)
     */
    public void assertOnePropertyViolation(Object object, String propertyName) {
        ValidationTestHelper.assertOnePropertyViolation(softly, validator, object, propertyName);
    }

    /**
     * Softly asserts that each of the specified properties has exactly one constraint violation on the given
     * object.
     *
     * @param object        the object to validate
     * @param propertyNames the names of the properties to validate on the given object
     */
    public void assertPropertiesEachHaveOneViolation(Object object, String... propertyNames) {
        Arrays.stream(propertyNames)
                .forEach(propertyName -> assertOnePropertyViolation(object, propertyName));
    }

    /**
     * Softly asserts that there are <strong>no</strong> constraint violations on the given object for the
     * given {@code propertyName}.
     *
     * @param object       the object to validate
     * @param propertyName the name of the property to validate on the given object
     * @see ValidationTestHelper#assertNoPropertyViolations(SoftAssertions, Validator, Object, String, Class...)
     */
    public void assertNoPropertyViolations(Object object, String propertyName) {
        ValidationTestHelper.assertNoPropertyViolations(softly, validator, object, propertyName);
    }

    /**
     * Softly assert that each of the specified properties has no constraint violations on the given object.
     *
     * @param object        the object to validate
     * @param propertyNames the names of the properties to validate on the given object
     */
    public void assertPropertiesEachHaveNoViolations(Object object, String... propertyNames) {
        Arrays.stream(propertyNames)
                .forEach(propertyName -> assertNoPropertyViolations(object, propertyName));
    }

    /**
     * Softly asserts that there are exactly {@code numExpectedViolations} constraint violations on the given object
     * for the given {@code propertyName}.
     *
     * @param object                the object to validate
     * @param propertyName          the name of the property to validate on the given object
     * @param numExpectedViolations the number of violations that are expected
     * @see ValidationTestHelper#assertPropertyViolations(SoftAssertions, Validator, Object, String, int, Class...)
     */
    public void assertPropertyViolations(Object object, String propertyName, int numExpectedViolations) {
        ValidationTestHelper.assertPropertyViolations(softly, validator, object, propertyName, numExpectedViolations);
    }

    /**
     * Softly asserts that the constraint violations match {@code expectedMessages} on the given
     * object for the given {@code propertyName}.
     *
     * @param object           the object to validate
     * @param propertyName     the name of the property to validate on the given object
     * @param expectedMessages the exact validation messages that are expected
     * @see ValidationTestHelper#assertPropertyViolations(SoftAssertions, Validator, Object, String, String...)
     */
    public void assertPropertyViolations(Object object, String propertyName, String... expectedMessages) {
        ValidationTestHelper.assertPropertyViolations(softly, validator, object, propertyName, expectedMessages);
    }
}
