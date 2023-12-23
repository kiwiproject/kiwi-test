package org.kiwiproject.test.logback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.kiwiproject.collect.KiwiLists.first;
import static org.kiwiproject.test.logback.InMemoryAppenderAssertions.assertThatAppender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

@DisplayName("InMemoryAppender")
@Slf4j
class InMemoryAppenderTest {

    @RegisterExtension
    private final InMemoryAppenderExtension inMemoryAppenderExtension =
            new InMemoryAppenderExtension(InMemoryAppenderTest.class);

    private InMemoryAppender appender;

    @BeforeEach
    void setUp() {
        appender = inMemoryAppenderExtension.appender();
    }

    @Test
    void shouldAppendAnEvent() {
        LOG.info("Hello, logger");

        assertThat(appender.orderedEvents()).hasSize(1);
        var event = first(appender.orderedEvents());
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage()).isEqualTo("Hello, logger");
    }

    @Nested
    class GetEventMap {

        @BeforeEach
        void setUp() {
            LOG.info("Message A");
            LOG.debug("Message B");
            LOG.warn("Message C");
            LOG.error("Message D");
        }

        @Test
        void shouldReturnEvents() {
            var eventMap = appender.eventMap();

            assertThat(eventMap.get(1).getFormattedMessage()).isEqualTo("Message A");
            assertThat(eventMap.get(2).getFormattedMessage()).isEqualTo("Message B");
            assertThat(eventMap.get(3).getFormattedMessage()).isEqualTo("Message C");
            assertThat(eventMap.get(4).getFormattedMessage()).isEqualTo("Message D");
        }

        @Test
        void shouldReturnUnmodifiableCopyOfEventMap() {
            var eventMap = appender.eventMap();

            var newEvent = new LoggingEvent();
            assertThatThrownBy(() -> eventMap.put(2, newEvent)).isExactlyInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void shouldReturnCopyOfEventMap() {
            var originalEventMap = appender.eventMap();

            LOG.info("Another message");
            LOG.debug("And another");

            var newEventMap = appender.eventMap();
            assertThat(newEventMap).hasSize(originalEventMap.size() + 2);
        }
    }

    @Test
    void shouldReturnEventsInOrderTheyWereLogged() {
        LOG.info("Message 1");
        LOG.debug("Message 2");
        LOG.warn("Message 3");

        assertThat(appender.orderedEvents())
                .extracting("level", "formattedMessage")
                .containsExactly(
                        tuple(Level.INFO, "Message 1"),
                        tuple(Level.DEBUG, "Message 2"),
                        tuple(Level.WARN, "Message 3")
                );
    }

    @Test
    void shouldReturnOrderedLogMessages() {
        LOG.info("Message 1");
        LOG.debug("Message 2");
        LOG.warn("Message 3");
        LOG.error("Message 4");

        assertThat(appender.orderedEventMessages()).containsExactly(
                "Message 1",
                "Message 2",
                "Message 3",
                "Message 4"
        );
    }

    @Test
    void shouldClearAllLoggingEvents() {
        IntStream.range(0, 10).forEach(i -> LOG.debug("Message {}", i));
        assertThat(appender.orderedEvents()).hasSize(10);

        appender.clearEvents();

        assertThat(appender.orderedEvents()).isEmpty();
    }

    @Nested
    class EventAssertions {

        @Test
        void shouldAssertWhenEmpty() {
            var events = assertThatAppender(appender).hasNumberOfLoggingEventsAndGet(0);
            assertThat(events).isEmpty();
        }

        @Test
        void shouldAssertWhenContainsLoggingEvents() {
            var messages = IntStream.range(0, 5).mapToObj(i -> "Message " + i).toArray(String[]::new);
            Arrays.stream(messages).forEach(LOG::debug);

            var eventsRef = new AtomicReference<List<ILoggingEvent>>();
            assertThatCode(() -> {
                var loggingEvents = assertThatAppender(appender).hasNumberOfLoggingEventsAndGet(5);
                eventsRef.set(loggingEvents);
            }).doesNotThrowAnyException();

            var events = eventsRef.get();
            assertThat(events).extracting(ILoggingEvent::getFormattedMessage).containsExactly(messages);
        }

        @Test
        void shouldAssertNumberOfLoggingEventsOnly() {
            var messages = IntStream.range(0, 5).mapToObj(i -> "Message " + i).toArray(String[]::new);
            Arrays.stream(messages).forEach(LOG::debug);

            assertThatCode(() -> assertThatAppender(appender).hasNumberOfLoggingEventsAndGet(5))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldGetOrderedLoggingEventsOnly() {
            var messages = IntStream.range(0, 5).mapToObj(i -> "Message " + i).toArray(String[]::new);
            Arrays.stream(messages).forEach(LOG::debug);

            List<ILoggingEvent> events = assertThatAppender(appender).andGetOrderedEvents();
            assertThat(events).extracting(ILoggingEvent::getFormattedMessage).containsExactly(messages);
        }

        @Test
        void shouldCheckContainsMessageWhenItExists() {
            var messages = IntStream.range(0, 5).mapToObj(i -> "Message " + i).toArray(String[]::new);
            Arrays.stream(messages).forEach(LOG::debug);

            assertThatCode(() -> assertThatAppender(appender).containsMessage("Message 0").containsMessage("Message 4"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCheckContainsMessageWhenItDoesNotExist() {
            assertThatExceptionOfType(AssertionError.class)
                    .isThrownBy(() -> assertThatAppender(appender).containsMessage("Message 0"));
        }
    }
}
