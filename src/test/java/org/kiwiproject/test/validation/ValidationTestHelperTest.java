package org.kiwiproject.test.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ValidationTestHelper")
class ValidationTestHelperTest {

    @Test
    void shouldReturnSingletonValidator() {
        var validator1 = ValidationTestHelper.getValidator();
        var validator2 = ValidationTestHelper.getValidator();
        var validator3 = ValidationTestHelper.getValidator();

        assertThat(validator1)
                .isNotNull()
                .isSameAs(validator2)
                .isSameAs(validator3);
    }

    @Test
    void shouldBuildNewValidators() {
        var validator1 = ValidationTestHelper.newValidator();
        var validator2 = ValidationTestHelper.newValidator();

        assertThat(validator1)
                .isNotNull()
                .isNotSameAs(validator2);
    }
}