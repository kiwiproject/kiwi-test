package org.kiwiproject.test.junit.jupiter.params.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;

import java.lang.annotation.Annotation;
import java.util.Arrays;

@DisplayName("RandomLongArgumentsProvider")
class RandomLongArgumentsProviderTest {

    @Test
    void shouldThrowIllegalArgumentException_WhenMaxLessThanMin() {
        var randomLongSource = newRandomLongSource(5, 4, 25);
        var provider = new RandomLongArgumentsProvider();
        provider.accept(randomLongSource);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider.provideArguments(null))
                .withMessage("min must be equal or less than max");
    }

    @Test
    void shouldProduceTheExpectedNumberOfValues() {
        var randomLongSource = newRandomLongSource(42L, 50L, 100);

        var provider = new RandomLongArgumentsProvider();
        provider.accept(randomLongSource);

        var arguments = provider.provideArguments(null)
                .map(Arguments::get)
                .flatMap(Arrays::stream)
                .mapToLong(value -> (long) value)
                .toArray();

        assertThat(arguments)
                .hasSize(100)
                .containsAnyOf(42, 43, 44, 45, 46, 47, 48, 49, 50);
    }

    @ParameterizedTest
    @RandomLongSource
    void shouldProvideRandomIntegersWithDefaultAnnotationValues(long value) {
        assertThat(value).isBetween(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    @ParameterizedTest
    @RandomLongSource(min = 60, max = 70, count = 75)
    void shouldProvideRandomIntegersWithCustomBounds(long value) {
        assertThat(value).isBetween(60L, 70L);
    }

    /**
     * @implNote The min here must be {@code (Long.MAX_VALUE - 1)} otherwise the min and max bounds will both
     * equal {@code Long.MAX_VALUE} which causes {@link java.util.concurrent.ThreadLocalRandom#nextLong(long, long) nextLong}
     * to throw an IllegalArgumentException.
     */
    @ParameterizedTest
    @RandomLongSource(min = Long.MAX_VALUE - 1, count = 5)
    void shouldProvideRandomLongsWithUpperBoundAsMaxLongMinusOne(long value) {
        assertThat(value).isEqualTo(Long.MAX_VALUE - 1);
    }

    @ParameterizedTest
    @RandomLongSource(max = Long.MIN_VALUE, count = 5)
    void shouldProvideRandomLongsWithLowerBoundAsMinLong(long value) {
        assertThat(value).isEqualTo(Long.MIN_VALUE);
    }

    private static RandomLongSource newRandomLongSource(long min, long max, int count) {
        return new RandomLongSource() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return RandomLongSource.class;
            }

            @Override
            public long min() {
                return min;
            }

            @Override
            public long max() {
                return max;
            }

            @Override
            public int count() {
                return count;
            }
        };
    }
}