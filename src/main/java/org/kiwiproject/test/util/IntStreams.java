package org.kiwiproject.test.util;

import lombok.experimental.UtilityClass;
import org.kiwiproject.base.KiwiDeprecated;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Static utilities related to {@link IntStream}.
 *
 * @deprecated This has moved into kiwi as of 0.23.0 and will be removed in 0.20.0
 */
@UtilityClass
@Deprecated(since = "0.19.0", forRemoval = true)
@KiwiDeprecated(since = "0.19.0", removeAt = "0.20.0")
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
