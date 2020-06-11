package org.kiwiproject.test.assertj.jsonassert;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.kiwiproject.collect.KiwiMaps.newLinkedHashMap;
import static org.kiwiproject.test.constants.KiwiTestConstants.JSON_HELPER;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.error.AssertJMultipleFailuresError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("JSONAssertSoftAssertions")
class JSONAssertSoftAssertionsTest {

    private JSONAssertSoftAssertions jsonAssertSoftly;
    private SoftAssertions softly;

    @BeforeEach
    void setUp() {
        softly = new SoftAssertions();
        jsonAssertSoftly = new JSONAssertSoftAssertions(softly);
    }

    @Nested
    class AssertEqualsLenient {

        @Test
        void shouldNotCareAboutOrderInJsonStrings() {
            var jerryJson = JSON_HELPER.toJson(newLinkedHashMap(
                    "first", "Jerry",
                    "last", "Seinfeld",
                    "job", "actor/comedian"
            ));

            var jerryJsonDifferentOrder = JSON_HELPER.toJson(newLinkedHashMap(
                    "job", "actor/comedian",
                    "last", "Seinfeld",
                    "first", "Jerry"
            ));

            jsonAssertSoftly.assertEqualsLenient(jerryJsonDifferentOrder, jerryJson);
        }

        @SuppressWarnings("java:S2970")  // Suppress "Assertions should be complete"
        @Test
        void shouldFailWhenJsonStringsAreNotEqual() {
            var jerryJson = JSON_HELPER.toJson(newLinkedHashMap(
                    "first", "Jerry",
                    "last", "Seinfeld",
                    "job", "actor/comedian"
            ));

            var otherJson = JSON_HELPER.toJsonFromKeyValuePairs(
                    "foo", "bar"
            );

            assertThatCode(() -> jsonAssertSoftly.assertEqualsLenient(jerryJson, otherJson))
                    .doesNotThrowAnyException();

            assertThatThrownBy(() -> softly.assertAll())
                    .isExactlyInstanceOf(AssertJMultipleFailuresError.class)
                    .hasMessageContaining("Expected: first")
                    .hasMessageContaining("Expected: last")
                    .hasMessageContaining("Expected: job");
        }
    }
}