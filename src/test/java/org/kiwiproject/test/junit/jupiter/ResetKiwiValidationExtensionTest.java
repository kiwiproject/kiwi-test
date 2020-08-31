package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.kiwiproject.validation.KiwiValidations;

@DisplayName("ResetKiwiValidationExtension")
class ResetKiwiValidationExtensionTest {

    @Nested
    class AfterAll {

        @RepeatedTest(10)
        void shouldSetNewValidatorInstance() {
            var originalValidator = KiwiValidations.getValidator();

            new ResetKiwiValidationExtension().afterAll(null);

            assertThat(KiwiValidations.getValidator()).isNotSameAs(originalValidator);
        }
    }
}
