package org.kiwiproject.test.junit.jupiter.params.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

@DisplayName("BlankStringArgumentsProvider")
class BlankStringArgumentsProviderTest {

    @ParameterizedTest
    @ArgumentsSource(BlankStringArgumentsProvider.class)
    void testThatEachProvidedArgumentIsBlank(String blankString) {
        assertThat(blankString).isBlank();
    }
}
