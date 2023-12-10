package org.kiwiproject.test.xmlunit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.kiwiproject.test.util.Fixtures.fixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("KiwiXmlAssert")
@SuppressWarnings("java:S5778")
class KiwiXmlAssertTest {

    @Nested
    class EntrancePoints {

        @Test
        void canUseAssertThatXml() {
            var xml = fixture("KiwiXmlAssertTest/alice-smith.xml");
            var otherXml = fixture("KiwiXmlAssertTest/alice-smith-duplicate.xml");

            assertThatCode(() -> KiwiXmlAssert.assertThatXml(xml).isIdenticalTo(otherXml))
                    .doesNotThrowAnyException();
        }

        @Test
        void canUseAssertThat() {
            var xml = fixture("KiwiXmlAssertTest/alice-smith.xml");
            var otherXml = fixture("KiwiXmlAssertTest/alice-smith-duplicate.xml");

            assertThatCode(() -> KiwiXmlAssert.assertThat(xml).isIdenticalTo(otherXml))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class CanUseTestName {

        @Test
        void shouldAcceptTestNameAsString() {
            var xml = fixture("KiwiXmlAssertTest/alice-smith.xml");
            var otherXml = fixture("KiwiXmlAssertTest/alice-jones.xml");

            assertThatThrownBy(() ->
                    KiwiXmlAssert.assertThat(xml)
                            .withTestName("custom-test-name")
                            .and(otherXml)
                            .areIdentical())
                    .isExactlyInstanceOf(AssertionError.class);
        }

        @Test
        void shouldAcceptTestNameFromTestInfo(TestInfo testInfo) {
            var xml = fixture("KiwiXmlAssertTest/alice-smith.xml");
            var otherXml = fixture("KiwiXmlAssertTest/alice-jones.xml");

            assertThatThrownBy(() ->
                    KiwiXmlAssert.assertThat(xml)
                            .withTestNameFrom(testInfo)
                            .and(otherXml)
                            .areIdentical())
                    .isExactlyInstanceOf(AssertionError.class);
        }
    }

    @Nested
    class TerminalOperations {

        @Nested
        class And {

            @Test
            void shouldReturnCompareAssertForFurtherChaining() {
                var xml = fixture("KiwiXmlAssertTest/alice-smith.xml");
                var otherXml = fixture("KiwiXmlAssertTest/alice-smith-duplicate.xml");

                assertThatCode(() -> KiwiXmlAssert.assertThat(xml)
                        .withTestName("custom-test-name")
                        .and(otherXml)
                        .ignoreWhitespace()
                        .ignoreChildNodesOrder()
                        .ignoreComments()
                        .areIdentical())
                        .doesNotThrowAnyException();
            }
        }

        @Nested
        class IsIdenticalTo {

            @Test
            void shouldCompareIdenticalXml() {
                var xml = fixture("KiwiXmlAssertTest/alice-smith.xml");
                var otherXml = fixture("KiwiXmlAssertTest/alice-smith-duplicate.xml");

                assertThatCode(() -> KiwiXmlAssert.assertThat(xml).isIdenticalTo(otherXml))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldThrowAssertionErrorWhenXmlIsDifferent() {
                var xml = fixture("KiwiXmlAssertTest/alice-smith.xml");
                var otherXml = fixture("KiwiXmlAssertTest/alice-jones.xml");

                assertThatThrownBy(() ->
                        KiwiXmlAssert.assertThat(xml).isIdenticalTo(otherXml))
                        .isExactlyInstanceOf(AssertionError.class);
            }
        }

        @Nested
        class IsIdenticalToIgnoringWhitespace {

            @ParameterizedTest
            @ValueSource(strings = {
                    "KiwiXmlAssertTest/alice-smith-condensed.xml",
                    "KiwiXmlAssertTest/alice-smith-extra-whitespace.xml"
            })
            void shouldIgnoreWhitespace(String otherFixture) {
                var xml = fixture("KiwiXmlAssertTest/alice-smith.xml");
                var otherXml = fixture(otherFixture);

                assertThatCode(() ->
                        KiwiXmlAssert.assertThat(xml).isIdenticalToIgnoringWhitespace(otherXml))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldThrowAssertionErrorWhenXmlIsDifferent() {
                var xml = fixture("KiwiXmlAssertTest/alice-smith.xml");
                var otherXml = fixture("KiwiXmlAssertTest/alice-jones.xml");

                assertThatThrownBy(() ->
                        KiwiXmlAssert.assertThat(xml).isIdenticalToIgnoringWhitespace(otherXml))
                        .isExactlyInstanceOf(AssertionError.class);
            }
        }

        @Nested
        class IsIdenticalToIgnoringComments {

            @Test
            void shouldIgnoreComments() {
                var xml = fixture("KiwiXmlAssertTest/alice-smith.xml");
                var otherXml = fixture("KiwiXmlAssertTest/alice-smith-with-comments.xml");

                assertThatCode(() ->
                        KiwiXmlAssert.assertThat(xml).isIdenticalToIgnoringComments(otherXml))
                        .doesNotThrowAnyException();
            }

            /**
             * @implNote This is a "canary test" to flag if XMLUnit ever changes the unexpected
             * behavior that it doesn't strip comments that are between tags, only comments
             * at the top and bottom, and within elements. View the test file to see.
             */
            @Test
            void canaryDoesNotIgnoreCommentsBetweenTags() {
                var xml = fixture("KiwiXmlAssertTest/alice-smith.xml");
                var otherXml = fixture("KiwiXmlAssertTest/alice-smith-with-comments-between-tags.xml");

                assertThatThrownBy(() ->
                        KiwiXmlAssert.assertThat(xml).isIdenticalToIgnoringComments(otherXml))
                        .isExactlyInstanceOf(AssertionError.class);
            }
        }

        @Nested
        class IsIdenticalToIgnoringWhitespaceAndComments {

            @Test
            void shouldIgnoreWhitespaceAndComments() {
                var xml = fixture("KiwiXmlAssertTest/alice-smith.xml");
                var otherXml = fixture("KiwiXmlAssertTest/alice-smith-with-comments-and-extra-whitespace.xml");

                assertThatCode(() ->
                        KiwiXmlAssert.assertThat(xml).isIdenticalToIgnoringWhitespaceAndComments(otherXml))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldThrowAssertionErrorWhenXmlIsDifferent() {
                var xml = fixture("KiwiXmlAssertTest/alice-smith.xml");
                var otherXml = fixture("KiwiXmlAssertTest/alice-jones.xml");

                assertThatThrownBy(() ->
                        KiwiXmlAssert.assertThat(xml).isIdenticalToIgnoringWhitespaceAndComments(otherXml))
                        .isExactlyInstanceOf(AssertionError.class);
            }
        }
    }

}
