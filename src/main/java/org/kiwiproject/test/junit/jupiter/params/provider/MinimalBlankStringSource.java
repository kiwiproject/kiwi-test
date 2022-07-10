package org.kiwiproject.test.junit.jupiter.params.provider;

import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @MinimalBlankStringSource} is an {@link ArgumentsSource} which provides a few blank strings including
 * null, zero-length string, strings with only spaces, and strings with only newline, carriage return, and tab.
 *
 * <p>
 * Usage:
 * <pre>
 * {@literal @}ParameterizedTest
 * {@literal @}MinimalBlankStringSource
 *  void testThatEachProvidedArgumentIsBlank(String blankString) {
 *      assertThat(blankString).isBlank();
 *      // or whatever else you need to test where you need a blank String...
 *  }
 * </pre>
 *
 * @see BlankStringSource
 * @see AsciiOnlyBlankStringSource
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(MinimalBlankStringArgumentsProvider.class)
public @interface MinimalBlankStringSource {
}
