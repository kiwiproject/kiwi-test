package org.kiwiproject.test.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.annotations.Beta;
import org.assertj.core.api.Assertions;

import java.util.List;

/**
 * Provides AssertJ assertions for {@link InMemoryAppender}.
 */
@Beta
public class InMemoryAppenderAssertions {

    private final InMemoryAppender appender;

    private InMemoryAppenderAssertions(InMemoryAppender appender) {
        this.appender = appender;
    }

    /**
     * Begin assertions for an {@link InMemoryAppender}.
     *
     * @param appender the appender to assert against
     * @return a new InMemoryAppenderAssertions instance
     */
    public static InMemoryAppenderAssertions assertThat(InMemoryAppender appender) {
        return new InMemoryAppenderAssertions(appender);
    }

    /**
     * Assert this appender has the expected number of logging events, and if the assertion succeeds, return a
     * list containing those events.
     *
     * @return a List containing the logging events
     */
    public List<ILoggingEvent> hasNumberOfLoggingEventsAndGet(int expectedEventCount) {
        return hasNumberOfLoggingEvents(expectedEventCount).andGetOrderedEvents();
    }

    /**
     * Assert this appender has the expected number of logging events, and if the assertion succeeds, return this
     * instance to continue chaining assertions.
     *
     * @return this instance
     */
    public InMemoryAppenderAssertions hasNumberOfLoggingEvents(int expectedEventCount) {
        var events = appender.orderedEvents();
        Assertions.assertThat(events).hasSize(expectedEventCount);
        return this;
    }

    /**
     * Assert this appender contains the given message at least once (but possibly more than once).
     *
     * @param message the exact message to find
     * @return this instance
     */
    public InMemoryAppenderAssertions containsMessage(String message) {
        var messages = appender.orderedEvents().stream().map(ILoggingEvent::getMessage).toList();
        Assertions.assertThat(messages).contains(message);
        return this;
    }

    /**
     * A terminal method if you want to get the actual logging events after performing assertions, for example,
     * to perform additional inspections. Does not perform any assertions.
     *
     * @return a List containing the logging events
     */
    public List<ILoggingEvent> andGetOrderedEvents() {
        return appender.orderedEvents();
    }
}
