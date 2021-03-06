package org.kiwiproject.test.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.kiwiproject.test.validation.ValidationTestHelper.assertHasViolations;
import static org.kiwiproject.test.validation.ValidationTestHelper.assertNoPropertyViolations;
import static org.kiwiproject.test.validation.ValidationTestHelper.assertNoViolations;
import static org.kiwiproject.test.validation.ValidationTestHelper.assertOnePropertyViolation;
import static org.kiwiproject.test.validation.ValidationTestHelper.assertPropertyViolations;
import static org.kiwiproject.test.validation.ValidationTestHelper.assertViolations;
import static org.kiwiproject.test.validation.ValidationTestHelper.getValidator;
import static org.kiwiproject.test.validation.ValidationTestHelper.newValidator;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.assertj.core.error.AssertJMultipleFailuresError;
import org.hibernate.validator.constraints.Length;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.groups.Default;
import java.util.function.Consumer;

@DisplayName("ValidationTestHelper")
@ExtendWith(SoftAssertionsExtension.class)
class ValidationTestHelperTest {

    @Test
    void shouldReturnSingletonValidator() {
        var validator1 = getValidator();
        var validator2 = getValidator();
        var validator3 = getValidator();

        assertThat(validator1)
                .isNotNull()
                .isSameAs(validator2)
                .isSameAs(validator3);
    }

    @Test
    void shouldBuildNewValidators() {
        var validator1 = newValidator();
        var validator2 = newValidator();

        assertThat(validator1)
                .isNotNull()
                .isNotSameAs(validator2);
    }

    @Nested
    class AssertNoViolations {

        @Test
        void shouldPass_WhenNoViolations() {
            var alice = new Person("Alice", "Mayberry", 32, null);

            assertNoViolations(alice);
        }

        @Test
        void shouldThrow_WhenViolationsExist() {
            var jonesy = new Person(null, "Jones", 27, null);

            assertThatThrownBy(() -> assertNoViolations(jonesy))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        void shouldPass_UsingSoftAssertions_WhenNoViolations(SoftAssertions softly) {
            var fred = new Person("Fred", "Smith", 25, "Freddy");

            assertNoViolations(softly, fred);
        }

        @Test
        void shouldThrow_UsingSoftAssertions_WhenViolationsExist() {
            var jonesy = new Person(null, "Jones", 27, null);

            assertSoftAssertionFailure(softly -> assertNoViolations(softly, jonesy));
        }
    }

    @Nested
    class AssertHasViolations {

        @Test
        void shouldPass_WhenAnyViolationsExist() {
            var alice = new Person("Bob", "Sackamano", 142, null);

            assertHasViolations(alice);
        }

        @Test
        void shouldThrow_WhenNoViolationsExist() {
            var alice = new Person("Alice", "Smith", 42, null);

            assertThatThrownBy(() -> assertHasViolations(alice))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        void shouldPass_UsingSoftAssertions_WhenAnyViolationsExist(SoftAssertions softly) {
            var george = new Person("George", "", 43, null);

            assertHasViolations(softly, george);
        }

        @Test
        void shouldThrow_UsingSoftAssertions_WhenNoViolationsExist() {
            var jan = new Person("Janice", "Kramer", 35, "Jan");

            assertSoftAssertionFailure(softly -> assertHasViolations(softly, jan));
        }
    }

    @Nested
    class AssertViolations {

        @Test
        void shouldPass_WhenViolationsExist() {
            var alice = new Person("Alice", "Mayberry", 132, null);

            assertViolations(alice, 1);
        }

        @Test
        void shouldThrow_WhenNoViolationsExist() {
            var alice = new Person("Alice", "Mayberry", 32, null);

            assertThatThrownBy(() -> assertViolations(alice, 1))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        void shouldPass_UsingSoftAssertions_WhenViolationsExist(SoftAssertions softly) {
            var bob = new Person("Bob", null, 25, "Bobby");

            assertViolations(softly, bob, 1);
        }

        @Test
        void shouldThrow_UsingSoftAssertions_WhenNoViolationsExist() {
            var alice = new Person("Alice", "Mayberry", 32, null);

            assertSoftAssertionFailure(softly ->
                    assertViolations(softly, alice, 1));
        }
    }

    @Nested
    class AssertOnePropertyViolation {

        @Test
        void shouldPass_WhenExactlyOneViolationExists() {
            var missy = new Person("", "Smith", 45, null);

            assertOnePropertyViolation(missy, "firstName");
        }

        @Test
        void shouldThrow_WhenNoViolationsExist() {
            var carlos = new Person("Carlos", "Smith", 45, null);

            assertThatThrownBy(() -> assertOnePropertyViolation(carlos, "lastName"))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        void shouldThrow_WhenMoreThanOneViolationExists() {
            var carlos = new Person("Carlos", "", 45, null);

            assertThatThrownBy(() -> assertOnePropertyViolation(carlos, "lastName"))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        void shouldPass_UsingSoftAssertions_WhenExactlyOneViolationExists(SoftAssertions softly) {
            var carlos = new Person("", "Smith", 45, null);

            assertOnePropertyViolation(softly, carlos, "firstName");
        }

        @Test
        void shouldThrow_UsingSoftAssertions_WhenViolationsExist() {
            var carlos = new Person("Carlos", "", 45, null);

            assertSoftAssertionFailure(softly ->
                    assertOnePropertyViolation(softly, carlos, "lastName"));
        }
    }

    @Nested
    class AssertNoPropertyViolations {

        @Test
        void shouldPass_WhenNoPropertyViolationsExist() {
            var diane = new Person("Diane", "McFerry", 28, "Dee");

            assertNoPropertyViolations(diane, "firstName");
            assertNoPropertyViolations(diane, "lastName");
            assertNoPropertyViolations(diane, "age");
            assertNoPropertyViolations(diane, "nickname");
        }

        @Test
        void shouldThrow_WhenPropertyViolationsExist() {
            var jeff = new Person("Jeff", null, 10, null);

            assertThatThrownBy(() -> assertNoPropertyViolations(jeff, "lastName"))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        void shouldPass_UsingSoftAssertions_WhenNoPropertyViolationsExist(SoftAssertions softly) {
            var tommy = new Person("Tom", "Stitches", 12, "Tommy");

            assertNoPropertyViolations(softly, tommy, "firstName");
            assertNoPropertyViolations(softly, tommy, "lastName");
            assertNoPropertyViolations(softly, tommy, "age");
            assertNoPropertyViolations(softly, tommy, "nickname");
        }

        @Test
        void shouldThrow_UsingSoftAssertions_WhenPropertyViolationsExist() {
            var jeff = new Person("Jeff", null, 10, null);

            assertSoftAssertionFailure(softly ->
                    assertNoPropertyViolations(softly, jeff, "lastName"));
        }
    }

    @Nested
    class AssertPropertyViolationsByCount {

        @Test
        void shouldPass_WhenActualViolationCount_EqualsExpectedCount() {
            var alice = new Person("", "", 26, null);

            assertPropertyViolations(alice, "firstName", 1);
            assertPropertyViolations(alice, "lastName", 2);
            assertPropertyViolations(alice, "age", 0);
        }

        @Test
        void shouldThrow_WhenActualViolationCount_DoesNotEqualExpectedCount() {
            var alice = new Person("Alice", "Smith", 22, null);

            assertThatThrownBy(() -> assertPropertyViolations(alice, "firstName", 1))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        void shouldPass_UsingSoftAssertions_WhenActualViolationCount_EqualsExpectedCount(SoftAssertions softly) {
            var alice = new Person("", "", 26, null);

            assertPropertyViolations(softly, alice, "firstName", 1);
            assertPropertyViolations(softly, alice, "lastName", 2);
            assertPropertyViolations(softly, alice, "age", 0);
        }

        @Test
        void shouldThrow_UsingSoftAssertions_WhenActualViolationCount_DoesNotEqualExpectedCount() {
            var alice = new Person("Alice", "Smith", 22, null);

            assertSoftAssertionFailure(softly ->
                    assertPropertyViolations(softly, alice, "lastName", 1));
        }
    }

    /**
     * @implNote We need to catch {@link AssertionError} since JUnit, OpenTest4J, and AssertJ all throw subclasses
     * of the standard Java {@link AssertionError}.
     */
    private static void assertSoftAssertionFailure(Consumer<SoftAssertions> consumer) {
        try {
            SoftAssertions.assertSoftly(consumer);
            fail("Should have thrown an exception...");
        } catch (AssertionError error) {
            assertThat(error).isInstanceOf(AssertJMultipleFailuresError.class);
        }
    }

    @Nested
    class AssertPropertyViolationsByMessage {

        @Test
        void shouldMatchExpectedMessages() {
            var alice = Person.builder().age(135).build();

            assertPropertyViolations(alice, "firstName", "must not be blank");
            assertPropertyViolations(alice, "lastName", "must not be blank");
            assertPropertyViolations(alice, "age", "must be less than or equal to 130");
            assertPropertyViolations(alice, "nickname");  // there are no errors
        }

        @Test
        void shouldIgnoreErrorMessageOrder() {
            var alice = Person.builder().lastName("").build();

            var emptyError = "must not be blank";
            var lengthError = "length must be between 2 and 255";

            assertPropertyViolations(alice, "lastName", emptyError, lengthError);
            assertPropertyViolations(alice, "lastName", lengthError, emptyError);
        }

        @Test
        void shouldThrowAssertionError_WhenThereAreNoActualMessages() {
            var alice = Person.builder().age(125).build();

            assertThatThrownBy(() ->
                    assertPropertyViolations(alice, "age", "must be less than or equal to 130"))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        void shouldWorkWithAssertJSoftAssertions(SoftAssertions softly) {
            var alice = Person.builder().age(135).build();

            assertPropertyViolations(softly, alice, "firstName", "must not be blank");
            assertPropertyViolations(softly, alice, "lastName", "must not be blank");
            assertPropertyViolations(softly, alice, "age", "must be less than or equal to 130");
            assertPropertyViolations(softly, alice, "nickname");
        }

        @Test
        void shouldIgnoreMessageOrder_WhenUsingSoftAssertions(SoftAssertions softly) {
            var alice = Person.builder().lastName("").build();

            var emptyError = "must not be blank";
            var lengthError = "length must be between 2 and 255";

            assertPropertyViolations(softly, alice, "lastName", emptyError, lengthError);
            assertPropertyViolations(softly, alice, "lastName", lengthError, emptyError);
        }

        /**
         * @implNote This test ensures that we get a soft assertion error if an expected message is not present in
         * the actual messages. It catches {@link AssertionError} since JUnit, OpenTest4J, and AssertJ all throw
         * subclasses of {@link AssertionError}.
         */
        @Test
        void shouldThrowAssertJMultipleFailure_WhenAssertingViolationThatDoesNotExist() {
            var alice = Person.builder().age(125).build();

            try {
                SoftAssertions.assertSoftly(softly ->
                        assertPropertyViolations(softly, alice, "age", "must be less than or equal to 130"));
                fail("Should have thrown an exception...");
            } catch (AssertionError error) {
                assertThat(error)
                        .isInstanceOf(AssertJMultipleFailuresError.class)
                        .hasMessageContaining("Messages [must be less than or equal to 130] not found in actual messages []");
            }
        }
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

    @Nested
    class ValidationWithGroups {

        private User user;

        @BeforeEach
        void setUp() {
            user = User.builder()
                    .userName("alice")
                    .emailAddress("alice@example.org")
                    .build();
        }

        @Test
        void shouldAssertNoViolations() {
            assertNoViolations(user, Default.class, Transient.class);

            assertThatThrownBy(() -> assertNoViolations(user, Default.class, Persistent.class))
                    .isInstanceOf(AssertionError.class);

            var persistentUser = user.withId(42L);
            assertNoViolations(persistentUser, Default.class, Persistent.class);
        }

        @Test
        void shouldAssertNoViolations_UsingSoftAssertions(SoftAssertions softly) {
            assertNoViolations(softly, user, Default.class, Transient.class);

            var persistentUser = user.withId(84L);
            assertNoViolations(softly, persistentUser, Default.class, Persistent.class);
        }

        @Test
        void shouldAssertViolationCount() {
            assertViolations(user, 1, Default.class, Persistent.class);
        }

        @Test
        void shouldAssertViolationCount_UsingSoftAssertions(SoftAssertions softly) {
            assertViolations(softly, user, 1, Default.class, Persistent.class);
        }

        @Test
        void shouldAssertOnePropertyViolation() {
            assertOnePropertyViolation(user, "id", Default.class, Persistent.class);
        }

        @Test
        void shouldAssertOnePropertyViolation_UsingSoftAssertions(SoftAssertions softly) {
            assertOnePropertyViolation(softly, user, "id", Default.class, Persistent.class);
        }

        @Test
        void shouldAssertNoPropertyViolations() {
            assertNoPropertyViolations(user, "id", Default.class, Transient.class);
            assertNoPropertyViolations(user, "userName", Default.class, Transient.class);
            assertNoPropertyViolations(user, "emailAddress", Default.class, Transient.class);

            var persistentUser = user.withId(84L);
            assertNoPropertyViolations(persistentUser, "id", Default.class, Persistent.class);
            assertNoPropertyViolations(persistentUser, "userName", Default.class, Persistent.class);
            assertNoPropertyViolations(persistentUser, "emailAddress", Default.class, Persistent.class);
        }

        @Test
        void shouldAssertNoPropertyViolations_UsingSoftAssertions(SoftAssertions softly) {
            assertNoPropertyViolations(softly, user, "id", Default.class, Transient.class);
            assertNoPropertyViolations(softly, user, "userName", Default.class, Transient.class);
            assertNoPropertyViolations(softly, user, "emailAddress", Default.class, Transient.class);

            var persistentUser = user.withId(84L);
            assertNoPropertyViolations(softly, persistentUser, "id", Default.class, Persistent.class);
            assertNoPropertyViolations(softly, persistentUser, "userName", Default.class, Persistent.class);
            assertNoPropertyViolations(softly, persistentUser, "emailAddress", Default.class, Persistent.class);
        }
    }

    @Builder
    @Value
    private static class User {

        @Null(groups = Transient.class)
        @NotNull(groups = Persistent.class)
        @With
        Long id;

        @NotBlank
        String userName;

        @NotBlank
        String emailAddress;
    }

    private interface Transient {
    }

    private interface Persistent {
    }
}
