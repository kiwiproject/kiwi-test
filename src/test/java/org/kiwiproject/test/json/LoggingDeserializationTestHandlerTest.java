package org.kiwiproject.test.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kiwiproject.json.JsonHelper;

import java.util.stream.IntStream;

@DisplayName("LoggingDeserializationTestHandler")
@Slf4j
class LoggingDeserializationTestHandlerTest {

    private JsonHelper jsonHelper;
    private LoggingDeserializationTestHandler handler;

    @BeforeEach
    void setUp() {
        jsonHelper = JsonHelper.newDropwizardJsonHelper();
        handler = new LoggingDeserializationTestHandler();
    }

    @Test
    void shouldCountEveryUnknownProperty() {
        ensureFailOnUnknownPropertiesIsDisabled();

        var numTimes = 10;
        IntStream.rangeClosed(1, numTimes).forEach(ignored -> jsonHelper.copy(new Foo(), Bar.class));

        assertThat(handler.getErrorCount()).isEqualTo(numTimes);
    }

    @Nested
    class WhenFailOnUnknownProperties {

        @Nested
        class IsEnabled {

            @Test
            void shouldLogAndThrowUnrecognizedPropertyException() {
                ensureFailOnUnknownPropertiesIsEnabled();

                assertThatThrownBy(() ->
                        jsonHelper.copy(new Foo(), Bar.class))
                        .hasCauseInstanceOf(UnrecognizedPropertyException.class)
                        .hasMessageContaining("Unrecognized field \"foo\"")
                        .hasMessageContaining("not marked as ignorable");

                assertThat(handler.getErrorCount()).isOne();
            }
        }

        @Nested
        class IsDisabled {

            @Test
            void shouldNotThrowException() {
                ensureFailOnUnknownPropertiesIsDisabled();

                assertThatCode(() -> jsonHelper.copy(new Foo(), Bar.class))
                        .doesNotThrowAnyException();

                assertThat(handler.getErrorCount()).isOne();
            }
        }
    }

    @Nested
    class WhenIgnoreUnknownTrue {

        @Test
        void shouldNotThrowException() {
            ensureFailOnUnknownPropertiesIsDisabled();

            assertThatCode(() -> jsonHelper.copy(new Foo(), Baz.class))
                    .doesNotThrowAnyException();

            assertThat(handler.getErrorCount()).isZero();
        }

        @Test
        void shouldNotThrow_EvenIfFailOnUnknownPropertiesEnabled() {
            ensureFailOnUnknownPropertiesIsEnabled();

            assertThatCode(() -> jsonHelper.copy(new Foo(), Baz.class))
                    .doesNotThrowAnyException();

            assertThat(handler.getErrorCount()).isZero();
        }
    }

    private void ensureFailOnUnknownPropertiesIsDisabled() {
        jsonHelper.getObjectMapper()
                .addHandler(handler)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private void ensureFailOnUnknownPropertiesIsEnabled() {
        jsonHelper.getObjectMapper()
                .addHandler(handler)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @SuppressWarnings("unused")
    static class Foo {
        public String foo = "foo";
    }

    @SuppressWarnings("unused")
    static class Bar {
        public String bar;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Baz {
    }
}