package org.kiwiproject.test.logback;

import static java.util.Comparator.comparing;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.common.annotations.Beta;
import lombok.Synchronized;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * A logback appender that stores logging events in an in-memory map.
 * The events can be accessed, ordered, and cleared.
 * <p>
 * <em>This is for testing purposes only, and is not at all intended for production use!</em>
 * <p>
 * <strong>Migration from kiwi-beta:</strong>
 * If you are migrating from the InMemoryAppender in kiwi-beta, note that its
 * {@code assertNumberOfLoggingEventsAndGet} method does not exist in this class.
 * Instead, you can get the same behavior using {@link InMemoryAppenderAssertions}.
 * Specifically, you can use {@link InMemoryAppenderAssertions#hasNumberOfLoggingEvents(int)}
 * or {@link InMemoryAppenderAssertions#hasNumberOfLoggingEventsAndGet(int)} if you also
 * need to get the list of events.
 */
@Beta
public class InMemoryAppender extends AppenderBase<ILoggingEvent> {

    private final AtomicInteger messageOrder;
    private final ConcurrentMap<Integer, ILoggingEvent> eventMap;

    /**
     * Create a new InMemoryAppender with no messages.
     */
    public InMemoryAppender() {
        this.messageOrder = new AtomicInteger();
        this.eventMap = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Synchronized
    protected void append(ILoggingEvent eventObject) {
        eventMap.put(messageOrder.incrementAndGet(), eventObject);
    }

    /**
     * Clears all logging events from this appender.
     */
    @Synchronized
    public void clearEvents() {
        messageOrder.set(0);
        eventMap.clear();
    }

    /**
     * Return a copy of the internal event map.
     * <p>
     * The keys are the message order starting at one, and the values are the corresponding logging events.
     *
     * @return an unmodifiable copy of the event map
     */
    @SuppressWarnings("unused")
    public Map<Integer, ILoggingEvent> eventMap() {
        return Map.copyOf(eventMap);
    }

    /**
     * Retrieves a list of logging events ordered by ascending timestamp.
     *
     * @return the ordered list of logging events
     */
    public List<ILoggingEvent> orderedEvents() {
        return orderedEventStream().toList();
    }

    /**
     * Retrieves a list of logged messages ordered by ascending timestamp.
     *
     * @return the ordered list of logged messages
     */
    @SuppressWarnings("unused")
    public List<String> orderedEventMessages() {
        return orderedEventStream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    /**
     * Retrieves a stream of logging events ordered by ascending timestamp.
     *
     * @return the ordered stream of logged messages
     */
    public Stream<ILoggingEvent> orderedEventStream() {
        return eventMap.values()
                .stream()
                .sorted(comparing(ILoggingEvent::getTimeStamp));
    }
}
