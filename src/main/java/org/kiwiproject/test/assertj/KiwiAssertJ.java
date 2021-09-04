package org.kiwiproject.test.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.experimental.UtilityClass;
import org.assertj.core.api.ObjectAssert;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

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

    /**
     * Assert the given {@link Optional} contains a value, and return that value if true. Otherwise, the assertion
     * fails if the Optional does not contain a value.
     * <p>
     * This is useful in situations in which you don't want to use AssertJ's Optional assertion methods. One
     * specific example is when the Optional contains some JSON or XML, and you want to check that the contained
     * value, if present, contains several substrings. Example:
     * <pre>
     * var xml = assertPresentAndGet(xmlOptional);
     * assertThat(xml)
     *         .contains("&lt;name&gt;Alice&lt;/name&gt;")
     *         .contains("&lt;age&gt;42&lt;/age&gt;")
     *         .contains("&lt;state&gt;VA&lt;/state&gt;");
     * </pre>
     * You could also do this using the {@link org.assertj.core.api.AbstractOptionalAssert#hasValueSatisfying(Consumer)}
     * method in AssertJ and putting the same assertions as above within a lambda. However, we usually find the example
     * code shown above to be simpler and easier to read.
     *
     * @param optional the Optional
     * @param <T>      the type that the Optional contains
     * @return the value
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> T assertPresentAndGet(Optional<T> optional) {
        assertThat(optional).isPresent();
        return optional.orElseThrow();
    }
}
