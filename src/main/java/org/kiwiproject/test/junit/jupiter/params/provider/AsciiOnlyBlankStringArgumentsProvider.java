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
 * {@link org.junit.jupiter.params.ParameterizedTest ParameterizedTest}. This will generate a {@code null} String
 * argument, an empty String argument, a String with multiple spaces, and a String with whitespace characters.
 * <p>
 * <strong>However</strong>, unlike {@link BlankStringArgumentsProvider}, this will only use characters that fall
 * in the ASCII character set. Unless it is absolutely necessary to test against ASCII only whitespace (for example, to
 * validate using the Bean Validation API's {@link jakarta.validation.constraints.NotBlank NotBlank} annotation), it is
 * recommended you use {@link BlankStringArgumentsProvider} instead.
 * <p>
 * Usage:
 * <pre>
 * {@literal @}ParameterizedTest
 * {@literal @}ArgumentsSource(AsciiOnlyBlankStringArgumentsProvider.class)
 *  void testThatEachProvidedArgumentIsBlank(String blankString) {
 *      assertThat(blankString).isBlank();
 *      // or whatever else you need to test where you need a blank String...
 *  }
 * </pre>
 *
 * @see AsciiOnlyBlankStringSource
 * @see BlankStringSource
 * @see BlankStringArgumentsProvider
 */
public class AsciiOnlyBlankStringArgumentsProvider implements ArgumentsProvider {

    /**
     * A bunch of null, empty, or blank ASCII Strings.
     */
    public static final List<String> ASCII_ONLY_BLANK_STRINGS = unmodifiableList(Arrays.asList(
            // null, empty, blank space-only
            null,
            "",
            " ",
            "    ",

            // ASCII characters
            "\t", // HORIZONTAL TABULATION (U+0009)
            "\n", // LINE FEED (U+000A)
            "\u000B", // VERTICAL TABULATION
            "\f", // FORM FEED (U+000C)
            "\r", // CARRIAGE RETURN (U+000D)
            "\u001C", // FILE SEPARATOR
            "\u001D", // GROUP SEPARATOR
            "\u001E", // RECORD SEPARATOR
            "\u001F" // UNIT SEPARATOR
    ));

    /**
     * Provides various blank arguments for {@link org.junit.jupiter.params.ParameterizedTest ParameterizedTest} tests.
     *
     * @param context the extension context
     * @return a {@link Stream} of blank String {@link Arguments}
     * @implNote The stream must be instantiated for each test. If you try to extract these fields out to a constant,
     * any test class that uses this {@link ArgumentsProvider} more than once will fail due to the stream being
     * exhausted. So don't do that.
     */
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return ASCII_ONLY_BLANK_STRINGS.stream().map(Arguments::of);
    }
}
