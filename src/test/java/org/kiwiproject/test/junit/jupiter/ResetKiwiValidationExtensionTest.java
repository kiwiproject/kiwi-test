package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kiwiproject.test.mockito.MockitoHelpers.mockNeverCalled;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kiwiproject.validation.KiwiValidations;

@DisplayName("ResetKiwiValidationExtension")
class ResetKiwiValidationExtensionTest {

    @Nested
    class AfterAll {

        private ExtensionContext extensionContext;

        @BeforeEach
        void setUp() {
            extensionContext = mockNeverCalled(ExtensionContext.class);
        }

        @RepeatedTest(10)
        void shouldSetNewValidatorInstance() {
            var originalValidator = KiwiValidations.getValidator();

            new ResetKiwiValidationExtension().afterAll(extensionContext);

            assertThat(KiwiValidations.getValidator()).isNotSameAs(originalValidator);
        }
    }
}
