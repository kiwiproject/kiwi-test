package org.kiwiproject.test.junit.jupiter.params.provider;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;

import java.lang.annotation.Annotation;
import java.util.Arrays;

@DisplayName("RandomIntArgumentsProvider")
class RandomIntArgumentsProviderTest {

    @Test
    void shouldProduceTheExpectedNumberOfValues() {
        var randomIntSource = new RandomIntSource() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return RandomIntSource.class;
            }

            @Override
            public int min() {
                return 2;
            }

            @Override
            public int max() {
                return 6;
            }

            @Override
            public int count() {
                return 100;
            }
        };

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
    @RandomIntSource(min = 2147483647, count = 5)
    void shouldProvideRandomIntegersWithUpperBoundAsMaxInteger(int value) {
        assertThat(value).isEqualTo(Integer.MAX_VALUE);
    }

    @ParameterizedTest
    @RandomIntSource(max = -2147483648, count = 5)
    void shouldProvideRandomIntegersWithBoundsAsMinInteger(int value) {
        assertThat(value).isEqualTo(Integer.MIN_VALUE);
    }
}
