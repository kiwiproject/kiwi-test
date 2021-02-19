package org.kiwiproject.test.junit.jupiter.params.provider;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

@DisplayName("AsciiOnlyBlankStringArgumentsProvider")
class AsciiOnlyBlankStringArgumentsProviderTest {

    @ParameterizedTest
    @ArgumentsSource(AsciiOnlyBlankStringArgumentsProvider.class)
    void shouldProvideBlankArguments(String blankString) {
        assertThat(blankString).isBlank();

        if (nonNull(blankString)) {
            assertOnlyAsciiCharactersIn(blankString);
        }
    }

    @ParameterizedTest
    @AsciiOnlyBlankStringSource
    void shouldProvideBlankArgumentsUsingAnnotation(String blankString) {
        assertThat(blankString).isBlank();

        if (nonNull(blankString)) {
            assertOnlyAsciiCharactersIn(blankString);
        }
    }

    private static void assertOnlyAsciiCharactersIn(String blankString) {
        blankString.chars().forEach(charValue ->
                assertThat(charValue)
                        .describedAs("char %d not less than 255", charValue)
                        .isLessThanOrEqualTo(0xFF));
    }
}
