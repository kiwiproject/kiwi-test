package org.kiwiproject.test.junit.jupiter.params.provider;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

@DisplayName("MinimalBlankStringArgumentsProvider")
class MinimalBlankStringArgumentsProviderTest {

    @ParameterizedTest
    @ArgumentsSource(MinimalBlankStringArgumentsProvider.class)
    void shouldProvideBlankArguments(String blankString) {
        assertThat(blankString).isBlank();

        if (nonNull(blankString)) {
            assertOnlyCharactersIn(blankString);
        }
    }

    @ParameterizedTest
    @MinimalBlankStringSource
    void shouldProvideBlankArgumentsUsingAnnotation(String blankString) {
        assertThat(blankString).isBlank();

        if (nonNull(blankString)) {
            assertOnlyCharactersIn(blankString);
        }
    }

    private static void assertOnlyCharactersIn(String blankString) {
        blankString.chars().forEach(charValue ->
                assertThat(charValue)
                        .isIn(
                                9,  // tab
                                10,  // newline
                                13,  // carriage return
                                32  // space
                        ));
    }
}
