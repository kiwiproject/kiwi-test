package org.kiwiproject.test.assertj;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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

    @Nested
    class AssertThatOnlyOneElementInIterable {

        @Test
        void shouldAcceptLists() {
            var anakin = new Person();
            var people = List.of(anakin);

            KiwiAssertJ.assertThatOnlyElementIn(people).isSameAs(anakin);
        }

        @Test
        void shouldAcceptSets() {
            var kenobi = new Person();
            var people = Set.of(kenobi);

            KiwiAssertJ.assertThatOnlyElementIn(people).isSameAs(kenobi);
        }

        @Test
        void shouldThrow_WhenIterable_HasNoElements() {
            var emptySet = Collections.emptySortedSet();
            assertThatThrownBy(() -> KiwiAssertJ.assertThatOnlyElementIn(emptySet))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("Expected exactly one element");
        }

        @Test
        void shouldThrow_WhenIterable_HasMoreThanOneElement() {
            var people = Set.of(new Person(), new Person());
            assertThatThrownBy(() -> KiwiAssertJ.assertThatOnlyElementIn(people))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("Expected exactly one element");
        }
    }

    @Nested
    class AssertAndGetOnlyElementInIterable {

        @Test
        void shouldAcceptLists() {
            var anakin = new Person();
            var people = List.of(anakin);

            var darth = KiwiAssertJ.assertAndGetOnlyElementIn(people);
            assertThat(darth).isSameAs(anakin);
        }

        @Test
        void shouldAcceptSets() {
            var kenobi = new Person();
            var people = Set.of(kenobi);

            var formerMaster = KiwiAssertJ.assertAndGetOnlyElementIn(people);
            assertThat(formerMaster).isSameAs(kenobi);
        }

        @Test
        void shouldThrow_WhenIterable_HasNoElements() {
            var emptySet = Collections.emptySortedSet();
            assertThatThrownBy(() -> KiwiAssertJ.assertAndGetOnlyElementIn(emptySet))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("Expected exactly one element");
        }

        @Test
        void shouldThrow_WhenIterable_HasMoreThanOneElement() {
            var people = Set.of(new Person(), new Person());
            assertThatThrownBy(() -> KiwiAssertJ.assertAndGetOnlyElementIn(people))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("Expected exactly one element");
        }
    }

    @Nested
    class AssertAndGetOnlyEntryInMap {

        @Test
        void shouldGetOnlyEntry() {
            var map = Map.of("answer", 42);
            var entry = KiwiAssertJ.assertAndGetOnlyEntryIn(map);
            assertThat(entry).isEqualTo(entry("answer", 42));
        }

        @Test
        void shouldThrowWhenMapIsEmpty() {
            var emptyMap = Collections.emptyMap();
            assertThatThrownBy(() -> KiwiAssertJ.assertAndGetOnlyEntryIn(emptyMap))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("Expected exactly one entry");
        }

        @Test
        void shouldThrowWhenMapHasMoreThanOneEntry() {
            var employees = Map.of("bob", new Employee(), "alice", new Employee());
            assertThatThrownBy(() -> KiwiAssertJ.assertAndGetOnlyEntryIn(employees))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("Expected exactly one entry");
        }
    }

    @Nested
    class AssertPresentAndGet {

        @ParameterizedTest
        @MethodSource("org.kiwiproject.test.assertj.KiwiAssertJTest#objectsForOptionals")
        void shouldGetTheContainedValue(Object value) {
            var anOptional = Optional.of(value);
            var containedValue = KiwiAssertJ.assertPresentAndGet(anOptional);
            assertThat(containedValue).isSameAs(value);
        }

        @Test
        void shouldThrowWhenOptionalIsEmpty() {
            var empty = Optional.empty();
            assertThatThrownBy(() -> KiwiAssertJ.assertPresentAndGet(empty))
                    .isInstanceOf(AssertionError.class);
        }
    }

    static Stream<Object> objectsForOptionals() {
        return Stream.of(42, "foo", new Object(), 84L, 24.0, new Person(), new Car());
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