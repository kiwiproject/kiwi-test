package org.kiwiproject.test.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.kiwiproject.collect.KiwiLists.first;
import static org.kiwiproject.collect.KiwiLists.fourth;
import static org.kiwiproject.collect.KiwiLists.second;
import static org.kiwiproject.collect.KiwiLists.third;
import static org.kiwiproject.test.junit.ParameterizedTests.inputs;
import static org.kiwiproject.test.validation.ParameterizedValidationTestHelper.expectedMessages;
import static org.kiwiproject.test.validation.ParameterizedValidationTestHelper.expectedMessagesLists;
import static org.kiwiproject.test.validation.ParameterizedValidationTestHelper.expectedViolations;
import static org.kiwiproject.test.validation.ParameterizedValidationTestHelper.noExpectedMessages;
import static org.kiwiproject.test.validation.ParameterizedValidationTestHelper.validationGroups;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Past;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.error.AssertJMultipleFailuresError;
import org.hibernate.validator.constraints.Length;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

/**
 * @implNote Suppressing Sonar "Assertions should be complete" warning b/c it doesn't
 * know that the methods inside {@link ParameterizedValidationTestHelper} perform assertions.
 */
@DisplayName("ParameterizedValidationTestHelper")
@SuppressWarnings("java:S2970")
class ParameterizedValidationTestHelperTest {

    private ParameterizedValidationTestHelper testHelper;
    private SoftAssertions softly;
    private Person person;

    @BeforeEach
    void setUp() {
        softly = new SoftAssertions();
        testHelper = new ParameterizedValidationTestHelper(softly);
        person = new Person();
    }

    @Nested
    class AssertPropertyViolationCounts {

        @Test
        void shouldPass_WhenAllExpectedCounts_AreCorrect() {
            var firstNames = inputs("Bob", "Alice", "", " ", null);
            var expectedFirstNameViolations = expectedViolations(0, 0, 1, 1, 1);

            var lastNames = inputs("Smith", "Ng", "Vader", "X", "", " ", null);
            var expectedLastNameViolations = expectedViolations(0, 0, 0, 1, 2, 2, 1);

            testHelper.assertPropertyViolationCounts(
                    "firstName", firstNames, expectedFirstNameViolations, person, person::setFirstName);
            testHelper.assertPropertyViolationCounts(
                    "lastName", lastNames, expectedLastNameViolations, person, person::setLastName);

            assertNoSoftAssertionFailures();
        }

        @Test
        void shouldThrow_WhenSomeExpectedCounts_AreNotCorrect() {
            var lastNames = inputs("Smith", "X", "", " ", null);
            var expectedViolations = expectedViolations(0, 2, 0, 1, 0);
            testHelper.assertPropertyViolationCounts(
                    "lastName", lastNames, expectedViolations, person, person::setLastName);

            var thrown = assertSoftAssertionsFailures();
            var errors = extractErrorMessageFrom(thrown);

            assertThat(errors).hasSize(4);

            assertThat(first(errors)).contains("input: [X]")
                    .contains("Expected size: 2 but was: 1")
                    .contains("propertyPath=lastName")
                    .contains("length must be between 2 and 100");

            assertThat(second(errors)).contains("input: []")
                    .contains("Expected size: 0 but was: 2")
                    .contains("propertyPath=lastName")
                    .contains("must not be blank")
                    .contains("length must be between 2 and 100");

            assertThat(third(errors)).contains("input: [ ]")
                    .contains("Expected size: 1 but was: 2")
                    .contains("propertyPath=lastName")
                    .contains("must not be blank")
                    .contains("length must be between 2 and 100");

            assertThat(fourth(errors)).contains("input: [null]")
                    .contains("Expected size: 0 but was: 1")
                    .contains("propertyPath=lastName")
                    .contains("must not be blank");
        }
    }

    @Nested
    class AssertPropertyViolationMessages {

        @Test
        void shouldPass_WhenAllExpectedMessages_AreCorrect() {
            var firstNames = inputs("Bob", "Alice", "", " ", null);
            var expectedFirstNameMessages = expectedMessagesLists(
                    noExpectedMessages(),
                    noExpectedMessages(),
                    expectedMessages("must not be blank"),
                    expectedMessages("must not be blank"),
                    expectedMessages("must not be blank")
            );

            var lastNames = inputs("Smith", "Ng", "X", "Vader", "");
            var expectedLastNameMessages = expectedMessagesLists(
                    noExpectedMessages(),
                    noExpectedMessages(),
                    expectedMessages("length must be between 2 and 100"),
                    noExpectedMessages(),
                    expectedMessages("must not be blank", "length must be between 2 and 100")
            );

            testHelper.assertPropertyViolationMessages(
                    "firstName", firstNames, expectedFirstNameMessages, person, person::setFirstName);
            testHelper.assertPropertyViolationMessages(
                    "lastName", lastNames, expectedLastNameMessages, person, person::setLastName);

            assertNoSoftAssertionFailures();
        }

        @Test
        void shouldThrow_WhenSomeExpectedMessages_AreNotCorrect() {
            var lastNames = inputs("Smith", "Ng", "X", "Vader", "");
            var expectedMessages = expectedMessagesLists(
                    expectedMessages("must not be blank"),  // incorrect
                    noExpectedMessages(),  // correct
                    expectedMessages("length must be between 2 and 100"),  // correct
                    noExpectedMessages(), // correct
                    expectedMessages("length must be between 2 and 100")  // incorrect: missing "must not be blank"
            );

            testHelper.assertPropertyViolationMessages(
                    "lastName", lastNames, expectedMessages, person, person::setLastName);

            var thrown = assertSoftAssertionsFailures();
            var errors = extractErrorMessageFrom(thrown);

            assertThat(errors).hasSize(2);

            assertThat(first(errors))
                    .contains("input: [Smith]")
                    .contains("Expecting ArrayList:")
                    .contains("[]")
                    .contains("to contain only")
                    .contains("[\"must not be blank\"]")
                    .contains("but could not find the following element(s):")
                    .contains("[\"must not be blank\"]");

            // NOTE:
            // We are omitting assertion on the value below, since the order is not deterministic, i.e., sometimes
            // it will be:
            //   length must be between 2 and 100, must not be blank
            //
            // and other times it will be:
            //   must not be blank, length must be between 2 and 100
            //
            assertThat(second(errors))
                    .contains("input: []")
                    .contains("Expecting ArrayList:")
                    // (see note above on omitting value assertion)
                    .contains("to contain only")
                    .contains("[\"length must be between 2 and 100\"]")
                    .contains("but the following element(s) were unexpected:")
                    .contains("[\"must not be blank\"]");
        }
    }

    @Test
    void shouldAssertStringPropertyCannotBeBlank() {
        testHelper.assertStringPropertyCannotBeBlank("firstName", person, person::setFirstName);
        assertNoSoftAssertionFailures();
    }

    @Test
    void shouldAssertStringPropertyMustBeNull() {
        testHelper.assertStringPropertyMustBeNull(
                "accomplishments", person, person::setAccomplishments, validationGroups(NewPerson.class));
        assertNoSoftAssertionFailures();
    }

    @Test
    void shouldAssertStringPropertyHasNoViolations() {
        testHelper.assertStringPropertyHasNoViolations(
                "otherDetails", person, person::setOtherDetails);
        assertNoSoftAssertionFailures();
    }

    @Test
    void shouldAssertInstantPropertyCannotBeNull() {
        testHelper.assertInstantPropertyCannotBeNull(
                "createdAtInstant", person, person::setCreatedAtInstant, validationGroups(ExistingPerson.class));
        testHelper.assertInstantPropertyCannotBeNull(
                "updatedAtInstant", person, person::setUpdatedAtInstant, validationGroups(ExistingPerson.class));
        assertNoSoftAssertionFailures();
    }

    @Test
    void shouldAssertInstantPropertyMustBeNull() {
        testHelper.assertInstantPropertyMustBeNull(
                "createdAtInstant", person, person::setCreatedAtInstant, validationGroups(NewPerson.class));
        testHelper.assertInstantPropertyMustBeNull(
                "updatedAtInstant", person, person::setUpdatedAtInstant, validationGroups(NewPerson.class));
        assertNoSoftAssertionFailures();
    }

    @Test
    void shouldAssertZonedDateTimePropertyCannotBeNull() {
        testHelper.assertZonedDateTimePropertyCannotBeNull(
                "createdAt", person, person::setCreatedAt, validationGroups(ExistingPerson.class));
        testHelper.assertZonedDateTimePropertyCannotBeNull(
                "updatedAt", person, person::setUpdatedAt, validationGroups(ExistingPerson.class));
        assertNoSoftAssertionFailures();
    }

    @Test
    void shouldAssertZonedDateTimePropertyMustBeNull() {
        testHelper.assertZonedDateTimePropertyMustBeNull(
                "createdAt", person, person::setCreatedAt, validationGroups(NewPerson.class));
        testHelper.assertZonedDateTimePropertyMustBeNull(
                "updatedAt", person, person::setUpdatedAt, validationGroups(NewPerson.class));
        assertNoSoftAssertionFailures();
    }

    @Test
    void shouldAssertLongPropertyCannotBeNull() {
        testHelper.assertLongPropertyCannotBeNull(
                "id", person, person::setId, validationGroups(ExistingPerson.class));
        assertNoSoftAssertionFailures();
    }

    @Test
    void shouldAssertLongPropertyMustBeNull() {
        testHelper.assertLongPropertyMustBeNull(
                "id", person, person::setId, validationGroups(NewPerson.class));
        assertNoSoftAssertionFailures();
    }

    @Test
    void shouldAssertIntegerPropertyCannotBeNull() {
        testHelper.assertIntegerPropertyCannotBeNull(
                "achievementLevel", person, person::setAchievementLevel, validationGroups(ExistingPerson.class));
        assertNoSoftAssertionFailures();
    }

    @Test
    void shouldAssertIntegerPropertyMustBeNull() {
        testHelper.assertIntegerPropertyMustBeNull(
                "achievementLevel", person, person::setAchievementLevel, validationGroups(NewPerson.class));
        assertNoSoftAssertionFailures();
    }

    @Test
    void shouldAssertEnumPropertyCannotBeNull() {
        testHelper.assertEnumPropertyCannotBeNull(
                "favoriteColor", person, FavoriteColor.class, person::setFavoriteColor);
        assertNoSoftAssertionFailures();
    }

    private void assertNoSoftAssertionFailures() {
        assertThatCode(() -> softly.assertAll()).doesNotThrowAnyException();
    }

    private Throwable assertSoftAssertionsFailures() {
        var thrown = catchThrowable(softly::assertAll);
        assertThat(thrown)
                .describedAs("Expected Throwable to be an AssertJMultipleFailuresError")
                .isExactlyInstanceOf(AssertJMultipleFailuresError.class);
        return thrown;
    }

    private static List<String> extractErrorMessageFrom(Throwable thrown) {
        return ((AssertJMultipleFailuresError) thrown).getFailures().stream()
                .map(Throwable::getMessage)
                .toList();
    }

    @Data
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor
    private static class Person {

        @Null(groups = NewPerson.class)
        @NotNull(groups = ExistingPerson.class)
        private Long id;

        @Null(groups = NewPerson.class)
        @NotNull(groups = ExistingPerson.class)
        private ZonedDateTime createdAt;

        @Null(groups = NewPerson.class)
        @NotNull(groups = ExistingPerson.class)
        private ZonedDateTime updatedAt;

        @Null(groups = NewPerson.class)
        @NotNull(groups = ExistingPerson.class)
        private Instant createdAtInstant;

        @Null(groups = NewPerson.class)
        @NotNull(groups = ExistingPerson.class)
        private Instant updatedAtInstant;

        @NotBlank
        private String firstName;

        @NotBlank
        @Length(min = 2, max = 100)
        private String lastName;

        // I guess new people aren't allowed to have accomplishments yet...
        @Null(groups = NewPerson.class)
        private String accomplishments;

        private String otherDetails;

        @Past
        private Date birthDate;

        @Null(groups = NewPerson.class)
        @NotNull(groups = ExistingPerson.class)
        private Integer achievementLevel;

        @NotNull
        private FavoriteColor favoriteColor;
    }

    private enum FavoriteColor {
        RED, GREEN, BLUE
    }

    private interface NewPerson {
    }

    private interface ExistingPerson {
    }
}
