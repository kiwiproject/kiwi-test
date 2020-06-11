package org.kiwiproject.test.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.experimental.UtilityClass;

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
}
