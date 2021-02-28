package org.kiwiproject.test.junit.jupiter.params.provider;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;

import java.lang.annotation.Annotation;
import java.util.Arrays;

@DisplayName("RandomIntArgumentsProvider")
class RandomIntArgumentsProviderTest {

    @Test
    void shouldThrowIllegalArgumentException_WhenMaxLessThanMin() {
        var randomIntSource = newRandomIntSource(5, 4, 25);
        var provider = new RandomIntArgumentsProvider();
        provider.accept(randomIntSource);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider.provideArguments(null))
                .withMessage("min must be equal or less than max");
    }

    @Test
    void shouldProduceTheExpectedNumberOfValues() {
        var randomIntSource = newRandomIntSource(2, 6, 100);

        var provider = new RandomIntArgumentsProvider();
        provider.accept(randomIntSource);

        var arguments = provider.provideArguments(null)
                .map(Arguments::get)
                .flatMap(Arrays::stream)
                .mapToInt(value -> (int) value)
                .toArray();

        assertThat(arguments)
                .hasSize(100)
                .containsAnyOf(2, 3, 4, 5, 6);

        assertThat(stream(arguments).min().orElseThrow()).isGreaterThanOrEqualTo(2);
        assertThat(stream(arguments).max().orElseThrow()).isLessThanOrEqualTo(6);
    }

    @ParameterizedTest
    @RandomIntSource
    void shouldProvideRandomIntegersWithDefaultAnnotationValues(int value) {
        assertThat(value).isBetween(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @ParameterizedTest
    @RandomIntSource(min = 5, max = 10, count = 50)
    void shouldProvideRandomIntegersWithCustomBounds(int value) {
        assertThat(value).isBetween(5, 10);
    }

    @ParameterizedTest
    @RandomIntSource(min = Integer.MAX_VALUE, count = 5)
    void shouldProvideRandomIntegersWithUpperBoundAsMaxInteger(int value) {
        assertThat(value).isEqualTo(Integer.MAX_VALUE);
    }

    @ParameterizedTest
    @RandomIntSource(max = Integer.MIN_VALUE, count = 5)
    void shouldProvideRandomIntegersWithLowerBoundAsMinInteger(int value) {
        assertThat(value).isEqualTo(Integer.MIN_VALUE);
    }

    private RandomIntSource newRandomIntSource(int min, int max, int count) {
        return new RandomIntSource() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return RandomIntSource.class;
            }

            @Override
            public int min() {
                return min;
            }

            @Override
            public int max() {
                return max;
            }

            @Override
            public int count() {
                return count;
            }
        };
    }
}
