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
import java.nio.file.Path;

@DisplayName("Fixtures")
class FixturesTest {

    private static final String FIXTURES_TEST_DIRECTORY = "FixturesTest";

    private static final String PANGRAM_FIXTURE =
            Path.of(FIXTURES_TEST_DIRECTORY, "pangram.txt").toString();

    private static final String PANGRAM_LEADING_TRAILING_WHITESPACE_FIXTURE =
            Path.of(FIXTURES_TEST_DIRECTORY, "pangram-leading-trailing-whitespace.txt").toString();

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

            @ClearBoxTest("non-public API")
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
    class FixtureStripLeadingAndTrailingWhitespace {

        @Test
        void shouldStripLeadingAndTrailingWhitespace() {
            var fixture = Fixtures.fixtureStripLeadingAndTrailingWhitespace(PANGRAM_LEADING_TRAILING_WHITESPACE_FIXTURE);
            var expected = Fixtures.fixture(PANGRAM_LEADING_TRAILING_WHITESPACE_FIXTURE).strip();
            assertThat(fixture).isEqualTo(expected);
        }

        @Test
        void shouldStripLeadingAndTrailingWhitespaceWithCharset() {
            var fixture = Fixtures.fixtureStripLeadingAndTrailingWhitespace(PANGRAM_LEADING_TRAILING_WHITESPACE_FIXTURE, StandardCharsets.UTF_8);
            var expected = Fixtures.fixture(PANGRAM_LEADING_TRAILING_WHITESPACE_FIXTURE, StandardCharsets.UTF_8).strip();
            assertThat(fixture).isEqualTo(expected);
        }
    }

    @Nested
    class FixtureStripLeadingWhitespace {

        @Test
        void shouldStripLeadingWhitespace() {
            var fixture = Fixtures.fixtureStripLeadingWhitespace(PANGRAM_LEADING_TRAILING_WHITESPACE_FIXTURE);
            var expected = Fixtures.fixture(PANGRAM_LEADING_TRAILING_WHITESPACE_FIXTURE).stripLeading();
            assertThat(fixture).isEqualTo(expected);
        }

        @Test
        void shouldStripLeadingWhitespaceWithCharset() {
            var fixture = Fixtures.fixtureStripLeadingWhitespace(PANGRAM_LEADING_TRAILING_WHITESPACE_FIXTURE, StandardCharsets.UTF_8);
            var expected = Fixtures.fixture(PANGRAM_LEADING_TRAILING_WHITESPACE_FIXTURE, StandardCharsets.UTF_8).stripLeading();
            assertThat(fixture).isEqualTo(expected);
        }

        @Test
        void shouldNotStripTrailingWhitespace() {
            var fixture = Fixtures.fixtureStripLeadingWhitespace(PANGRAM_LEADING_TRAILING_WHITESPACE_FIXTURE);
            var expected = Fixtures.fixture(PANGRAM_LEADING_TRAILING_WHITESPACE_FIXTURE).stripLeading();
            assertThat(fixture).isEqualTo(expected);
        }
    }

    @Nested
    class FixtureStripTrailingWhitespace {

        @Test
        void shouldStripTrailingWhitespace() {
            var fixture = Fixtures.fixtureStripTrailingWhitespace(PANGRAM_LEADING_TRAILING_WHITESPACE_FIXTURE);
            var expected = Fixtures.fixture(PANGRAM_LEADING_TRAILING_WHITESPACE_FIXTURE).stripTrailing();
            assertThat(fixture).isEqualTo(expected);
        }

        @Test
        void shouldStripTrailingWhitespaceWithCharset() {
            var fixture = Fixtures.fixtureStripTrailingWhitespace(PANGRAM_LEADING_TRAILING_WHITESPACE_FIXTURE, StandardCharsets.UTF_8);
            var expected = Fixtures.fixture(PANGRAM_LEADING_TRAILING_WHITESPACE_FIXTURE, StandardCharsets.UTF_8).stripTrailing();
            assertThat(fixture).isEqualTo(expected);
        }

        @Test
        void shouldNotStripLeadingWhitespace() {
            var fixture = Fixtures.fixtureStripTrailingWhitespace(PANGRAM_LEADING_TRAILING_WHITESPACE_FIXTURE);
            var expected = Fixtures.fixture(PANGRAM_LEADING_TRAILING_WHITESPACE_FIXTURE).stripTrailing();
            assertThat(fixture).isEqualTo(expected);
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
