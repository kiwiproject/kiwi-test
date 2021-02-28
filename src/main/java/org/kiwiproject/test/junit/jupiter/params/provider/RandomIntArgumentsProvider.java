package org.kiwiproject.test.junit.jupiter.params.provider;

import static com.google.common.base.Preconditions.checkArgument;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * An {@link ArgumentsProvider} that provides random integer values. Accepts a {@link RandomIntSource} to
 * allow customization of the provided values.
 *
 * @see RandomIntSource
 */
class RandomIntArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<RandomIntSource> {

    private RandomIntSource randomIntSource;

    @Override
    public void accept(RandomIntSource randomIntSource) {
        this.randomIntSource = randomIntSource;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote In order to allow for a max including {@link Integer#MAX_VALUE}, we are generating {@code long}
     * values in the range [min, max + 1] using {@link ThreadLocalRandom#nextLong(long, long)}. The reason is because
     * all the {@code nextXxx(origin, bound)} methods are inclusive of the {@code origin} but <em>exclusive</em>
     * of the {@code bound}, therefore we cannot get to {@link Integer#MAX_VALUE} using
     * {@link ThreadLocalRandom#nextInt(int, int)}.
     */
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        checkArgument(randomIntSource.min() <= randomIntSource.max(), "min must be equal or less than max");

        var random = ThreadLocalRandom.current();
        var originInclusive = randomIntSource.min();
        var boundExclusive = randomIntSource.max() + 1L;
        var count = randomIntSource.count();

        return Stream.generate(() -> random.nextLong(originInclusive, boundExclusive))
                .map(Math::toIntExact)
                .map(Arguments::of)
                .limit(count);
    }
}
