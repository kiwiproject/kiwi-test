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

@UtilityClass
@Slf4j
public class JavaTimeArgumentMatchers {

    private static final int DEFAULT_SLACK_MILLIS = 500;

    public static ArgumentMatcher<Instant> isNear(Instant expectedTime) {
        return isNear(expectedTime, DEFAULT_SLACK_MILLIS);
    }

    public static ArgumentMatcher<Instant> isNear(Instant expectedTime, Duration slack) {
        checkArgumentNotNull(slack, "slack cannot be null");

        return isNear(expectedTime, slack.toMillis());
    }

    public static ArgumentMatcher<Instant> isNear(Instant expectedTime, long slackMillis) {
        checkArgumentNotNull(expectedTime, "expectedTime cannot be null");
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
