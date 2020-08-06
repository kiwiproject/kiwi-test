package org.kiwiproject.test.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kiwiproject.net.UncheckedURISyntaxException;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;

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
        void shouldThrow_WhenFixtureCannotBeRead() {
            // Use charset that will cause an IOException
            assertThatThrownBy(() -> Fixtures.fixture(PANGRAM_FIXTURE, StandardCharsets.UTF_16))
                    .isExactlyInstanceOf(UncheckedIOException.class)
                    .hasCauseExactlyInstanceOf(MalformedInputException.class)
                    .hasMessage("Error reading fixture: %s", PANGRAM_FIXTURE);
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