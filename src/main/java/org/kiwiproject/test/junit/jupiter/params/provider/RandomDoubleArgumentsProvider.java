package org.kiwiproject.test.junit.jupiter.params.provider;

import static com.google.common.base.Preconditions.checkArgument;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * An {@link ArgumentsProvider} that provides random double values. Accepts a {@link RandomDoubleSource} to
 * allow customization of the provided values.
 *
 * @see RandomDoubleSource
 */
class RandomDoubleArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<RandomDoubleSource> {

    private RandomDoubleSource randomDoubleSource;

    @Override
    public void accept(RandomDoubleSource randomDoubleSource) {
        this.randomDoubleSource = randomDoubleSource;
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        checkArgument(randomDoubleSource.min() <= randomDoubleSource.max(), "min must be equal or less than max");

        var random = ThreadLocalRandom.current();
        var originInclusive = randomDoubleSource.min();
        var boundExclusive = calculateBound();
        var count = randomDoubleSource.count();

        return Stream.generate(() -> random.nextDouble(originInclusive, boundExclusive))
                .map(Arguments::of)
                .limit(count);
    }

    /**
     * @implNote In order to get a random double up to and including the max, we have to pick an arbitrarily
     * small number above the max so that {@link ThreadLocalRandom#nextDouble(double, double)} will at least
     * come pretty close to the max, even if it never generates exactly the max.
     */
    private double calculateBound() {
        if (randomDoubleSource.max() == Double.MAX_VALUE) {
            return Double.MAX_VALUE;
        }

        return randomDoubleSource.max() + 1.0E-12;
    }
}
