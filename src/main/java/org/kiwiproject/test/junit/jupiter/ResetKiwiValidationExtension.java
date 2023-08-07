package org.kiwiproject.test.junit.jupiter;

import jakarta.validation.Validator;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.validation.KiwiValidations;

/**
 * A JUnit Jupiter {@link org.junit.jupiter.api.extension.Extension} that resets the (static)
 * {@link jakarta.validation.Validator} used in {@link KiwiValidations}.
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
