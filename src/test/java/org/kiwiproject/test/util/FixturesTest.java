package org.kiwiproject.test.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kiwiproject.net.UncheckedURISyntaxException;
import org.kiwiproject.test.junit.jupiter.ClearBoxTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;

@DisplayName("Fixtures")
class FixturesTest {

    private static final String PANGRAM_FIXTURE = "FixturesTest/pangram.txt";

    @Nested
    class Fixture {

        @Test
        void shouldReadContents() {
            var fixture = Fixtures.fixture(PANGRAM_FIXTURE);

            assertThat(fixture).isEqualTo("The quick brown fox jumps over the lazy dog");
        }

        @Test
        void shouldThrow_WhenFixtureDoesNotExist() {
            assertThatThrownBy(() -> Fixtures.fixture("foo/bar/baz.txt"))
                    .isExactlyInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldThrow_WhenFixtureCannotBeRead_DueToMalformedInput() {
            // Use charset that will cause a MalformedInputException
            assertThatThrownBy(() -> Fixtures.fixture(PANGRAM_FIXTURE, StandardCharsets.UTF_16))
                    .isExactlyInstanceOf(UncheckedIOException.class)
                    .hasCauseExactlyInstanceOf(MalformedInputException.class)
                    .hasMessage("Error reading fixture: %s", PANGRAM_FIXTURE);
        }

        @Test
        void shouldThrow_WhenFixtureCannotBeRead_DueToUnmappableCharacter() {
            // Use charset that will cause UnmappableCharacterException
            // The JIS_X0212-1990 charset was found by trial and error, not
            // because I know anything about Japanese charsets!
            var jisCharset = Charset.forName("JIS_X0212-1990");
            assertThatThrownBy(() -> Fixtures.fixture(PANGRAM_FIXTURE, jisCharset))
                    .isExactlyInstanceOf(UncheckedIOException.class)
                    .hasCauseExactlyInstanceOf(UnmappableCharacterException.class)
                    .hasMessage("Error reading fixture: %s", PANGRAM_FIXTURE);
        }

        @Nested
        class UncheckedIOExceptionOrOriginalError {

            @ClearBoxTest
            void shouldReturnTheOriginalError_WhenIt_HasNoCause() {
                var error = new Error();
                assertThat(Fixtures.uncheckedIOExceptionOrOriginalError(error, PANGRAM_FIXTURE))
                        .isSameAs(error);
            }

            @ClearBoxTest
            void shouldReturnTheOriginalError_WhenItsCause_IsNotCharacterCodingException() {
                var error = new Error(new IOException("I/O error"));
                assertThat(Fixtures.uncheckedIOExceptionOrOriginalError(error, PANGRAM_FIXTURE))
                        .isSameAs(error);
            }

            @ClearBoxTest
            void shouldReturnUncheckedIOException_WhenErrorCause_IsMalformedInputException() {
                var malformedInputEx = new MalformedInputException(42);
                var error = new Error(malformedInputEx);
                assertThat(Fixtures.uncheckedIOExceptionOrOriginalError(error, PANGRAM_FIXTURE))
                        .isExactlyInstanceOf(UncheckedIOException.class)
                        .hasCause(malformedInputEx);
            }

            @ClearBoxTest
            void shouldReturnUncheckedIOException_WhenErrorCause_IsUnmappableCharacterException() {
                var unmappableCharacterEx = new UnmappableCharacterException(84);
                var error = new Error(unmappableCharacterEx);
                assertThat(Fixtures.uncheckedIOExceptionOrOriginalError(error, PANGRAM_FIXTURE))
                        .isExactlyInstanceOf(UncheckedIOException.class)
                        .hasCause(unmappableCharacterEx);
            }
        }
    }

    @Nested
    class FixtureFile {

        @Test
        void shouldReturnFile() {
            var fixtureFile = Fixtures.fixtureFile(PANGRAM_FIXTURE);

            assertThat(fixtureFile)
                    .isAbsolute()
                    .asString()
                    .endsWith(PANGRAM_FIXTURE);
        }
    }

    @Nested
    class FixturePath {

        @Test
        void shouldReturnPath() {
            var fixturePath = Fixtures.fixturePath(PANGRAM_FIXTURE);

            assertThat(fixturePath)
                    .isAbsolute()
                    .asString()
                    .endsWith(PANGRAM_FIXTURE);
        }
    }

    @Nested
    class PathFromURL {

        @Test
        void shouldThrow_WhenGivenURLThatCannotBeConvertedToURI() throws MalformedURLException {
            var url = new URL("file://tmp/file-with>invalid-character.txt");
            assertThatThrownBy(() -> Fixtures.pathFromURL(url))
                    .isExactlyInstanceOf(UncheckedURISyntaxException.class)
                    .hasCauseExactlyInstanceOf(URISyntaxException.class)
                    .hasMessage("Error getting path from URL: " + url);
        }
    }
}
