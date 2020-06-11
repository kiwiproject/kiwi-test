package org.kiwiproject.test.constants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.kiwiproject.test.constants.KiwiTestConstants.JSON_DESERIALIZATION_HANDLER;
import static org.kiwiproject.test.constants.KiwiTestConstants.JSON_HELPER;
import static org.kiwiproject.test.constants.KiwiTestConstants.OBJECT_MAPPER;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import lombok.Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kiwiproject.test.junit.jupiter.WhiteBoxTest;

@DisplayName("KiwiTestConstants")
class KiwiTestConstantsTest {

    @Nested
    class ObjectMapper {

        @WhiteBoxTest
        void shouldHave_FailOnUnknownProperties_Disabled() {
            assertThat(OBJECT_MAPPER.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
                    .describedAs("We are expecting FAIL_ON_UNKNOWN_PROPERTIES to be disabled by default")
                    .isFalse();
        }
    }

    @Nested
    class JsonHelper {

        @Test
        void shouldCountDeserializationProblems() {
            var json = JSON_HELPER.toJsonFromKeyValuePairs(
                    "field1", "value1",
                    "field2", 42,
                    "field3", "value3",
                    "field4", "value4"
            );

            assertThatCode(() -> JSON_HELPER.toObject(json, Strict.class)).doesNotThrowAnyException();

            assertThat(JSON_DESERIALIZATION_HANDLER.getErrorCount())
                    .describedAs("The unknown properties should have been counted")
                    .isEqualTo(2);

            assertThatCode(() -> JSON_HELPER.toObject(json, Lenient.class)).doesNotThrowAnyException();

            assertThat(JSON_DESERIALIZATION_HANDLER.getErrorCount())
                    .describedAs("The unknown properties should have been ignored")
                    .isEqualTo(2);
        }
    }

    @Value
    static class Strict {
        String field1;
        int field2;
    }

    @Value
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Lenient {
        String field1;
        int field2;
    }
}