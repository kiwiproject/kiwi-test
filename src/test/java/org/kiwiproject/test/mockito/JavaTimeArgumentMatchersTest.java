package org.kiwiproject.test.mockito;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.kiwiproject.test.mockito.JavaTimeArgumentMatchers.isNear;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@DisplayName("JavaTimeArgumentMatchers")
class JavaTimeArgumentMatchersTest {

    @Nested
    class IsNearInstant {

        private Timekeeper timekeeper;

        @BeforeEach
        void setUp() {
            timekeeper = mock(Timekeeper.class);
            when(timekeeper.announce(any(Instant.class))).thenReturn("The time is now 2020-11-06T02:12:26.890880Z");
        }

        @Test
        void shouldNotAllowNullInstant() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> JavaTimeArgumentMatchers.isNear((Instant) null));

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> JavaTimeArgumentMatchers.isNear((Instant) null, Duration.ofMillis(100)));

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> JavaTimeArgumentMatchers.isNear((Instant) null, 500));
        }

        @Test
        void shouldNotAllowNullSlack() {
            var now = Instant.now();
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> JavaTimeArgumentMatchers.isNear(now, null));
        }

        @ParameterizedTest
        @ValueSource(longs = {-100, -1, 0})
        void shouldRequirePositiveSlackMillis(long slackMillis) {
            var now = Instant.now();
            assertThatIllegalStateException()
                    .isThrownBy(() -> JavaTimeArgumentMatchers.isNear(now, slackMillis));
        }

        @Nested
        class UsingDefaultSlack {

            @ParameterizedTest
            @ValueSource(longs = {-500, -250, 0, 250, 500})
            void shouldSucceed_WhenWithinExpectedTime_PlusOrMinusSlack(long millis) {
                var anInstant = callAnnounce(millis);

                assertThatCode(() -> verify(timekeeper).announce(argThat(isNear(anInstant))))
                        .doesNotThrowAnyException();
            }

            @ParameterizedTest
            @ValueSource(longs = {-1000, -501, 501, 1000})
            void shouldFail_WhenOutsideExpectedTime_PlusOrMinusSlack(long millis) {
                var anInstant = callAnnounce(millis);

                assertThatThrownBy(() -> verify(timekeeper).announce(argThat(isNear(anInstant))))
                        .isInstanceOf(AssertionError.class);
            }
        }

        @Nested
        class UsingCustomSlack {

            @ParameterizedTest
            @CsvSource({
                    "-1000, 1000",
                    "-750, 750",
                    "-100, 100",
                    "-50, 100",
                    "0, 250",
                    "50, 100",
                    "100, 100",
                    "300, 500",
                    "750, 750",
                    "750, 1000",
            })
            void shouldSucceed_WhenWithinExpectedTime_PlusOrMinusSlack(long millis, long slackMillis) {
                var anInstant = callAnnounce(millis);

                var slack = Duration.ofMillis(slackMillis);
                assertThatCode(() -> verify(timekeeper).announce(argThat(isNear(anInstant, slack))))
                        .doesNotThrowAnyException();
            }

            @ParameterizedTest
            @CsvSource({
                    "-1001, 1000",
                    "-751, 750",
                    "-101, 100",
                    "101, 100",
                    "301, 300",
                    "751, 750",
                    "1001, 1000",
            })
            void shouldFail_WhenOutsideExpectedTime_PlusOrMinusSlack(long millis, long slackMillis) {
                var anInstant = callAnnounce(millis);

                var slack = Duration.ofMillis(slackMillis);
                assertThatThrownBy(() -> verify(timekeeper).announce(argThat(isNear(anInstant, slack))))
                        .isInstanceOf(AssertionError.class);
            }
        }

        // Call util.announce with the argument as (now + millisToAdd)
        private Instant callAnnounce(long millisToAdd) {
            var anInstant = Instant.now();
            timekeeper.announce(anInstant.plusMillis(millisToAdd));
            return anInstant;
        }
    }

    @Nested
    class IsNearZonedDateTime {

        private Timekeeper timekeeper;

        @BeforeEach
        void setUp() {
            timekeeper = mock(Timekeeper.class);
            when(timekeeper.announce(any(ZonedDateTime.class)))
                    .thenReturn("The time is now 2022-07-10T16:28:08.934789-04:00[America/New_York]");
        }

        @Test
        void shouldNotAllowNullZonedDateTime() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> JavaTimeArgumentMatchers.isNear((ZonedDateTime) null));

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> JavaTimeArgumentMatchers.isNear((ZonedDateTime) null, Duration.ofMillis(250)));

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> JavaTimeArgumentMatchers.isNear((ZonedDateTime) null, 500));
        }

        @Test
        void shouldNotAllowNullSlack() {
            var now = ZonedDateTime.now();
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> JavaTimeArgumentMatchers.isNear(now, null));
        }

        @ParameterizedTest
        @ValueSource(longs = {-100, -1, 0})
        void shouldRequirePositiveSlackMillis(long slackMillis) {
            var now = ZonedDateTime.now();
            assertThatIllegalStateException()
                    .isThrownBy(() -> JavaTimeArgumentMatchers.isNear(now, slackMillis));
        }

        @Nested
        class UsingDefaultSlack {

            @ParameterizedTest
            @ValueSource(longs = {-500, -250, 0, 250, 500})
            void shouldSucceed_WhenWithinExpectedTime_PlusOrMinusSlack(long millis) {
                var zonedDateTime = callAnnounce(millis);

                assertThatCode(() -> verify(timekeeper).announce(argThat(isNear(zonedDateTime))))
                        .doesNotThrowAnyException();
            }

            @ParameterizedTest
            @ValueSource(longs = {-1000, -501, 501, 1000})
            void shouldFail_WhenOutsideExpectedTime_PlusOrMinusSlack(long millis) {
                var zonedDateTime = callAnnounce(millis);

                assertThatThrownBy(() -> verify(timekeeper).announce(argThat(isNear(zonedDateTime))))
                        .isInstanceOf(AssertionError.class);
            }
        }

        @Nested
        class UsingCustomSlack {

            @ParameterizedTest
            @CsvSource({
                    "-1000, 1000",
                    "-750, 750",
                    "-100, 100",
                    "-50, 100",
                    "0, 250",
                    "50, 100",
                    "100, 100",
                    "300, 500",
                    "750, 750",
                    "750, 1000",
            })
            void shouldSucceed_WhenWithinExpectedTime_PlusOrMinusSlack(long millis, long slackMillis) {
                var zonedDateTime = callAnnounce(millis);

                var slack = Duration.ofMillis(slackMillis);
                assertThatCode(() -> verify(timekeeper).announce(argThat(isNear(zonedDateTime, slack))))
                        .doesNotThrowAnyException();
            }

            @ParameterizedTest
            @CsvSource({
                    "-1001, 1000",
                    "-751, 750",
                    "-101, 100",
                    "101, 100",
                    "301, 300",
                    "751, 750",
                    "1001, 1000",
            })
            void shouldFail_WhenOutsideExpectedTime_PlusOrMinusSlack(long millis, long slackMillis) {
                var zonedDateTime = callAnnounce(millis);

                var slack = Duration.ofMillis(slackMillis);
                assertThatThrownBy(() -> verify(timekeeper).announce(argThat(isNear(zonedDateTime, slack))))
                        .isInstanceOf(AssertionError.class);
            }
        }

        // Call util.announce with the argument as (now + millisToAdd)
        private ZonedDateTime callAnnounce(long millisToAdd) {
            var now = ZonedDateTime.now();
            timekeeper.announce(now.plus(millisToAdd, ChronoUnit.MILLIS));
            return now;
        }
    }

    static class Timekeeper {

        String announce(Instant anInstant) {
            return "The time is now " + anInstant.toString();
        }

        String announce(ZonedDateTime zonedDateTime) {
            return "The time is now " + zonedDateTime.toString();
        }
    }
}
