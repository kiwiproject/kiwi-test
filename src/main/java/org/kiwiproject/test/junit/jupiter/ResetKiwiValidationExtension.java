package org.kiwiproject.test.junit.jupiter;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.validation.KiwiValidations;

import javax.validation.Validator;

/**
 * A JUnit Jupiter {@link org.junit.jupiter.api.extension.Extension} that resets the (static)
 * {@link javax.validation.Validator} used in {@link KiwiValidations}.
 *
 * @see KiwiValidations#setValidator(Validator)
 * @see KiwiValidations#getValidator()
 */
public class ResetKiwiValidationExtension implements AfterAllCallback {

    @Override
    public void afterAll(ExtensionContext context) {
        var freshValidator = KiwiValidations.newValidator();
        KiwiValidations.setValidator(freshValidator);
    }
}
