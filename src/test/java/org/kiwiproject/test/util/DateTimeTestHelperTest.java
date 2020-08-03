package org.kiwiproject.test.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentest4j.MultipleFailuresError;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@DisplayName("DateTimeTestHelper")
class DateTimeTestHelperTest {

    private static final String PROPERTY = "createdAt";

    @Nested
    class WhenRegularAssertions {

        @Nested
        class ShouldPass {

            @ParameterizedTest
            @ValueSource(longs = {100, 499, 500})
            void whenZonedDateTimesWithinDefaultTolerance(long value) {
                var now = ZonedDateTime.now();
                var later = now.plus(value, ChronoUnit.MILLIS);

                assertThatCode(() -> DateTimeTestHelper.assertTimeDifferenceWithinTolerance(PROPERTY, now, later))
                        .doesNotThrowAnyException();
            }

            @ParameterizedTest
            @ValueSource(longs = {100, 499, 500})
            void whenInstantsWithinDefaultTolerance(long value) {
                var now = Instant.now();
                var later = now.plusMillis(value);

                assertThatCode(() -> DateTimeTestHelper.assertTimeDifferenceWithinTolerance(PROPERTY, now, later))
                        .doesNotThrowAnyException();
            }

            @ParameterizedTest
            @ValueSource(longs = {50, 99, 100})
            void whenZonedDateTimesWithinCustomTolerance(long value) {
                var now = ZonedDateTime.now();
                var later = now.plus(value, ChronoUnit.MILLIS);
                var customTolerance = 100;

                assertThatCode(() -> DateTimeTestHelper.assertTimeDifferenceWithinTolerance(PROPERTY, now, later, customTolerance))
                        .doesNotThrowAnyException();
            }

            @ParameterizedTest
            @ValueSource(longs = {50, 99, 100})
            void whenInstantsWithinCustomTolerance(long value) {
                var now = Instant.now();
                var later = now.plusMillis(value);
                var customTolerance = 100;

                assertThatCode(() -> DateTimeTestHelper.assertTimeDifferenceWithinTolerance(PROPERTY, now, later, customTolerance))
                        .doesNotThrowAnyException();
            }

            // This test doesn't actually check the log message; it only verifies that when this case occurs and the
            // time difference is within tolerance, no exception is throw. Manual verification of the log message is the
            // only way to verify the actual log message right now.
            @ParameterizedTest
            @ValueSource(longs = {1, 100, 250, 10_000, 100_000})
            void whenEndTimeIsBeforeStartTime(long value) {
                var now = Instant.now();
                var endThatIsBeforeNow = now.minusMillis(value);

                var toleranceMillis = value * 2;
                assertThatCode(() ->
                        DateTimeTestHelper.assertTimeDifferenceWithinTolerance(PROPERTY, now, endThatIsBeforeNow, toleranceMillis))
                        .doesNotThrowAnyException();
            }
        }

        @Nested
        class ShouldFail {

            @ParameterizedTest
            @ValueSource(longs = {501, 550, 900})
            void whenZonedDateTimesOutsideDefaultTolerance(long value) {
                var now = ZonedDateTime.now();
                var later = now.plus(value, ChronoUnit.MILLIS);

                assertThatThrownBy(() ->
                        DateTimeTestHelper.assertTimeDifferenceWithinTolerance(PROPERTY, now, later))
                        .isExactlyInstanceOf(AssertionError.class);
            }

            @ParameterizedTest
            @ValueSource(longs = {501, 550, 900})
            void whenInstantsOutsideDefaultTolerance(long value) {
                var now = Instant.now();
                var later = now.plusMillis(value);

                assertThatThrownBy(() ->
                        DateTimeTestHelper.assertTimeDifferenceWithinTolerance(PROPERTY, now, later))
                        .isExactlyInstanceOf(AssertionError.class);
            }

            @ParameterizedTest
            @ValueSource(longs = {251, 300, 700})
            void whenZonedDateTimesOutsideCustomTolerance(long value) {
                var now = ZonedDateTime.now();
                var later = now.plus(value, ChronoUnit.MILLIS);
                var customTolerance = 250;

                assertThatThrownBy(() ->
                        DateTimeTestHelper.assertTimeDifferenceWithinTolerance(PROPERTY, now, later, customTolerance))
                        .isExactlyInstanceOf(AssertionError.class);
            }

            @ParameterizedTest
            @ValueSource(longs = {251, 300, 700})
            void whenInstantsOutsideCustomTolerance(long value) {
                var now = Instant.now();
                var later = now.plusMillis(value);
                var customTolerance = 250;

                assertThatThrownBy(() ->
                        DateTimeTestHelper.assertTimeDifferenceWithinTolerance(PROPERTY, now, later, customTolerance))
                        .isExactlyInstanceOf(AssertionError.class);
            }
        }
    }

    @Nested
    class WhenSoftAssertions {

        @Nested
        class ShouldPass {

            @ParameterizedTest
            @ValueSource(longs = {100, 499, 500})
            void whenZonedDateTimesWithinDefaultTolerance(long value) {
                var now = ZonedDateTime.now();
                var later = now.plus(value, ChronoUnit.MILLIS);

                assertThatCode(() ->
                        SoftAssertions.assertSoftly(softly ->
                                DateTimeTestHelper.assertTimeDifferenceWithinTolerance(softly, PROPERTY, now, later))
                ).doesNotThrowAnyException();
            }

            @ParameterizedTest
            @ValueSource(longs = {100, 499, 500})
            void whenInstantsWithinDefaultTolerance(long value) {
                var now = Instant.now();
                var later = now.plusMillis(value);

                assertThatCode(() ->
                        SoftAssertions.assertSoftly(softly ->
                                DateTimeTestHelper.assertTimeDifferenceWithinTolerance(softly, PROPERTY, now, later))
                ).doesNotThrowAnyException();
            }

            @ParameterizedTest
            @ValueSource(longs = {100, 749, 750})
            void whenZonedDateTimesWithinCustomTolerance(long value) {
                var now = ZonedDateTime.now();
                var later = now.plus(value, ChronoUnit.MILLIS);
                var customTolerance = 750;

                assertThatCode(() ->
                        SoftAssertions.assertSoftly(softly ->
                                DateTimeTestHelper.assertTimeDifferenceWithinTolerance(softly, PROPERTY, now, later, customTolerance))
                ).doesNotThrowAnyException();
            }

            @ParameterizedTest
            @ValueSource(longs = {100, 1249, 1250})
            void whenInstantsWithinCustomTolerance(long value) {
                var now = Instant.now();
                var later = now.plusMillis(value);
                var customTolerance = 1250;

                assertThatCode(() ->
                        SoftAssertions.assertSoftly(softly ->
                                DateTimeTestHelper.assertTimeDifferenceWithinTolerance(softly, PROPERTY, now, later, customTolerance))
                ).doesNotThrowAnyException();
            }
        }

        @Nested
        class ShouldFail {

            @ParameterizedTest
            @ValueSource(longs = {501, 550, 900})
            void whenZonedDateTimesOutsideDefaultTolerance(long value) {
                var now = ZonedDateTime.now();
                var later = now.plus(value, ChronoUnit.MILLIS);

                assertThatThrownBy(() ->
                        SoftAssertions.assertSoftly(softly ->
                                DateTimeTestHelper.assertTimeDifferenceWithinTolerance(softly, PROPERTY, now, later)))
                        .isInstanceOf(MultipleFailuresError.class);
            }

            @ParameterizedTest
            @ValueSource(longs = {501, 550, 900})
            void whenInstantsOutsideDefaultTolerance(long value) {
                var now = Instant.now();
                var later = now.plusMillis(value);

                assertThatThrownBy(() ->
                        SoftAssertions.assertSoftly(softly ->
                                DateTimeTestHelper.assertTimeDifferenceWithinTolerance(softly, PROPERTY, now, later)))
                        .isInstanceOf(MultipleFailuresError.class);
            }

            @ParameterizedTest
            @ValueSource(longs = {301, 350, 500})
            void whenZonedDateTimesOutsideCustomTolerance(long value) {
                var now = ZonedDateTime.now();
                var later = now.plus(value, ChronoUnit.MILLIS);
                var customTolerance = 300;

                assertThatThrownBy(() ->
                        SoftAssertions.assertSoftly(softly ->
                                DateTimeTestHelper.assertTimeDifferenceWithinTolerance(softly, PROPERTY, now, later, customTolerance)))
                        .isInstanceOf(MultipleFailuresError.class);
            }

            @ParameterizedTest
            @ValueSource(longs = {101, 125, 200})
            void whenInstantsOutsideCustomTolerance(long value) {
                var now = Instant.now();
                var later = now.plusMillis(value);
                var customTolerance = 100;

                assertThatThrownBy(() ->
                        SoftAssertions.assertSoftly(softly ->
                                DateTimeTestHelper.assertTimeDifferenceWithinTolerance(softly, PROPERTY, now, later, customTolerance)))
                        .isInstanceOf(MultipleFailuresError.class);
            }
        }
    }
}
