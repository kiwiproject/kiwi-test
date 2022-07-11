package org.kiwiproject.test.junit.jupiter.params.provider;

import static java.util.Collections.unmodifiableList;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Creates an {@link ArgumentsProvider} that can feed a test method with multiple blank {@link String} objects for a
 * {@link org.junit.jupiter.params.ParameterizedTest ParameterizedTest}. This will generate a limited number of blank
 * strings including null and empty strings, and a few whitespace-only strings.
 * <p>
 * Usage:
 * <pre>
 * {@literal @}ParameterizedTest
 * {@literal @}ArgumentsSource(MinimalBlankStringArgumentsProvider.class)
 *  void testThatEachProvidedArgumentIsBlank(String blankString) {
 *      assertThat(blankString).isBlank();
 *      // or whatever else you need to test where you need a blank String...
 *  }
 * </pre>
 * <p>
 * For convenience, we recommend using the {@link MinimalBlankStringSource} annotation on parameterized tests.
 *
 * @see MinimalBlankStringSource
 */
public class MinimalBlankStringArgumentsProvider implements ArgumentsProvider {

    /**
     * A limited number of null, empty, or whitespace-only Strings.
     */
    public static final List<String> BLANK_STRINGS = unmodifiableList(
            Arrays.asList(null, "", " ", "\t", "\n", "\r")
    );

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return BLANK_STRINGS.stream().map(Arguments::of);
    }
}
