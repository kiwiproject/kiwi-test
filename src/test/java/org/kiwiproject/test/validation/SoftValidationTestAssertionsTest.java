package org.kiwiproject.test.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Value;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.hibernate.validator.constraints.Length;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Objects;

@DisplayName("SoftValidationTestAssertions")
@ExtendWith(SoftAssertionsExtension.class)
class SoftValidationTestAssertionsTest {

    private SoftValidationTestAssertions softValidation;

    @Nested
    class AssertPropertyViolationMessages {

        @Test
        void shouldHaveExpectedMessages(SoftAssertions softly) {
            var alice = Person.builder().age(135).build();

            softValidation = newSoftValidation(softly);

            softValidation.assertPropertyViolations(alice, "firstName", "must not be blank");
            softValidation.assertPropertyViolations(alice, "lastName", "must not be blank");
            softValidation.assertPropertyViolations(alice, "age", "must be less than or equal to 130");
            softValidation.assertPropertyViolations(alice, "nickname");
        }

        @Test
        void shouldNotCareAboutMessageOrder(SoftAssertions softly) {
            var alice = Person.builder().lastName("").build();
            var emptyError = "must not be blank";
            var lengthError = "length must be between 2 and 255";

            softValidation = newSoftValidation(softly);

            softValidation.assertPropertyViolations(alice, "lastName", emptyError, lengthError);
            softValidation.assertPropertyViolations(alice, "lastName", lengthError, emptyError);
        }
    }

    @Nested
    class AssertNoViolations {

        @Test
        void shouldVerifyNoViolations(SoftAssertions softly) {
            var alice = new Person("Alice", "Mayberry", 32, null);

            var constraintViolations = newSoftValidation(softly).assertNoViolations(alice);
            assertThat(constraintViolations).isEmpty();
        }
    }

    @Nested
    class AssertNoPropertyViolations {

        @Test
        void shouldVerifyNoPropertyViolations(SoftAssertions softly) {
            var alice = new Person("Alice", "Mayberry", 32, null);

            softValidation = newSoftValidation(softly);

            var firstNameViolations = softValidation.assertNoPropertyViolations(alice, "firstName");
            assertThat(firstNameViolations).isEmpty();

            var lastNameViolations = softValidation.assertNoPropertyViolations(alice, "lastName");
            assertThat(lastNameViolations).isEmpty();

            var ageViolations = softValidation.assertNoPropertyViolations(alice, "age");
            assertThat(ageViolations).isEmpty();

            var nicknameViolations = softValidation.assertNoPropertyViolations(alice, "nickname");
            assertThat(nicknameViolations).isEmpty();
        }
    }

    @Nested
    class AssertViolations {

        @Test
        void shouldVerifyExpectedNumberOfViolationsForObject(SoftAssertions softly) {
            var alice = new Person("", "Mayberry", 132, null);

            var constraintViolations = newSoftValidation(softly).assertViolations(alice, 2);
            assertThat(constraintViolations).hasSize(2);
        }
    }

    @Nested
    class AssertPropertyViolations {

        @Test
        void shouldVerifyExpectedNumberOfViolationsForProperty(SoftAssertions softly) {
            var alice = new Person("", "", 132, null);

            softValidation = newSoftValidation(softly);

            var firstNameViolations = softValidation.assertPropertyViolations(alice, "firstName", 1);
            assertThat(firstNameViolations).hasSize(1);

            var lastNameViolations = softValidation.assertPropertyViolations(alice, "lastName", 2);
            assertThat(lastNameViolations).hasSize(2);

            var ageViolations = softValidation.assertPropertyViolations(alice, "age", 1);
            assertThat(ageViolations).hasSize(1);

            var nicknameViolations = softValidation.assertPropertyViolations(alice, "nickname", 0);
            assertThat(nicknameViolations).isEmpty();
        }
    }

    @Nested
    class AssertOnePropertyViolation {

        @Test
        void shouldVerifyOneViolationForProperty(SoftAssertions softly) {
            var alice = new Person("", null, 132, null);

            softValidation = newSoftValidation(softly);

            softValidation.assertOnePropertyViolation(alice, "firstName");
            softValidation.assertOnePropertyViolation(alice, "lastName");
            softValidation.assertOnePropertyViolation(alice, "age");
        }
    }

    @Nested
    class AssertPropertiesEachHaveOneViolation {

        @Test
        void shouldVerifyOneViolationForMultipleProperties(SoftAssertions softly) {
            var alice = new Person("", null, 132, null);

            var constraintViolations = newSoftValidation(softly)
                    .assertPropertiesEachHaveOneViolation(alice, "firstName", "lastName", "age");
            assertThat(constraintViolations)
                    .extracting(ConstraintViolation::getPropertyPath)
                    .map(Objects::toString)
                    .containsExactlyInAnyOrder("firstName", "lastName", "age");
        }
    }

    @Nested
    class AssertPropertiesEachHaveNoViolations {

        @Test
        void shouldVerifyNoViolationsForMultipleProperties(SoftAssertions softly) {
            var alice = new Person("Alice", "Mayberry", 32, null);

            var constraintViolations = newSoftValidation(softly)
                    .assertPropertiesEachHaveNoViolations(alice, "firstName", "lastName", "age");
            assertThat(constraintViolations).isEmpty();
        }
    }

    /**
     * @implNote Because we cannot inject a {@link SoftAssertions} object into any method not annotated with
     * {@link Test}, we have to create a new {@link SoftValidationTestAssertions} instance in every test instead of
     * doing it in a {@link org.junit.jupiter.api.BeforeEach} (which is what we would generally prefer).
     */
    private static SoftValidationTestAssertions newSoftValidation(SoftAssertions softly) {
        return new SoftValidationTestAssertions(softly);
    }

    @Builder
    @Value
    private static class Person {

        @NotBlank
        String firstName;

        @NotBlank
        @Length(min = 2, max = 255)
        String lastName;

        @PositiveOrZero
        @Max(130)
        int age;

        String nickname;
    }
}
