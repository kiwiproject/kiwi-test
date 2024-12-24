package org.kiwiproject.test.junit.jupiter;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicator that a test is "clear box", generally used to indicate a test is calling a non-public API and/or that
 * internal structures or logic are being tested.
 * <p>
 * This is often useful when testing complex internal logic or exceptions that are challenging or near impossible
 * to simulate. One downside is that clear box tests are more tightly coupled due to the test's knowledge of the
 * code being tested, and are therefore more brittle.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Test
public @interface ClearBoxTest {

    /**
     * Optional description or explanation why this is a "clear box" test.
     *
     * @return the description (empty string by default)
     */
    String value() default "";
}
