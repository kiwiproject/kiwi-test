package org.kiwiproject.test.assertj.dropwizard.metrics;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.joining;
import static org.kiwiproject.base.KiwiStrings.f;
import static org.kiwiproject.collect.KiwiMaps.isNullOrEmpty;
import static org.kiwiproject.logging.LazyLogParameterSupplier.lazy;

import com.codahale.metrics.health.HealthCheck;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.assertj.core.api.Assertions;
import org.kiwiproject.base.KiwiStrings;

import java.util.Arrays;

/**
 * Provides for fluent {@link HealthCheck} tests using AssertJ assertions.
 * <p>
 * All methods return 'this' to facilitate a fluent API via method chaining.
 * <p>
 * Note that both io.dropwizard.metrics:metrics-healthchecks and io.dropwizard.metrics:metrics-core must
 * be available at runtime.
 */
@SuppressWarnings("UnusedReturnValue")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HealthCheckResultAssertions {

    private final HealthCheck.Result result;

    /**
     * Starting point for health check fluent assertions on a {@link HealthCheck}.
     * <p>
     * Executes the health check, returning this instance for fluent assertion chaining.
     *
     * @param healthCheck the health check to execute
     * @return this instance
     */
    public static HealthCheckResultAssertions assertThat(HealthCheck healthCheck) {
        return assertThatHealthCheck(healthCheck);
    }

    /**
     * Starting point for health check fluent assertions on a {@link HealthCheck}.
     * <p>
     * Executes the health check, returning this instance for fluent assertion chaining.
     * <p>
     * This method is provided as an alias of {@link #assertThat(HealthCheck)} to avoid conflicts
     * when statically importing AssertJ's {@code Assertions#assertThat}, and therefore allow both
     * to be statically imported.
     *
     * @param healthCheck the health check to execute
     * @return this instance
     */
    public static HealthCheckResultAssertions assertThatHealthCheck(HealthCheck healthCheck) {
        var result = healthCheck.execute();
        return assertThat(result);
    }

    /**
     * Starting point for health check fluent assertions on a {@link HealthCheck.Result}.
     *
     * @param result the health check result to assert upon
     * @return this instance
     */
    public static HealthCheckResultAssertions assertThat(HealthCheck.Result result) {
        return assertThatResult(result);
    }

    /**
     * Starting point for health check fluent assertions on a {@link HealthCheck.Result}.
     * <p>
     * This method is provided as an alias of {@link #assertThat(HealthCheck.Result)} to avoid conflicts
     * when statically importing AssertJ's {@code Assertions#assertThat}, and therefore allow both
     * to be statically imported.
     *
     * @param result the health check result to assert upon
     * @return this instance
     */
    public static HealthCheckResultAssertions assertThatResult(HealthCheck.Result result) {
        return new HealthCheckResultAssertions(result);
    }

    /**
     * Asserts the health check result is healthy.
     *
     * @return this instance
     */
    public HealthCheckResultAssertions isHealthy() {
        Assertions.assertThat(result.isHealthy())
                .describedAs("Expected result to be healthy")
                .isTrue();

        return this;
    }

    /**
     * Asserts the health check result is not healthy.
     *
     * @return this instance
     */
    public HealthCheckResultAssertions isUnhealthy() {
        Assertions.assertThat(result.isHealthy())
                .describedAs("Expected result to be unhealthy")
                .isFalse();

        return this;
    }

    /**
     * Asserts the health check result has the given message.
     *
     * @param message the expected message
     * @return this instance
     */
    public HealthCheckResultAssertions hasMessage(String message) {
        Assertions.assertThat(result.getMessage())
                .describedAs("Wrong message")
                .isEqualTo(message);

        return this;
    }

    /**
     * Asserts the heath check result contains the given message fragment.
     *
     * @param messageFragment the expected message fragment
     * @return this instance
     */
    public HealthCheckResultAssertions hasMessageContaining(String messageFragment) {
        Assertions.assertThat(result.getMessage())
                .describedAs("Wrong message fragment")
                .contains(messageFragment);

        return this;
    }

    /**
     * Asserts the health check result starts with the given message prefix.
     *
     * @param messagePrefix the expected message prefix
     * @return this instance
     */
    public HealthCheckResultAssertions hasMessageStartingWith(String messagePrefix) {
        Assertions.assertThat(result.getMessage())
                .describedAs("Wrong message prefix")
                .startsWith(messagePrefix);

        return this;
    }

    /**
     * Asserts the health check result ends with the given message suffix.
     *
     * @param messageSuffix the expected message suffix
     * @return this instance
     */
    public HealthCheckResultAssertions hasMessageEndingWith(String messageSuffix) {
        Assertions.assertThat(result.getMessage())
                .describedAs("Wrong message suffix")
                .endsWith(messageSuffix);

        return this;
    }

    /**
     * Asserts the health check result has a message interpolated using the given message template and arguments.
     *
     * @param messageTemplate the message template to use
     * @param args            the arguments for the template
     * @return this instance
     * @implNote This uses {@link KiwiStrings#format(String, Object...)} to format {@code messageTemplate} using the
     * given {@code args}. THIS MEANS IT WILL NOT WORK if you assume it uses {@link String#format(String, Object...)}
     * formatting specifiers.
     * @see KiwiStrings#format(String, Object...)
     */
    public HealthCheckResultAssertions hasMessage(String messageTemplate, Object... args) {
        Assertions.assertThat(result.getMessage())
                .describedAs("Wrong formatted message")
                .isEqualTo(KiwiStrings.format(messageTemplate, args));

        return this;
    }

    /**
     * Asserts the health check result has an error, i.e. {@link HealthCheck.Result#getError()} returns a non-null
     * result.
     *
     * @return this instance
     */
    public HealthCheckResultAssertions hasError() {
        assertResultHasError();

        return this;
    }

    /**
     * Asserts the health check result has a null error.
     *
     * @return this instance
     */
    public HealthCheckResultAssertions hasNoError() {
        Assertions.assertThat(result.getError())
                .describedAs("Expected not to have an error")
                .isNull();

        return this;
    }

    /**
     * Asserts the health check result has an error whose type is an instance of the given type.
     *
     * @param type the expected type
     * @return this instance
     * @implNote Uses {@link org.assertj.core.api.AbstractAssert#isInstanceOf(Class)}
     */
    public HealthCheckResultAssertions hasErrorInstanceOf(Class<?> type) {
        Assertions.assertThat(result.getError())
                .describedAs("Wrong error type")
                .isInstanceOf(type);

        return this;
    }

    /**
     * Asserts the health check result has an error whose type is EXACTLY the given type.
     *
     * @param type the expected exact type
     * @return this instance
     * @implNote Uses {@link org.assertj.core.api.AbstractAssert#isExactlyInstanceOf(Class)}
     */
    public HealthCheckResultAssertions hasErrorExactlyInstanceOf(Class<?> type) {
        Assertions.assertThat(result.getError())
                .describedAs("Wrong exact error type")
                .isExactlyInstanceOf(type);

        return this;
    }

    /**
     * Asserts the health check result has an error with the given message.
     *
     * @param message the expected message
     * @return this instance
     */
    public HealthCheckResultAssertions hasErrorWithMessage(String message) {
        assertResultHasError();

        Assertions.assertThat(result.getError().getMessage())
                .describedAs("Wrong error message")
                .isEqualTo(message);

        return this;
    }

    /**
     * Asserts the health check result has an error that contains the given message fragment.
     *
     * @param messageFragment the expected message fragment
     * @return this instance
     */
    public HealthCheckResultAssertions hasErrorWithMessageContaining(String messageFragment) {
        assertResultHasError();

        Assertions.assertThat(result.getError().getMessage())
                .describedAs("Wrong error message fragment")
                .contains(messageFragment);

        return this;
    }

    private void assertResultHasError() {
        Assertions.assertThat(result.getError())
                .describedAs("Expected to have an error")
                .isNotNull();
    }

    /**
     * Asserts the health check has one or more details.
     *
     * @return this instance
     */
    public HealthCheckResultAssertions hasDetails() {
        Assertions.assertThat(result.getDetails())
                .describedAs("Expected at least one detail")
                .isNotEmpty();
        return this;
    }

    /**
     * Asserts the health check has {@code expectedSize} details.
     *
     * @param expectedSize the expected number of details
     * @return this instance
     */
    public HealthCheckResultAssertions hasDetailsWithSize(int expectedSize) {
        var details = result.getDetails();

        // details should never be null, but just in case Metrics ever changes that
        var size = isNull(details) ? 0 : details.size();

        Assertions.assertThat(details)
                .describedAs("Expected %d details, but found %d", expectedSize, size)
                .hasSize(expectedSize);
        return this;
    }

    /**
     * Asserts the health check result has an empty map of details.
     *
     * @return this instance
     */
    public HealthCheckResultAssertions hasEmptyDetails() {
        Assertions.assertThat(result.getDetails())
                .describedAs("Expected empty (non-null) details")
                .isEmpty();

        return this;
    }

    /**
     * Asserts the health check result has a null map of details.
     *
     * @return this instance
     */
    public HealthCheckResultAssertions hasNullDetails() {
        Assertions.assertThat(result.getDetails())
                .describedAs("Expected null details")
                .isNull();

        return this;
    }

    /**
     * Asserts the health check result has an empty or null map of details.
     *
     * @return this instance
     */
    public HealthCheckResultAssertions hasNullOrEmptyDetails() {
        Assertions.assertThat(result.getDetails())
                .describedAs("Expected null or empty details")
                .isNullOrEmpty();

        return this;
    }

    /**
     * Asserts the health check result has a detail with the given key.
     *
     * @param key the expected key
     * @return this instance
     */
    public HealthCheckResultAssertions hasDetailsContainingKey(String key) {
        Assertions.assertThat(result.getDetails())
                .describedAs("Expected key not found in details")
                .containsKey(key);

        return this;
    }

    /**
     * Asserts the health check result has details containing all the given keys.
     *
     * @param keys the expected keys
     * @return this instance
     */
    public HealthCheckResultAssertions hasDetailsContainingKeys(String... keys) {
        Assertions.assertThat(result.getDetails())
                .describedAs("Expected keys not found in details")
                .containsKeys(keys);

        return this;
    }

    /**
     * Asserts the health check result has a detail with the given key and value.
     *
     * @param key   the expected key
     * @param value the expected value
     * @return this instance
     */
    public HealthCheckResultAssertions hasDetail(String key, Object value) {
        Assertions.assertThat(result.getDetails())
                .describedAs("Expected detail not found")
                .containsEntry(key, value);

        return this;
    }

    /**
     * Asserts the health check result does not contain the given key in its details.
     *
     * @param key the unexpected key
     * @return this instance
     */
    public HealthCheckResultAssertions doesNotHaveDetailsContainingKey(String key) {
        var details = result.getDetails();

        if (isNullOrEmpty(details)) {
            return this;
        }

        Assertions.assertThat(details)
                .describedAs("Expected details not to contain key '%s'", key)
                .doesNotContainKeys(key);

        return this;
    }

    /**
     * Asserts the health check result does not contain any of the given keys in its details.
     *
     * @param keys the unexpected keys
     * @return this instance
     */
    public HealthCheckResultAssertions doesNotHaveDetailsContainingKeys(String... keys) {
        var details = result.getDetails();

        if (isNullOrEmpty(details)) {
            return this;
        }

        var lazyKeysArg = lazy(() ->
                Arrays.stream(keys).map(key -> f("'{}'", key)).collect(joining(", ")));

        Assertions.assertThat(details)
                .describedAs("Expected details not to contain keys: %s", lazyKeysArg)
                .doesNotContainKeys(keys);

        return this;
    }
}
