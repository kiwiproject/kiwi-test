package org.kiwiproject.test.junit.jupiter.params.provider;

import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @RandomDoublesSource} is an {@link ArgumentsSource} that provides a limited number of random {@code double}
 * values for use in parameterized tests.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(RandomDoubleArgumentsProvider.class)
public @interface RandomDoubleSource {

    /**
     * The minimum value this source will produce.
     * <p>
     * Defaults to {@link Double#MIN_VALUE}.
     *
     * @return the minimum value
     */
    double min() default Double.MIN_VALUE;

    /**
     * The maximum value this source will produce.
     * <p>
     * Defaults to {@link Double#MAX_VALUE}.
     *
     * @return the maximum value
     * @implNote When set to {@link Double#MAX_VALUE}, the maximum is actually {@code (Double.MAX_VALUE - 1)} since
     * {@link java.util.concurrent.ThreadLocalRandom#nextDouble(double, double) nextDouble} has an exclusive upper
     * bound, and without resorting to using {@link java.math.BigDecimal} we can't ever get to {@link Double#MAX_VALUE}.
     * We assume that if you really need the maximum value in a test, you can just write a test for that specific case.
     */
    double max() default Double.MAX_VALUE;

    /**
     * The number of random doubles to produce.
     * <p>
     * Defaults to 25.
     *
     * @return the number of doubles to produce
     */
    int count() default 25;

}
