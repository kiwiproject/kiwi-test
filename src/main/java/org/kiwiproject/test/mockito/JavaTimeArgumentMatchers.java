package org.kiwiproject.test.mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kiwiproject.base.KiwiPreconditions.checkArgumentNotNull;
import static org.kiwiproject.base.KiwiPreconditions.checkPositive;
import static org.kiwiproject.logging.LazyLogParameterSupplier.lazy;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.mockito.ArgumentMatcher;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Static utility methods to create {@link ArgumentMatcher} instances for types in the {@code java.time} package.
 */
@UtilityClass
@Slf4j
public class JavaTimeArgumentMatchers {

    private static final int DEFAULT_SLACK_MILLIS = 500;
    private static final String EXPECTED_TIME_CANNOT_BE_NULL = "expectedTime cannot be null";
    private static final String SLACK_CANNOT_BE_NULL = "slack cannot be null";

    /**
     * Matches an {@link ZonedDateTime} near the given expected time within +/- 500 milliseconds.
     * <p>
     * In other words, the actual time must be in the range {@code [ expectedTime - 500ms , expectedTime + 500ms ]}.
     * <p>
     * Example usage:
     * <pre>
     * // Assume a Timekeeper class contains an announceTime(ZonedDateTime time) method
     * var expectedTime = determineExpectedZonedDateTime();
     * verify(timekeeper).announceTime(argThat(isNear(expectedTime));
     * </pre>
     *
     * @param expectedTime the expected time
     * @return a new {@link ArgumentMatcher} instance
     */
    public static ArgumentMatcher<ZonedDateTime> isNear(ZonedDateTime expectedTime) {
        return isNear(expectedTime, DEFAULT_SLACK_MILLIS);
    }

    /**
     * Matches an {@link ZonedDateTime} near the given expected time within +/- {@code slack} duration.
     * <p>
     * In other words, the actual time must be in the range {@code [ expectedTime - slack , expectedTime + slack ]}.
     * <p>
     * Example usage:
     * <pre>
     * // Assume a Timekeeper class contains an announceTime(ZonedDateTime time) method
     * var expectedTime = determineExpectedZonedDateTime();
     * var slack = Duration.ofMillis(250)
     * verify(timekeeper).announceTime(argThat(isNear(expectedTime, slack));
     * </pre>
     *
     * @param expectedTime the expected time
     * @param slack        the amount of time the actual time can differ before or after the expected time
     * @return a new {@link ArgumentMatcher} instance
     */
    public static ArgumentMatcher<ZonedDateTime> isNear(ZonedDateTime expectedTime, Duration slack) {
        checkArgumentNotNull(slack, SLACK_CANNOT_BE_NULL);
        return isNear(expectedTime, slack.toMillis());
    }

    /**
     * Matches an {@link ZonedDateTime} near the given expected time within +/- {@code slack} milliseconds.
     * <p>
     * In other words, the actual time must be in the range {@code [ expectedTime - slackMillis , expectedTime + slackMillis ]}.
     * <pre>
     * // Assume a Timekeeper class contains an announceTime(ZonedDateTime time) method
     * var expectedTime = determineExpectedZonedDateTime();
     * var slackMillis = 300;
     * verify(timekeeper).announceTime(argThat(isNear(expectedTime, slackMillis));
     * </pre>
     *
     * @param expectedTime the expected time
     * @param slackMillis  the number of milliseconds the actual time can differ before or after the expected time
     * @return a new {@link ArgumentMatcher} instance
     */
    public static ArgumentMatcher<ZonedDateTime> isNear(ZonedDateTime expectedTime, long slackMillis) {
        checkArgumentNotNull(expectedTime, EXPECTED_TIME_CANNOT_BE_NULL);
        checkPositive(slackMillis);

        return actualTime -> {
            LOG.trace("expectedTime: {} ; actualTime: {} ; Duration.between(expectedTimeTime, actualTime): {}",
                    expectedTime,
                    actualTime,
                    lazy(() -> Duration.between(expectedTime, actualTime)));

            var lowerBound = expectedTime.minus(slackMillis, ChronoUnit.MILLIS);
            var upperBound = expectedTime.plus(slackMillis, ChronoUnit.MILLIS);

            assertThat(actualTime)
                    .describedAs("actual time %s not between [ %s, %s ]", actualTime, lowerBound, upperBound)
                    .isBetween(lowerBound, upperBound);

            return true;
        };
    }

    /**
     * Matches an {@link Instant} near the given expected time within +/- 500 milliseconds.
     * <p>
     * In other words, the actual time must be in the range {@code [ expectedTime - 500ms , expectedTime + 500ms ]}.
     * <p>
     * Example usage:
     * <pre>
     * // Assume a Timekeeper class contains an announceTime(Instant time) method
     * var expectedInstant = determineExpectedInstant();
     * verify(timekeeper).announceTime(argThat(isNear(expectedInstant));
     * </pre>
     *
     * @param expectedTime the expected time
     * @return a new {@link ArgumentMatcher} instance
     */
    public static ArgumentMatcher<Instant> isNear(Instant expectedTime) {
        return isNear(expectedTime, DEFAULT_SLACK_MILLIS);
    }

    /**
     * Matches an {@link Instant} near the given expected time within +/- {@code slack} duration.
     * <p>
     * In other words, the actual time must be in the range {@code [ expectedTime - slack , expectedTime + slack ]}.
     * <p>
     * Example usage:
     * <pre>
     * // Assume a Timekeeper class contains an announceTime(Instant time) method
     * var expectedInstant = determineExpectedInstant();
     * var slack = Duration.ofMillis(250)
     * verify(timekeeper).announceTime(argThat(isNear(expectedInstant, slack));
     * </pre>
     *
     * @param expectedTime the expected time
     * @param slack        the amount of time the actual time can differ before or after the expected time
     * @return a new {@link ArgumentMatcher} instance
     */
    public static ArgumentMatcher<Instant> isNear(Instant expectedTime, Duration slack) {
        checkArgumentNotNull(slack, SLACK_CANNOT_BE_NULL);

        return isNear(expectedTime, slack.toMillis());
    }

    /**
     * Matches an {@link Instant} near the given expected time within +/- {@code slack} milliseconds.
     * <p>
     * In other words, the actual time must be in the range {@code [ expectedTime - slackMillis , expectedTime + slackMillis ]}.
     * <pre>
     * // Assume a Timekeeper class contains an announceTime(Instant time) method
     * var expectedInstant = determineExpectedInstant();
     * var slackMillis = 300;
     * verify(timekeeper).announceTime(argThat(isNear(expectedInstant, slackMillis));
     * </pre>
     *
     * @param expectedTime the expected time
     * @param slackMillis  the number of milliseconds the actual time can differ before or after the expected time
     * @return a new {@link ArgumentMatcher} instance
     */
    public static ArgumentMatcher<Instant> isNear(Instant expectedTime, long slackMillis) {
        checkArgumentNotNull(expectedTime, EXPECTED_TIME_CANNOT_BE_NULL);
        checkPositive(slackMillis);

        return actualTime -> {
            LOG.trace("expectedTime: {} ; actualTime: {} ; Duration.between(expectedTimeTime, actualTime): {}",
                    expectedTime,
                    actualTime,
                    lazy(() -> Duration.between(expectedTime, actualTime)));

            var lowerBound = expectedTime.minusMillis(slackMillis);
            var upperBound = expectedTime.plusMillis(slackMillis);

            assertThat(actualTime)
                    .describedAs("actual time %s not between [ %s, %s ]", actualTime, lowerBound, upperBound)
                    .isBetween(lowerBound, upperBound);

            return true;
        };
    }
}
