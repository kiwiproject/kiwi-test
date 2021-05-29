package org.kiwiproject.test.assertj.dropwizard.metrics;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.kiwiproject.test.assertj.dropwizard.metrics.HealthCheckResultAssertions.assertThat;

import com.codahale.metrics.health.HealthCheck;
import lombok.Builder;
import lombok.Singular;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Map;

@DisplayName("HealthCheckResultAssertions")
class HealthCheckResultAssertionsTest {

    /**
     * A simple "real mock" health check used in this test.
     */
    @Builder
    static class MockHealthCheck extends HealthCheck {

        @Builder.Default
        private final boolean healthy = true;

        private final String message;

        private final Exception error;

        @Builder.Default
        private final boolean throwExceptionOnCheck = false;

        // NOTE: @Singular always creates a non-null collection
        @Singular
        private final Map<String, Object> details;

        @Override
        protected Result check() throws Exception {
            if (throwExceptionOnCheck) {
                throw error;
            }

            var resultBuilder = Result.builder().withMessage(message);

            if (healthy) {
                resultBuilder.healthy();
            } else if (nonNull(error)) {
                resultBuilder.unhealthy(error);
            } else {
                resultBuilder.unhealthy();
            }

            details.forEach(resultBuilder::withDetail);

            return resultBuilder.build();
        }
    }

    @Nested
    class IsHealthy {

        @Test
        void shouldPass_WhenHealthy() {
            var healthCheck = MockHealthCheck.builder().healthy(true).build();

            assertThatCode(() -> assertThat(healthCheck).isHealthy())
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldFail_WhenNotHealthy() {
            var healthCheck = MockHealthCheck.builder().healthy(false).build();

            assertThatThrownBy(() -> assertThat(healthCheck).isHealthy())
                    .isNotNull()
                    .hasMessageContaining("Expected result to be healthy");
        }
    }

    @Nested
    class IsUnhealthy {

        @Test
        void shouldPass_WhenNotHealthy() {
            var healthCheck = MockHealthCheck.builder().healthy(false).build();

            assertThatCode(() -> assertThat(healthCheck).isUnhealthy())
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldFail_WhenHealthy() {
            var healthCheck = MockHealthCheck.builder().healthy(true).build();

            assertThatThrownBy(() -> assertThat(healthCheck).isUnhealthy())
                    .isNotNull()
                    .hasMessageContaining("Expected result to be unhealthy");
        }
    }

    @Nested
    class HasMessageMethods {

        private HealthCheck healthCheck;

        @BeforeEach
        void setUp() {
            healthCheck = MockHealthCheck.builder().message("The answer is 42").build();
        }

        @Nested
        class HasMessage {

            @Test
            void shouldPass_WhenMethodEquals() {
                assertThatCode(() -> assertThat(healthCheck)
                        .isHealthy()
                        .hasMessage("The answer is 42")
                ).doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenMessageDoesNotEqual() {
                assertThatThrownBy(() -> assertThat(healthCheck)
                        .isHealthy()
                        .hasMessage("The answer to the ultimate question is most definitely 42")
                ).hasMessageContaining("Wrong message");
            }
        }

        @Nested
        class HasMessageWithVarargs {

            @Test
            void shouldPass_WhenMessageEquals() {
                assertThatCode(() -> assertThat(healthCheck)
                        .isHealthy()
                        .hasMessage("The {} is {}", "answer", 42)
                ).doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenMessageDoesNotEqual() {
                assertThatThrownBy(() -> assertThat(healthCheck)
                        .isHealthy()
                        .hasMessage("The {} to the {} question is most definitely {}", "answer", "ultimate", 42)
                ).hasMessageContaining("Wrong formatted message");
            }
        }

        @Nested
        class HasMessageContaining {

            @Test
            void shouldPass_WhenErrorMessageContains() {
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasMessageContaining("answer is")
                ).doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenErrorMessageDoesNotContain() {
                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasMessageContaining("ultimate question")
                ).hasMessageContaining("Wrong message fragment");
            }
        }

        @Nested
        class HasMessageStartingWith {

            @Test
            void shouldPass_WhenErrorMessageStartsWith() {
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasMessageStartingWith("The answer"))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenErrorMessageDoesNotStartWith() {
                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasMessageStartingWith("answer is"))
                        .hasMessageContaining("Wrong message prefix");
            }
        }

        @Nested
        class HasMessageEndingWith {

            @Test
            void shouldPass_WhenErrorMessageEndsWith() {
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasMessageEndingWith("is 42"))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenErrorMessageDoesNotStartWith() {
                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasMessageEndingWith("something else"))
                        .hasMessageContaining("Wrong message suffix");
            }
        }
    }

    @Nested
    class ErrorMethods {

        private HealthCheck healthCheck;

        @BeforeEach
        void setUp() {
            var error = new CertificateExpiredException("it's way out of date");
            healthCheck = MockHealthCheck.builder().healthy(false).error(error).build();
        }

        @Nested
        class HasError {

            @Test
            void shouldPass_WhenHasError() {
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isUnhealthy()
                                .hasError())
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenHasNoError() {
                var noErrorHealthCheck = MockHealthCheck.builder().healthy(false).build();

                assertThatThrownBy(() ->
                        assertThat(noErrorHealthCheck)
                                .isUnhealthy()
                                .hasError())
                        .hasMessageContaining("Expected to have an error");
            }
        }

        @Nested
        class HasNoError {

            @Test
            void shouldPass_WhenHasNoError() {
                var noErrorHealthCheck = MockHealthCheck.builder().build();

                assertThatCode(() ->
                        assertThat(noErrorHealthCheck)
                                .isHealthy()
                                .hasNoError())
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenHasError() {
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isUnhealthy()
                                .hasNoError())
                        .hasMessageContaining("Expected not to have an error");
            }
        }

        @Nested
        class HasErrorInstanceOf {

            @Test
            void shouldPass_WhenErrorIsInstanceOfType() {
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isUnhealthy()
                                .hasErrorInstanceOf(CertificateException.class))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenErrorHasIncorrectType() {
                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .isUnhealthy()
                                .hasErrorInstanceOf(IllegalStateException.class))
                        .hasMessageContaining("Wrong error type");
            }
        }

        @Nested
        class HasErrorExactlyInstanceOf {

            @Test
            void shouldPass_WhenErrorIsExactType() {
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isUnhealthy()
                                .hasErrorExactlyInstanceOf(CertificateExpiredException.class))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenErrorHasIncorrectType() {
                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .isUnhealthy()
                                .hasErrorExactlyInstanceOf(CertificateNotYetValidException.class))
                        .hasMessageContaining("Wrong exact error type");
            }
        }

        @Nested
        class HasErrorWithMessage {

            @Test
            void shouldPass_WhenErrorMessageEquals() {
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .hasErrorWithMessage("it's way out of date"))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenErrorMessageDoesNotEqual() {
                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .hasErrorWithMessage("it's only slightly out of date"))
                        .hasMessageContaining("Wrong error message");
            }
        }

        @Nested
        class ErrorWithMessageContaining {

            @Test
            void shouldPass_WhenErrorMessageContains() {
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isUnhealthy()
                                .hasErrorWithMessageContaining("way out"))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenErrorMessageDoesNotContain() {
                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .hasErrorWithMessageContaining("way beyond expiration date"))
                        .hasMessageContaining("Wrong error message fragment");
            }
        }
    }

    @Nested
    class DetailMethods {

        private HealthCheck healthCheck;

        @BeforeEach
        void setUp() {
            healthCheck = MockHealthCheck.builder()
                    .detail("answer", 42)
                    .detail("book", "Hitchhiker's Guide to the Galaxy")
                    .detail("author", "Douglas Adams")
                    .detail("pubYear", 1979)
                    .build();
        }

        @Nested
        class HasDetailsContainingKey {

            @Test
            void shouldPass_WhenDetailsContainExpectedKey() {
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasDetailsContainingKey("answer")
                                .hasDetailsContainingKey("book")
                                .hasDetailsContainingKey("author")
                                .hasDetailsContainingKey("pubYear"))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenDetailsDoesNotContainExpectedKey() {
                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasDetailsContainingKey("answer")
                                .hasDetailsContainingKey("vogon")
                                .hasDetailsContainingKey("author"))
                        .hasMessageContaining("Expected key not found in details");
            }

            @Test
            void shouldFail_WhenDetailsIsNull() {
                var healthCheck = newHealthCheckWithNullDetails();

                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasDetailsContainingKey("foo"))
                        .hasMessageContaining("Expected key not found in details");
            }
        }

        @Nested
        class HasDetailsContainingKeys {

            @Test
            void shouldPass_WhenDetailsContainsExpectedKeys() {
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasDetailsContainingKeys("answer", "book", "author", "pubYear"))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenDetailsDoesNotContainKey() {
                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasDetailsContainingKeys("answer", "vogon", "author"))
                        .hasMessageContaining("Expected keys not found in details");
            }

            @Test
            void shouldFail_WhenDetailsIsNull() {
                var healthCheck = newHealthCheckWithNullDetails();

                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasDetailsContainingKey("foo"))
                        .hasMessageContaining("Expected key not found in details");
            }
        }

        @Nested
        class HasDetail {

            @Test
            void shouldPass_WhenDetailsContainExpectedKeyAndValue() {
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasDetail("answer", 42)
                                .hasDetail("author", "Douglas Adams")
                                .hasDetail("pubYear", 1979)
                                .hasDetail("book", "Hitchhiker's Guide to the Galaxy"))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenDetailsDoesNotContainExpectedKeyAndValue() {
                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasDetail("answer", 42)
                                .hasDetail("author", "Douglas Adams")
                                .hasDetail("pubYear", 2005)
                                .hasDetail("book", "The Hitchhiker's Guide to the Galaxy"))
                        .hasMessageContaining("Expected detail not found");
            }

            @Test
            void shouldFail_WhenDetailsIsNull() {
                var healthCheck = newHealthCheckWithNullDetails();

                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasDetail("foo", "bar"))
                        .hasMessageContaining("Expected detail not found");
            }
        }

        @Nested
        class HasEmptyDetails {

            @Test
            void shouldPass_WhenDetailsAreEmpty() {
                var healthCheck = MockHealthCheck.builder().details(Map.of()).build();

                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasEmptyDetails())
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenDetailsAreNotEmpty() {
                var healthCheck = MockHealthCheck.builder().details(Map.of("answer", 42)).build();


                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasEmptyDetails())
                        .hasMessageContaining("Expected empty (non-null) details");
            }
        }

        @Nested
        class HasNullDetails {

            @Test
            void shouldPass_WhenDetailsAreNull() {
                var healthCheck = newHealthCheckWithNullDetails();

                assertThatCode(() ->
                        assertThat(healthCheck)
                                .hasNullDetails())
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenDetailsAreNotNull() {
                var healthCheck = MockHealthCheck.builder().detail("answer", 42).build();

                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .hasNullDetails())
                        .hasMessageContaining("Expected null details");
            }

        }

        @Nested
        class HasNullOrEmptyDetails {

            @Test
            void shouldPass_WhenDetailsAreNull() {
                var healthCheck = newHealthCheckWithNullDetails();

                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasNullOrEmptyDetails())
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldPass_WhenDetailsAreEmpty() {
                var healthCheck = newMockHealthCheckWithEmptyDetails();

                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasNullOrEmptyDetails())
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenDetailsAreNotNullOrEmpty() {
                var healthCheck = MockHealthCheck.builder().detail("answer", 42).build();

                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .hasNullOrEmptyDetails())
                        .hasMessageContaining("Expected null or empty details");
            }
        }

        @Nested
        class DoesNotHaveDetailsContainingKey {

            @Test
            void shouldPass_WhenDetailsAreNull() {
                var healthCheck = newHealthCheckWithNullDetails();
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .doesNotHaveDetailsContainingKey("someKey"))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldPass_WhenDetailsAreEmpty() {
                var healthCheck = newMockHealthCheckWithEmptyDetails();
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .doesNotHaveDetailsContainingKey("someKey"))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldPass_WhenDetailsDoesNotContainGivenKey() {
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .doesNotHaveDetailsContainingKey("numPages"))
                        .doesNotThrowAnyException();
            }

            @ParameterizedTest
            @ValueSource(strings = {"book", "author", "pubYear"})
            void shouldFail_WhenDetailsContainsGivenKey(String key) {
                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .doesNotHaveDetailsContainingKey(key))
                        .hasMessageContaining("Expected details not to contain key '%s'", key);
            }
        }

        @Nested
        class DoesNotHaveDetailsContainingKeys {

            @Test
            void shouldPass_WhenDetailsAreNull() {
                var healthCheck = newHealthCheckWithNullDetails();
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .doesNotHaveDetailsContainingKeys("someKey", "anotherKey", "yetAnotherKey"))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldPass_WhenDetailsAreEmpty() {
                var healthCheck = newMockHealthCheckWithEmptyDetails();
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .doesNotHaveDetailsContainingKeys("someKey", "anotherKey", "yetAnotherKey"))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldPass_WhenDetailsDoesNotContainGivenKeys() {
                assertThatCode(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .doesNotHaveDetailsContainingKeys("numPages", "publisher", "ISBN"))
                        .doesNotThrowAnyException();
            }

            @ParameterizedTest
            @CsvSource({
                    "author, pubYear",
                    "author, book",
                    "book, pubYear"
            })
            void shouldFail_WhenDetailsContainsGivenKey(String key1, String key2) {
                assertThatThrownBy(() ->
                        assertThat(healthCheck)
                                .isHealthy()
                                .doesNotHaveDetailsContainingKeys(key1, key2))
                        .hasMessageContaining("Expected details not to contain keys: '%s', '%s'", key1, key2);
            }
        }
    }

    private HealthCheck newHealthCheckWithNullDetails() {
        // NOTE: Cannot use MockHealthCheck here b/c it always creates non-null details (b/c it uses @Singular)
        return new HealthCheck() {

            @Override
            protected Result check() {
                var result = Result.healthy("Healthy, but null details");
                var details = result.getDetails();
                verify(isNull(details),
                        "Incorrect state: assuming null details but details are: %s", details);

                return result;
            }
        };
    }

    private MockHealthCheck newMockHealthCheckWithEmptyDetails() {
        var healthCheck = MockHealthCheck.builder().build();
        var result = healthCheck.execute();
        var details = result.getDetails();
        verify(nonNull(details) && details.isEmpty(),
                "Incorrect state: assuming empty details but details are: %s", details);
        return healthCheck;
    }

}
