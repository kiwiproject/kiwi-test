package org.kiwiproject.test.assertj.dropwizard.metrics;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.kiwiproject.test.assertj.dropwizard.metrics.HealthCheckResultAssertions.assertThat;
import static org.kiwiproject.test.assertj.dropwizard.metrics.HealthCheckResultAssertions.nullSafeGetSize;
import static org.kiwiproject.test.constants.KiwiTestConstants.JSON_HELPER;

import com.codahale.metrics.health.HealthCheck;
import lombok.Builder;
import lombok.Setter;
import lombok.Singular;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.kiwiproject.test.junit.jupiter.ClearBoxTest;
import org.kiwiproject.test.junit.jupiter.params.provider.MinimalBlankStringSource;

import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

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

        @Singular(ignoreNullCollections = true)
        @Setter  // to allow setting explicitly to null
        private Map<String, Object> details;

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

            Optional.ofNullable(details).ifPresent(theDetails -> theDetails.forEach(resultBuilder::withDetail));

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
    class HasHealthyValue {

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void shouldPass_WhenReturnsExpectedValue(boolean healthy) {
            var healthCheck = MockHealthCheck.builder().healthy(healthy).build();

            assertThatCode(() -> assertThat(healthCheck).hasHealthyValue(healthy))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void shouldFail_WhenDoesNotReturnExpectedValue(boolean healthy) {
            var healthCheck = MockHealthCheck.builder().healthy(!healthy).build();

            assertThatThrownBy(() -> assertThat(healthCheck).hasHealthyValue(healthy))
                    .isNotNull()
                    .hasMessageContaining("Expected result to return %b from isHealthy", healthy);
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
        class HasDetails {

            @ParameterizedTest
            @ValueSource(ints = { 1, 5, 15 })
            void shouldPass_WhenDetailsHasOneOrMoreEntries(int size) {
                var details = createDetailsOfSize(size);
                var mockHealthCheck = MockHealthCheck.builder().details(details).build();

                assertThatCode(() ->
                        assertThat(mockHealthCheck)
                                .isHealthy()
                                .hasDetails())
                        .doesNotThrowAnyException();
            }

            @ParameterizedTest
            @NullAndEmptySource
            void shouldFail_WhenDetailsIsNullOrEmpty(Map<String, Object> details) {
                var mockHealthCheck = MockHealthCheck.builder().build();
                mockHealthCheck.setDetails(details);

                assertThatThrownBy(() -> assertThat(mockHealthCheck).hasDetails())
                        .hasMessageContaining("Expected at least one detail");
            }
        }

        @Nested
        class HasDetailsWithSize {

            @ParameterizedTest
            @ValueSource(ints = { 1, 5, 15 })
            void shouldPass_WhenDetailsHasExpectedSize(int size) {
                var details = createDetailsOfSize(size);
                var mockHealthCheck = MockHealthCheck.builder().details(details).build();

                assertThatCode(() ->
                        assertThat(mockHealthCheck)
                                .isHealthy()
                                .hasDetailsWithSize(size))
                        .doesNotThrowAnyException();
            }

            @ParameterizedTest
            @NullAndEmptySource
            void shouldFail_WhenDetailsIsNullOrEmpty(Map<String, Object> details) {
                var mockHealthCheck = MockHealthCheck.builder().build();
                mockHealthCheck.setDetails(details);

                assertThatThrownBy(() -> assertThat(mockHealthCheck).hasDetailsWithSize(3))
                        .hasMessageContaining("Expected 3 details, but found 0");
            }

            @ParameterizedTest
            @ValueSource(ints = { 1, 5, 10 })
            void shouldFail_WhenDetailsDoesNotHaveExpectedSize(int size) {
                var details = createDetailsOfSize(size);
                var mockHealthCheck = MockHealthCheck.builder().details(details).build();

                var expectedSize = size + 1;
                assertThatThrownBy(() -> assertThat(mockHealthCheck).hasDetailsWithSize(expectedSize))
                        .hasMessageContaining("Expected %d details, but found %d", expectedSize, size);
            }

            @ClearBoxTest
            void nullSafeGetSize_shouldReturnZeroForNullMaps() {
                Assertions.assertThat(nullSafeGetSize(null)).isZero();
            }

            @ClearBoxTest
            void nullSafeGetSize_shouldReturnMapSizeForNonNullMaps() {
                assertAll(
                        () -> Assertions.assertThat(nullSafeGetSize(Map.of())).isZero(),
                        () -> Assertions.assertThat(nullSafeGetSize(Map.of("1", 1))).isOne(),
                        () -> Assertions.assertThat(nullSafeGetSize(Map.of("1", 1, "2", 2))).isEqualTo(2),
                        () -> Assertions.assertThat(nullSafeGetSize(Map.of("1", 1, "2", 2, "3", 3))).isEqualTo(3)
                );
            }
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> createDetailsOfSize(int count) {
            var entryList = IntStream.rangeClosed(1, count)
                    .mapToObj(n -> Map.entry("key" + n, "value" + n))
                    .toList();
            Map.Entry<String, Object>[] entryArray = entryList.toArray(new Map.Entry[0]);
            return Map.ofEntries(entryArray);
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
                var theHealthCheck = newHealthCheckWithNullDetails();

                assertThatThrownBy(() ->
                        assertThat(theHealthCheck)
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
                var theHealthCheck = newHealthCheckWithNullDetails();

                assertThatThrownBy(() ->
                        assertThat(theHealthCheck)
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
                var theHealthCheck = newHealthCheckWithNullDetails();

                assertThatThrownBy(() ->
                        assertThat(theHealthCheck)
                                .isHealthy()
                                .hasDetail("foo", "bar"))
                        .hasMessageContaining("Expected detail not found");
            }
        }

        @Nested
        class HasDetailAtPath {

            private HealthCheck.Result result;

            @BeforeEach
            void setUp() throws Exception {
                var json = """
                        {
                            "timestamp": 1711234106709,
                            "foods": {
                                "fruits": {
                                    "apple-jazz": {
                                        "sku": "12345",
                                        "color": "red",
                                        "style": "Jazz",
                                        "count": 12,
                                        "unitPrice": 2.50
                                    },
                                    "orange-valencia": {
                                        "sku": "76543",
                                        "color": "orange",
                                        "style": "Valencia",
                                        "count": 6,
                                        "unitPrice": 2.75
                                    }
                                },
                                "snacks": {
                                    "cookie-oreo-mint": {
                                        "color": "Brown/Green",
                                        "style": "Mint Oreos",
                                        "count": 15,
                                        "unitPrice": 5.99
                                    }
                                }
                            },
                            "officeSupply": {
                                "paper-8x11": {
                                    "color": "white",
                                    "style": "500 sheets 8x11 printer paper",
                                    "count": 10,
                                    "unitPrice": 12.99,
                                    "extraInfo": null
                                }
                            }
                        }
                        """;

                var details = JSON_HELPER.toMap(json);
                var mockHealthCheck = MockHealthCheck.builder().details(details).build();
                result = mockHealthCheck.check();
            }

            @Test
            void shouldPass_WhenDetailAtPathHasExpectedValue() {
                assertAll(
                        () -> assertThatCode(() ->
                                assertThat(result).hasDetailAtPath("timestamp", 1711234106709L))
                                .doesNotThrowAnyException(),

                        () -> assertThatCode(() ->
                                assertThat(result).hasDetailAtPath("foods.fruits.apple-jazz.color", "red"))
                                .doesNotThrowAnyException(),

                        () -> assertThatCode(() ->
                                assertThat(result).hasDetailAtPath("foods.fruits.apple-jazz.count", 12))
                                .doesNotThrowAnyException(),

                        () -> assertThatCode(() ->
                                assertThat(result).hasDetailAtPath("foods.fruits.orange-valencia.unitPrice", 2.75))
                                .doesNotThrowAnyException(),

                        () -> assertThatCode(() ->
                                assertThat(result).hasDetailAtPath("foods.snacks.cookie-oreo-mint.color", "Brown/Green"))
                                .doesNotThrowAnyException(),

                        () -> assertThatCode(() ->
                                assertThat(result).hasDetailAtPath("officeSupply.paper-8x11.unitPrice", 12.99))
                                .doesNotThrowAnyException(),

                        () -> assertThatCode(() ->
                                assertThat(result).hasDetailAtPath("officeSupply.paper-8x11.extraInfo", null))
                                .doesNotThrowAnyException()
                );
            }

            @Test
            void shouldFail_WhenDetailAtPath_DoesNotExistOrHaveExpectedValue() {
                assertAll(
                        () -> assertThatThrownBy(() ->
                                assertThat(result)
                                        .hasDetailAtPath("foods.fruits.kiwi.color", "green"))
                                .hasMessageContaining("Expected detail at path 'foods.fruits.kiwi.color' to have value green, but was: null"),

                        () -> assertThatThrownBy(() ->
                                assertThat(result)
                                        .hasDetailAtPath("foods.fruits.apple-jazz.count", 10))
                                .hasMessageContaining("Expected detail at path 'foods.fruits.apple-jazz.count' to have value 10, but was: 12")
                );
            }

            @ParameterizedTest
            @MinimalBlankStringSource
            void shouldThrowIllegalArgument_WhenGivenBlankPath(String path) {
                assertThatIllegalArgumentException().isThrownBy(() ->
                                assertThat(result).hasDetailAtPath(path, "green"))
                        .withMessage("path must not be blank");
            }
        }

        @Nested
        class HasEmptyDetails {

            @Test
            void shouldPass_WhenDetailsAreEmpty() {
                var mockHealthCheck = MockHealthCheck.builder().details(Map.of()).build();

                assertThatCode(() ->
                        assertThat(mockHealthCheck)
                                .isHealthy()
                                .hasEmptyDetails())
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenDetailsAreNotEmpty() {
                var mockHealthCheck = MockHealthCheck.builder().details(Map.of("answer", 42)).build();


                assertThatThrownBy(() ->
                        assertThat(mockHealthCheck)
                                .isHealthy()
                                .hasEmptyDetails())
                        .hasMessageContaining("Expected empty (non-null) details");
            }
        }

        @Nested
        class HasNullDetails {

            @Test
            void shouldPass_WhenDetailsAreNull() {
                var theHealthCheck = newHealthCheckWithNullDetails();

                assertThatCode(() ->
                        assertThat(theHealthCheck)
                                .hasNullDetails())
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenDetailsAreNotNull() {
                var mockHealthCheck = MockHealthCheck.builder().detail("answer", 42).build();

                assertThatThrownBy(() ->
                        assertThat(mockHealthCheck)
                                .hasNullDetails())
                        .hasMessageContaining("Expected null details");
            }

        }

        @Nested
        class HasNullOrEmptyDetails {

            @Test
            void shouldPass_WhenDetailsAreNull() {
                var theHealthCheck = newHealthCheckWithNullDetails();

                assertThatCode(() ->
                        assertThat(theHealthCheck)
                                .isHealthy()
                                .hasNullOrEmptyDetails())
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldPass_WhenDetailsAreEmpty() {
                var mockHealthCheck = newMockHealthCheckWithEmptyDetails();

                assertThatCode(() ->
                        assertThat(mockHealthCheck)
                                .isHealthy()
                                .hasNullOrEmptyDetails())
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldFail_WhenDetailsAreNotNullOrEmpty() {
                var mockHealthCheck = MockHealthCheck.builder().detail("answer", 42).build();

                assertThatThrownBy(() ->
                        assertThat(mockHealthCheck)
                                .isHealthy()
                                .hasNullOrEmptyDetails())
                        .hasMessageContaining("Expected null or empty details");
            }
        }

        @Nested
        class DoesNotHaveDetailsContainingKey {

            @Test
            void shouldPass_WhenDetailsAreNull() {
                var theHealthCheck = newHealthCheckWithNullDetails();
                assertThatCode(() ->
                        assertThat(theHealthCheck)
                                .isHealthy()
                                .doesNotHaveDetailsContainingKey("someKey"))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldPass_WhenDetailsAreEmpty() {
                var mockHealthCheck = newMockHealthCheckWithEmptyDetails();
                assertThatCode(() ->
                        assertThat(mockHealthCheck)
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
                var theHealthCheck = newHealthCheckWithNullDetails();
                assertThatCode(() ->
                        assertThat(theHealthCheck)
                                .isHealthy()
                                .doesNotHaveDetailsContainingKeys("someKey", "anotherKey", "yetAnotherKey"))
                        .doesNotThrowAnyException();
            }

            @Test
            void shouldPass_WhenDetailsAreEmpty() {
                var mockHealthCheck = newMockHealthCheckWithEmptyDetails();
                assertThatCode(() ->
                        assertThat(mockHealthCheck)
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
