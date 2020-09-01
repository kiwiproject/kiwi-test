package org.kiwiproject.test.junit;

import static com.google.common.collect.Lists.newArrayList;

import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * Shared utilities for parameterized tests.
 */
@UtilityClass
public class ParameterizedTests {

    /**
     * Helper method that creates a list of inputs of some type {@code T}, mainly useful for readability in tests.
     *
     * @param values the values to use as inputs in a parameterized test
     * @param <T>    the type of objects
     * @return a mutable list containing the given values
     */
    @SafeVarargs
    public static <T> List<T> inputs(T... values) {
        return newArrayList(values);
    }
}
