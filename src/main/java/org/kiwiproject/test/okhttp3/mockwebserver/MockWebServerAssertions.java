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

    /**
     * Starting point for fluent assertions on {@link MockWebServer}.
     *
     * @param mockWebServer the {@link MockWebServer} to assert upon
     * @return this instance
     */
    public static MockWebServerAssertions assertThat(MockWebServer mockWebServer) {
        return assertThatMockWebServer(mockWebServer);
    }

    /**
     * Starting point for fluent assertions on {@link MockWebServer}.
     * <p>
     * This method is provided as an alias of {@link #assertThat(MockWebServer)} to avoid conflicts
     * when statically importing AssertJ's {@code Assertions#assertThat}, and therefore allow both
     * to be statically imported.
     *
     * @param mockWebServer the {@link MockWebServer} to assert upon
     * @return this instance
     */
    public static MockWebServerAssertions assertThatMockWebServer(MockWebServer mockWebServer) {
        return new MockWebServerAssertions(mockWebServer);
    }

    /**
     * Asserts the {@link MockWebServer} has the expected request count.
     *
     * @param requestCount the expected request count
     * @return this instance
     */
    public MockWebServerAssertions hasRequestCount(int requestCount) {
        Assertions.assertThat(mockWebServer.getRequestCount())
                .describedAs("Expected request count to be: %d", requestCount)
                .isEqualTo(requestCount);

        return this;
    }

    /**
     * Asserts the {@link MockWebServer} has the next {@link RecordedRequest} that satisfies
     * assertions provided by the {@code recordedRequestConsumer}.
     *
     * @param recordedRequestConsumer the {@link Consumer} containing the assertions on the {@link RecordedRequest}
     * @return this instance
     */
    public MockWebServerAssertions hasRecordedRequest(Consumer<RecordedRequest> recordedRequestConsumer) {
        var requestOrNull = RecordedRequests.takeRequestOrNull(mockWebServer);
        recordedRequestConsumer.accept(requestOrNull);

        return this;
    }

    /**
     * This is a terminal method that gets the next {@link RecordedRequest} (or {@code null})
     * and makes it available as a {@link RecordedRequestAssertions} instance for further
     * assertion chaining.
     *
     * @return this instance
     */
    public RecordedRequestAssertions recordedRequest() {
        var requestOrNull = RecordedRequests.takeRequestOrNull(mockWebServer);
        return RecordedRequestAssertions.assertThatRecordedRequest(requestOrNull);
    }
}
