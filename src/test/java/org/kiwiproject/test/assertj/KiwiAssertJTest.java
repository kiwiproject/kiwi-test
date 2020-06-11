package org.kiwiproject.test.assertj;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("KiwiAssertJ")
class KiwiAssertJTest {

    @Nested
    class AssertIsExactType {

        @Test
        void shouldBeTrue_WhenIsExactType() {
            Object obj = new Person();
            Person bob = KiwiAssertJ.assertIsExactType(obj, Person.class);

            assertThat(bob).isSameAs(obj);
        }

        @Test
        void shouldBeFalse_WhenIsNotExactType() {
            Object obj = new Employee();

            assertThatThrownBy(() -> KiwiAssertJ.assertIsExactType(obj, Person.class))
                    .isInstanceOf(AssertionError.class);
        }
    }

    @Nested
    class AssertIsTypeOrSubtype {

        @Test
        void shouldBeTrue_WhenIsExactType() {
            Object obj = new Person();
            Person alice = KiwiAssertJ.assertIsTypeOrSubtype(obj, Person.class);

            assertThat(alice).isSameAs(obj);
        }

        @Test
        void shouldBeTrue_WhenIsSubtype() {
            Object obj = new Employee();
            Person bob = KiwiAssertJ.assertIsTypeOrSubtype(obj, Person.class);

            assertThat(bob).isSameAs(obj);
        }

        @Test
        void shouldBeFalse_WhenIsNotTypeOrSubtype() {
            Object plane = new Airplane();

            assertThatThrownBy(() -> KiwiAssertJ.assertIsTypeOrSubtype(plane, Car.class))
                    .isInstanceOf(AssertionError.class);
        }
    }

    private static class Person {
    }

    private static class Employee extends Person {
    }

    private static class Airplane {
    }

    private static class Car {
    }
}