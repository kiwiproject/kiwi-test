package org.kiwiproject.test.jaxrs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kiwiproject.collect.KiwiLists.first;
import static org.kiwiproject.collect.KiwiLists.second;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import org.glassfish.jersey.logging.LoggingFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.MemoryHandler;

@DisplayName("RequestResponseLogger")
@ExtendWith(DropwizardExtensionsSupport.class)
class RequestResponseLoggerTest {

    private static final String SENDING_CLIENT_REQUEST = "Sending client request";
    private static final String CLIENT_RESPONSE_RECEIVED = "Client response received";
    private static final String CHECK_CLIENT_LOGGING_FILTER =
            "If you are seeing this, then the (package-private) org.glassfish.jersey.logging.ClientLoggingFilter may" +
                    " have changed its log messages! The SENDING_CLIENT_REQUEST and/or CLIENT_RESPONSE_RECEIVED" +
                    " constants need to be updated.";

    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public static class RequestResponseLoggerTestResource {

        @GET
        @Path("/foo")
        public Response get() {
            var entity = Map.of("foo", "bar");
            return Response.ok(entity).build();
        }
    }

    private static final ResourceExtension RESOURCES = ResourceExtension.builder()
            .bootstrapLogging(false)
            .addResource(new RequestResponseLoggerTestResource())
            .build();

    private Client client;
    private SimpleTestLogHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SimpleTestLogHandler();
        var memoryHandler = new MemoryHandler(handler, 1, Level.FINEST);

        client = RESOURCES.client();
        RequestResponseLogger.turnOnRequestResponseLogging(client,
                Level.INFO,
                LoggingFeature.Verbosity.PAYLOAD_ANY,
                "MyLogger",
                memoryHandler,
                4096);
    }

    @Test
    void shouldLogRequestsAndResponses() {
        client.target("/test/foo").request().get();

        var records = handler.getRecords();
        assertThat(records).hasSize(2);
        assertThat(records).extracting(LogRecord::getLoggerName).containsOnly("MyLogger");

        var messages = records.stream().map(LogRecord::getMessage).toList();
        assertThat(first(messages)).describedAs(CHECK_CLIENT_LOGGING_FILTER).contains(SENDING_CLIENT_REQUEST);
        assertThat(second(messages)).describedAs(CHECK_CLIENT_LOGGING_FILTER).contains(CLIENT_RESPONSE_RECEIVED);

        client.target("/test/foo").request().get();
        assertThat(records)
                .describedAs("Should have added two more records")
                .hasSize(4);
        var latestMessages = records.stream()
                .skip(2)
                .map(LogRecord::getMessage).toList();
        assertThat(first(latestMessages)).describedAs(CHECK_CLIENT_LOGGING_FILTER).contains(SENDING_CLIENT_REQUEST);
        assertThat(second(latestMessages)).describedAs(CHECK_CLIENT_LOGGING_FILTER).contains(CLIENT_RESPONSE_RECEIVED);
    }

    /**
     * Simplified from the private {@code JerseyTestLogHandler} inside {@link org.glassfish.jersey.test.JerseyTest}
     */
    private static class SimpleTestLogHandler extends Handler {

        @Getter
        private final List<LogRecord> records;

        private SimpleTestLogHandler() {
            this.records = new ArrayList<>();
        }

        @Override
        public void publish(final LogRecord logRecord) {
            records.add(logRecord);
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void close() throws SecurityException {
            // no-op
        }
    }
}
