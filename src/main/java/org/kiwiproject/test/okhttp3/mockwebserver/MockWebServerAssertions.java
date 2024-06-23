package org.kiwiproject.test.okhttp3.mockwebserver;

import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;

import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.Assertions;

import java.util.function.Consumer;

/**
 * Provides for fluent {@link MockWebServer} and {@link RecordedRequest} tests
 * using AssertJ assertions.
 * <p>
 * All methods return 'this' to facilitate a fluent API via method chaining.
 * <p>
 * Note that MockWebServer (com.squareup.okhttp3:mockwebserver) and OkHttp
 * dependencies must be available at runtime. OkHttp is a transitive dependency
 * of mockwebserver, so you should only need to add mockwebserver.
 */
public class MockWebServerAssertions {

    private final MockWebServer mockWebServer;

    private MockWebServerAssertions(MockWebServer mockWebServer) {
        this.mockWebServer = requireNotNull(mockWebServer);
    }

    public static MockWebServerAssertions assertThat(MockWebServer mockWebServer) {
        return assertThatMockWebServer(mockWebServer);
    }

    public static MockWebServerAssertions assertThatMockWebServer(MockWebServer mockWebServer) {
        return new MockWebServerAssertions(mockWebServer);
    }

    public MockWebServerAssertions hasRequestCount(int requestCount) {
        Assertions.assertThat(mockWebServer.getRequestCount())
                .describedAs("Expected request count to be: %d", requestCount)
                .isEqualTo(requestCount);

        return this;
    }

    public MockWebServerAssertions hasRecordedRequest(Consumer<RecordedRequest> recordedRequestConsumer) {
        var requestOrNull = RecordedRequests.takeRequestOrNull(mockWebServer);
        recordedRequestConsumer.accept(requestOrNull);

        return this;
    }

    public RecordedRequestAssertions recordedRequest() {
        var requestOrNull = RecordedRequests.takeRequestOrNull(mockWebServer);
        return RecordedRequestAssertions.assertThatRecordedRequest(requestOrNull);
    }
}
