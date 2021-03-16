package org.kiwiproject.test.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.experimental.UtilityClass;
import org.assertj.core.api.ObjectAssert;

import java.util.Map;

/**
 * Some AssertJ test utilities that you might or might find useful.
 */
@UtilityClass
public class KiwiAssertJ {

    /**
     * Assert the given object has <em>exactly</em> the given type.
     * <p>
     * Returns the object cast to an instance of that type if the assertion passes, otherwise fails the test.
     *
     * @param object the object to assert
     * @param type   the expected type
     * @param <T>    the expected type parameter
     * @return the object cast to an instance of T
     */
    public static <T> T assertIsExactType(Object object, Class<T> type) {
        assertThat(object).isExactlyInstanceOf(type);
        return type.cast(object);
    }

    /**
     * Assert the given object is an instance of the given type, i.e. is the same type or a subtype of the given type.ª
     * <p>
     * Returns the object cast to an instance of that type if the assertion passes, otherwise fails the test.
     *
     * @param object the object to assert
     * @param type   the expected type
     * @param <T>    the expected type parameter
     * @return the object cast to an instance of T
     */
    public static <T> T assertIsTypeOrSubtype(Object object, Class<T> type) {
        assertThat(object).isInstanceOf(type);
        return type.cast(object);
    }

    /**
     * Assert the given Iterable contains exactly one element, and returns {@link ObjectAssert} for further
     * AssertJ method chaining.
     * <p>
     * This is basically just a shortcut for {@code assertThat(iterable).hasSize(1).first()} but that seems to come up
     * enough in testing (for me anyway) that I don't want to type it out every time.
     *
     * @param iterable the Iterable
     * @param <T>      the type of element
     * @return an {@link ObjectAssert} for further method chaining
     */
    public static <T> ObjectAssert<T> assertThatOnlyElementIn(Iterable<T> iterable) {
        return assertThat(iterable)
                .describedAs("Expected exactly one element")
                .hasSize(1)
                .first();
    }

    /**
     * Assert the given Iterable contains exactly one element, and return that element for further inspection.
     *
     * @param iterable the Iterable
     * @param <T>      the type of element
     * @return the first element
     */
    public static <T> T assertAndGetOnlyElementIn(Iterable<T> iterable) {
        assertThat(iterable)
                .describedAs("Expected exactly one element")
                .hasSize(1);
        return iterable.iterator().next();
    }

    /**
     * Assert the given Map contains exactly one entry, and return that entry for further inspection.
     *
     * @param map the Map
     * @param <K> the type of keys
     * @param <V> the type of values
     * @return the first entry
     */
    public static <K, V> Map.Entry<K, V> assertAndGetOnlyEntryIn(Map<K, V> map) {
        assertThat(map)
                .describedAs("Expected exactly one entry")
                .hasSize(1);
        return map.entrySet().iterator().next();
    }
}
