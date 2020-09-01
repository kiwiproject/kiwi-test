package org.kiwiproject.test.validation;

import lombok.Builder;
import lombok.Value;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.hibernate.validator.constraints.Length;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;

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

            newSoftValidation(softly).assertNoViolations(alice);
        }
    }

    @Nested
    class AssertNoPropertyViolations {

        @Test
        void shouldVerifyNoPropertyViolations(SoftAssertions softly) {
            var alice = new Person("Alice", "Mayberry", 32, null);

            softValidation = newSoftValidation(softly);

            softValidation.assertNoPropertyViolations(alice, "firstName");
            softValidation.assertNoPropertyViolations(alice, "lastName");
            softValidation.assertNoPropertyViolations(alice, "age");
            softValidation.assertNoPropertyViolations(alice, "nickname");
        }
    }

    @Nested
    class AssertViolations {

        @Test
        void shouldVerifyExpectedNumberOfViolationsForObject(SoftAssertions softly) {
            var alice = new Person("", "Mayberry", 132, null);

            newSoftValidation(softly).assertViolations(alice, 2);
        }
    }

    @Nested
    class AssertPropertyViolations {

        @Test
        void shouldVerifyExpectedNumberOfViolationsForProperty(SoftAssertions softly) {
            var alice = new Person("", "", 132, null);

            softValidation = newSoftValidation(softly);

            softValidation.assertPropertyViolations(alice, "firstName", 1);
            softValidation.assertPropertyViolations(alice, "lastName", 2);
            softValidation.assertPropertyViolations(alice, "age", 1);
            softValidation.assertPropertyViolations(alice, "nickname", 0);
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

            newSoftValidation(softly).assertPropertiesEachHaveOneViolation(alice, "firstName", "lastName", "age");
        }
    }

    @Nested
    class AssertPropertiesEachHaveNoViolations {

        @Test
        void shouldVerifyNoViolationsForMultipleProperties(SoftAssertions softly) {
            var alice = new Person("Alice", "Mayberry", 32, null);

            newSoftValidation(softly).assertPropertiesEachHaveNoViolations(alice, "firstName", "lastName", "age");
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
