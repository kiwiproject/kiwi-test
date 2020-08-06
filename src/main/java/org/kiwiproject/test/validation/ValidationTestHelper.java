package org.kiwiproject.test.validation;

import lombok.experimental.UtilityClass;

import javax.validation.Validation;
import javax.validation.Validator;

/**
 * Provides helper methods for making assertions on validation of objects using the Bean Validation API.
 * <p>
 */
@UtilityClass
public class ValidationTestHelper {

    // TODO Currently this contains only a subset of all the methods. Need to add the rest.

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
}
