package org.kiwiproject.test.junit.jupiter.params.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

@DisplayName("RandomDoubleArgumentsProvider")
@Slf4j
class RandomDoubleArgumentsProviderTest {

    @Test
    void shouldThrowIllegalArgumentException_WhenMaxLessThanMin() {
        var randomDoubleSource = newRandomDoubleSource(10.0, 9.9, 25);
        var provider = new RandomDoubleArgumentsProvider();
        provider.accept(randomDoubleSource);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider.provideArguments(null, null))
                .withMessage("min must be equal or less than max");
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 20, 42, 100, 500})
    void shouldProduceTheExpectedNumberOfValues(int count) {
        var min = ThreadLocalRandom.current().nextDouble(10.0);
        var max = ThreadLocalRandom.current().nextDouble(20.0, 40.0);
        var randomDoubleSource = newRandomDoubleSource(min, max, count);

        var provider = new RandomDoubleArgumentsProvider();
        provider.accept(randomDoubleSource);

        var arguments = provider.provideArguments(null, null)
                .map(Arguments::get)
                .flatMap(Arrays::stream)
                .mapToDouble(value -> (double) value)
                .toArray();

        assertThat(arguments).hasSize(count);
    }

    /**
     * This test creates a provider that generates 1,000 doubles between a small range and repeats the test
     * 10 times, to check that we don't generate numbers outside the bounds with the small offset we add to the max.
     */
    @RepeatedTest(10)
    void shouldProvideArgumentsWithinCustomBounds() {
        var min = 30.0;
        var max = 31.0;
        var count = 1_000;
        var randomDoubleSource = newRandomDoubleSource(min, max, count);

        var provider = new RandomDoubleArgumentsProvider();
        provider.accept(randomDoubleSource);

        var arguments = provider.provideArguments(null, null)
                .map(Arguments::get)
                .flatMap(Arrays::stream)
                .mapToDouble(value -> (double) value)
                .toArray();

        assertThat(arguments).hasSize(count);

        var minGenerated = Arrays.stream(arguments).min().orElseThrow();
        var maxGenerated = Arrays.stream(arguments).max().orElseThrow();
        LOG.debug("minGenerated: {} , maxGenerated: {}", minGenerated, maxGenerated);

        assertThat(minGenerated).isBetween(min, max);
        assertThat(maxGenerated).isBetween(min, max);
    }

    @ParameterizedTest
    @RandomDoubleSource
    void shouldProvideRandomDoublesWithDefaultAnnotationValues(double value) {
        assertThat(value).isBetween(Double.MIN_VALUE, Double.MAX_VALUE);
    }

    @ParameterizedTest
    @RandomDoubleSource(min = 90.0, max = 100.0, count = 100)
    void shouldProvideRandomDoublesWithCustomBounds(double value) {
        assertThat(value).isBetween(90.0, 100.0);
    }

    private static RandomDoubleSource newRandomDoubleSource(double min, double max, int count) {
        return new RandomDoubleSource() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return RandomDoubleSource.class;
            }

            @Override
            public double min() {
                return min;
            }

            @Override
            public double max() {
                return max;
            }

            @Override
            public int count() {
                return count;
            }
        };
    }
}
