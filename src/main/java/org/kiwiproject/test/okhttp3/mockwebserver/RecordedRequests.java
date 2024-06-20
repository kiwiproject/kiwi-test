package org.kiwiproject.test.okhttp3.mockwebserver;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.kiwiproject.base.UncheckedInterruptedException;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Contains test assertions for {@link RecordedRequest} when using {@link MockWebServer}.
 */
@UtilityClass
@Slf4j
public class RecordedRequests {

    /**
     * Get the next available {@link RecordedRequest} or throw an exception.
     *
     * @param mockWebServer the {@link MockWebServer} expected to contain the recorded request
     * @return the next available {@link RecordedRequest}
     * @throws IllegalStateException if there isn't an available {@link RecordedRequest}
     */
    public static RecordedRequest takeRequiredRequest(MockWebServer mockWebServer) {
        return takeRequestOrEmpty(mockWebServer)
                .orElseThrow(() -> new IllegalStateException("no request is currently available"));
    }

    /**
     * Get the next available {@link RecordedRequest} or return an empty Optional.
     *
     * @param mockWebServer the {@link MockWebServer} which may contain the recorded request
     * @return Optional containing the next available {@link RecordedRequest}, or an empty Optional
     */
    public static Optional<RecordedRequest> takeRequestOrEmpty(MockWebServer mockWebServer) {
        return Optional.ofNullable(takeRequestOrNull(mockWebServer));
    }

    /**
     * Assert there are not any available {@link RecordedRequest} instances.
     *
     * @param mockWebServer the {@link MockWebServer} to verify
     */
    public static void assertNoMoreRequests(MockWebServer mockWebServer) {
        assertThat(takeRequestOrNull(mockWebServer))
                .describedAs("There should not be any more requests, but (at least) one was found")
                .isNull();
    }

    /**
     * Get the next available {@link RecordedRequest} or return {@code null}.
     * <p>
     * Unlike {@link MockWebServer#takeRequest()}, does not block. Instead, waits
     * a brief amount of time (10 milliseconds) for the next request. And unlike
     * {@link MockWebServer#takeRequest(long, TimeUnit)}, converts a checked
     * InterruptedException to an {@link UncheckedInterruptedException} so that
     * tests don't need to constantly declare "throws" clauses.
     *
     * @param mockWebServer the {@link MockWebServer} which may contain the recorded request
     * @return the next available {@link RecordedRequest}, or null if not available
     * @throws UncheckedInterruptedException if the call to get the next request throws InterruptedException
     */
    public static RecordedRequest takeRequestOrNull(MockWebServer mockWebServer) {
        try {
            return mockWebServer.takeRequest(10, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.info("Interrupted waiting to get next request", e);
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        }
    }
}
