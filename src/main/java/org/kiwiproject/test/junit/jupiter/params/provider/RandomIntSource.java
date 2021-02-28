package org.kiwiproject.test.junit.jupiter.params.provider;

import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @RandomIntSource} is an {@link ArgumentsSource} that provides a limited number of random {@code int} values
 * for use in parameterized tests.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(RandomIntArgumentsProvider.class)
public @interface RandomIntSource {

    /**
     * The minimum value this source will produce.
     * <p>
     * Defaults to {@link Integer#MIN_VALUE}.
     */
    int min() default Integer.MIN_VALUE;

    /**
     * The maximum value this source will produce.
     * <p>
     * Defaults to {@link Integer#MAX_VALUE}.
     */
    int max() default Integer.MAX_VALUE;

    /**
     * The number of random integers to produce.
     * <p>
     * Defaults to 25.
     */
    int count() default 25;
}
