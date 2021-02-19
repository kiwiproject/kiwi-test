package org.kiwiproject.test.junit.jupiter.params.provider;

import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @AsciiOnlyBlankStringSource} is an {@link ArgumentsSource} which provides ASCII blank strings.
 * <p>
 * Usage:
 * <pre>
 * {@literal @}ParameterizedTest
 * {@literal @}AsciiOnlyBlankStringSource
 *  void testThatEachProvidedArgumentIsBlank(String blankString) {
 *      assertThat(blankString).isBlank();
 *      // or whatever else you need to test where you need a blank String...
 *  }
 * </pre>
 *
 * @see BlankStringSource
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(AsciiOnlyBlankStringArgumentsProvider.class)
public @interface AsciiOnlyBlankStringSource {
}
