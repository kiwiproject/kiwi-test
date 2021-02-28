package org.kiwiproject.test.junit.jupiter.params.provider;

import static com.google.common.base.Preconditions.checkArgument;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * An {@link ArgumentsProvider} that provides random integer values. Accepts a {@link RandomLongSource} to
 * allow customization of the provided values.
 *
 * @see RandomLongSource
 */
class RandomLongArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<RandomLongSource> {

    private RandomLongSource randomLongSource;

    @Override
    public void accept(RandomLongSource randomLongSource) {
        this.randomLongSource = randomLongSource;
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        checkArgument(randomLongSource.min() <= randomLongSource.max(), "min must be equal or less than max");

        var random = ThreadLocalRandom.current();
        var originInclusive = randomLongSource.min();
        var boundExclusive = calculateBound();
        var count = randomLongSource.count();

        return Stream.generate(() -> random.nextLong(originInclusive, boundExclusive))
                .map(Arguments::of)
                .limit(count);
    }

    private long calculateBound() {
        if (randomLongSource.max() == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        return randomLongSource.max() + 1;
    }
}
