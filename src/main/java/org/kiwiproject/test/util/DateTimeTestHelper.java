package org.kiwiproject.test.util;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * A test helper that makes assertions on date/time values, for example assertions that the elapsed time
 * is less than a threshold.
 * <p>
 * The usage for the time difference assertions expects the start time to be recorded locally, e.g. within a
 * test before issuing a database update, and the end time to be a timestamp assigned by a remote server. For
 * example, timestamp fields in a database that automatically update when a record is changed. Once a test receives
 * the updated object, in this example the object with the automatically updated timestamp field, that updated field
 * becomes the end time in the assertion methods in this test helper. Here is an example:
 * <pre>
 * // inside a test method assuming there is a someUser object...
 * var beforeUpdate = Instant.now();
 * var updatedUser = userDao.update(someUser);
 *
 * DateTimeTestHelper.assertTimeDifferenceWithinTolerance("updatedAt", beforeUpdate, updatedUser.getUpdatedAt());
 * </pre>
 * Why would you ever do this? In the above example it's just a sanity check to make sure we are updating the audit
 * trail property {@code updatedAt}. And alas, too many times we've seen someone forget to do this (and that includes
 * ourselves of course).
 *
 * @implNote Time difference assertions are made on the absolute time difference, to account for the possibility that a
 * remote machine assigning timestamps (e.g. a database) might have a clock time that is not exactly in sync with the
 * machine running a test. We would hope they are all synchronized but have learned from experience not to always
 * trust this to be the case.
 */
@Slf4j
@UtilityClass
public class DateTimeTestHelper {

    /**
     * The default permitted tolerance in milliseconds between a start and end time.
     */
    public static final long DEFAULT_TOLERANCE_MILLIS = 500;
    private static final String DELTA_MORE_THAN_TOLERANCE_MESSAGE = "%s delta of %d ms is more than tolerance %d ms. Start: %s (%d epoch ms), end: %s (%d epoch ms)";

    /**
     * Asserts that the elapsed time between the given start and end times is less than
     * {@link #DEFAULT_TOLERANCE_MILLIS}.
     *
     * @param property  the property name; used only in failed assertion messages
     * @param startTime the start time
     * @param endTime   the end time
     */
    public static void assertTimeDifferenceWithinTolerance(String property,
                                                           ZonedDateTime startTime,
                                                           ZonedDateTime endTime) {
        assertTimeDifferenceWithinTolerance(property, startTime, endTime, DEFAULT_TOLERANCE_MILLIS);
    }

    /**
     * Softly asserts that the elapsed time between the given start and end times is less than
     * {@link #DEFAULT_TOLERANCE_MILLIS}.
     *
     * @param softAssertions the {@link SoftAssertions} to use
     * @param property       the property name; used only in failed assertion messages
     * @param startTime      the start time
     * @param endTime        the end time
     */
    public static void assertTimeDifferenceWithinTolerance(SoftAssertions softAssertions,
                                                           String property,
                                                           ZonedDateTime startTime,
                                                           ZonedDateTime endTime) {
        assertTimeDifferenceWithinTolerance(softAssertions, property, startTime, endTime, DEFAULT_TOLERANCE_MILLIS);
    }

    /**
     * Asserts that the elapsed time between the given start and end times is less than the given tolerance.
     *
     * @param property        the property name; used only in failed assertion messages
     * @param startTime       the start time
     * @param endTime         the end time
     * @param toleranceMillis the allowed maximum tolerance between start and end time
     */
    public static void assertTimeDifferenceWithinTolerance(String property,
                                                           ZonedDateTime startTime,
                                                           ZonedDateTime endTime,
                                                           long toleranceMillis) {
        assertTimeDifferenceWithinTolerance(property, startTime.toInstant(), endTime.toInstant(), toleranceMillis);
    }

    /**
     * Softly asserts that the elapsed time between the given start and end times is less than the given tolerance.
     *
     * @param softAssertions  the {@link SoftAssertions} to use
     * @param property        the property name; used only in failed assertion messages
     * @param startTime       the start time
     * @param endTime         the end time
     * @param toleranceMillis the allowed maximum tolerance between start and end time
     */
    public static void assertTimeDifferenceWithinTolerance(SoftAssertions softAssertions,
                                                           String property,
                                                           ZonedDateTime startTime,
                                                           ZonedDateTime endTime,
                                                           long toleranceMillis) {
        assertTimeDifferenceWithinTolerance(softAssertions,
                property,
                startTime.toInstant(),
                endTime.toInstant(),
                toleranceMillis);
    }

    /**
     * Asserts that the elapsed time between the given start and end times is less than
     * {@link #DEFAULT_TOLERANCE_MILLIS}.
     *
     * @param property  the property name; used only in failed assertion messages
     * @param startTime the start time
     * @param endTime   the end time
     */
    public static void assertTimeDifferenceWithinTolerance(String property, Instant startTime, Instant endTime) {
        assertTimeDifferenceWithinTolerance(property, startTime, endTime, DEFAULT_TOLERANCE_MILLIS);
    }

    /**
     * Softly asserts that the elapsed time between the given start and end times is less than
     * {@link #DEFAULT_TOLERANCE_MILLIS}.
     *
     * @param softAssertions the {@link SoftAssertions} to use
     * @param property       the property name; used only in failed assertion messages
     * @param startTime      the start time
     * @param endTime        the end time
     */
    public static void assertTimeDifferenceWithinTolerance(SoftAssertions softAssertions,
                                                           String property,
                                                           Instant startTime,
                                                           Instant endTime) {
        assertTimeDifferenceWithinTolerance(softAssertions, property, startTime, endTime, DEFAULT_TOLERANCE_MILLIS);
    }

    /**
     * Asserts that the elapsed time between the given start and end times is less than the given tolerance.
     *
     * @param property        the property name; used only in failed assertion messages
     * @param startTime       the start time
     * @param endTime         the end time
     * @param toleranceMillis the allowed maximum tolerance between start and end time
     */
    public static void assertTimeDifferenceWithinTolerance(String property,
                                                           Instant startTime,
                                                           Instant endTime,
                                                           long toleranceMillis) {
        logWarningIfEndBeforeStart(startTime, endTime);

        var startMillis = startTime.toEpochMilli();
        var endMillis = endTime.toEpochMilli();
        var delta = Math.abs(endMillis - startMillis);

        assertThat(delta)
                .describedAs(DELTA_MORE_THAN_TOLERANCE_MESSAGE,
                        property, delta, toleranceMillis,
                        utcZonedDateTimeOfInstant(startTime),
                        startMillis,
                        utcZonedDateTimeOfInstant(endTime),
                        endMillis)
                .isLessThanOrEqualTo(toleranceMillis);
    }

    /**
     * Softly asserts that the elapsed time between the given start and end times is less than the given tolerance.
     *
     * @param softAssertions  the {@link SoftAssertions} to use
     * @param property        the property name; used only in failed assertion messages
     * @param startTime       the start time
     * @param endTime         the end time
     * @param toleranceMillis the allowed maximum tolerance between start and end time
     */
    public static void assertTimeDifferenceWithinTolerance(SoftAssertions softAssertions,
                                                           String property,
                                                           Instant startTime,
                                                           Instant endTime,
                                                           long toleranceMillis) {
        logWarningIfEndBeforeStart(startTime, endTime);

        var startMillis = startTime.toEpochMilli();
        var endMillis = endTime.toEpochMilli();
        var delta = Math.abs(endMillis - startMillis);

        softAssertions.assertThat(delta)
                .describedAs(DELTA_MORE_THAN_TOLERANCE_MESSAGE,
                        property, delta, toleranceMillis,
                        utcZonedDateTimeOfInstant(startTime),
                        startMillis,
                        utcZonedDateTimeOfInstant(endTime),
                        endMillis)
                .isLessThanOrEqualTo(toleranceMillis);
    }

    private static void logWarningIfEndBeforeStart(Instant startTime, Instant endTime) {
        if (endTime.isBefore(startTime)) {
            var diffInMillis = Duration.between(endTime, startTime).toMillis();
            LOG.warn("End time({}) is before start time ({}) by {} ms. The remote server assigning times for timestamp" +
                            " fields probably has a clock time that is slightly before the machine running this test.",
                    utcZonedDateTimeOfInstant(endTime),
                    utcZonedDateTimeOfInstant(startTime),
                    diffInMillis);
        }
    }

    private static ZonedDateTime utcZonedDateTimeOfInstant(Instant instant) {
        return ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

}
