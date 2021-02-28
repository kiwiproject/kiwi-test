package org.kiwiproject.test.junit.jupiter.params.provider;

import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @RandomLongSource} is an {@link ArgumentsSource} that provides a limited number of random {@code long} values
 * for use in parameterized tests.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(RandomLongArgumentsProvider.class)
public @interface RandomLongSource {

    /**
     * The minimum value this source will produce.
     * <p>
     * Defaults to {@link Long#MIN_VALUE}.
     */
    long min() default Long.MIN_VALUE;

    /**
     * The maximum value this source will produce.
     * <p>
     * Defaults to {@link Long#MAX_VALUE}.
     *
     * @implNote When set to {@link Long#MAX_VALUE}, the maximum is actually {@code (Long.MAX_VALUE - 1)} since
     * {@link java.util.concurrent.ThreadLocalRandom#nextLong(long, long) nextLong} has an exclusive upper bound, and
     * without resorting to using {@link java.math.BigInteger} we cannot ever get to {@link Long#MAX_VALUE}. We assume
     * that if you really need the maximum value in a test, you can just write a test for that specific case.
     */
    long max() default Long.MAX_VALUE;

    /**
     * The number of random longs to produce.
     * <p>
     * Defaults to 25.
     */
    int count() default 25;
}
