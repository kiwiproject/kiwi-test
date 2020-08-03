package org.kiwiproject.test.util;

import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.stream.IntStream;

// TODO: We've only used this in test code. Is it worth moving to kiwi? i.e. is there any non-testing
//  situation this might be useful?

/**
 * Static utilities related to {@link IntStream}.
 */
@UtilityClass
public class IntStreams {

    /**
     * Return an {@link IntStream} whose values are the indices ranging from {@code 0} to {@code values.size() - 1}.
     *
     * @param values the list
     * @param <T>    the list element type
     * @return IntStream of the list indices
     */
    public static <T> IntStream indicesOf(List<T> values) {
        return IntStream.range(0, values.size());
    }
}
