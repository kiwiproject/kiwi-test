package org.kiwiproject.test.okhttp3.mockwebserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.kiwiproject.test.assertj.KiwiAssertJ.assertPresentAndGet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Durations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kiwiproject.base.UncheckedInterruptedException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@DisplayName("RecordedRequests")
class RecordedRequestsTest {

    @RegisterExtension
    private final MockWebServerExtension serverExtension = new MockWebServerExtension();

    private MockWebServer server;

    @BeforeEach
    void setUp() {
        server = serverExtension.server();
    }

    @Nested
    class TakeRequiredRequest {

        @Test
        void shouldReturnTheAvailableRequest() {
            server.enqueue(new MockResponse().setResponseCode(200));

            var path = randomPath();
            makeRequest(path);

            var recordedRequest = RecordedRequests.takeRequiredRequest(server);

            assertRequest(recordedRequest, path);
        }

        @Test
        void shouldThrowIllegalState_WhenNoRequestIsAvailable() {
            assertThatIllegalStateException()
                    .isThrownBy(() -> RecordedRequests.takeRequiredRequest(server))
                    .withMessage("no request is currently available");
        }
    }

    @Nested
    class TakeRequestOrEmpty {

        @Test
        void shouldReturnTheAvailableRequest() {
            server.enqueue(new MockResponse().setResponseCode(202));

            var path = randomPath();
            makeRequest(path);

            var recordedRequestOpt = RecordedRequests.takeRequestOrEmpty(server);

            var recordedRequest = assertPresentAndGet(recordedRequestOpt);
            assertRequest(recordedRequest, path);
        }

        @Test
        void shouldReturnEmptyOptional_WhenNoRequestIsAvailable() {
            assertThat(RecordedRequests.takeRequestOrEmpty(server)).isEmpty();
        }
    }

    @Nested
    class AssertNoMoreRequests {

        @Test
        void shouldPass_WhenThereIsNoRequestAvailable() {
            assertThatCode(() -> RecordedRequests.assertNoMoreRequests(server))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldFail_WhenAnyRequestIsAvailable() {
            server.enqueue(new MockResponse().setResponseCode(202));

            var path = randomPath();
            makeRequest(path);

            assertThatThrownBy(() ->  RecordedRequests.assertNoMoreRequests(server))
                    .isNotNull()
                    .hasMessageContaining(
                        "There should not be any more requests, but (at least) one was found");
        }
    }

    @Nested
    class TakeRequestOrNull {

        @Test
        void shouldReturnTheAvailableRequest() {
            server.enqueue(new MockResponse().setResponseCode(204));

            var path = randomPath();
            makeRequest(path);

            var recordedRequestOpt = RecordedRequests.takeRequestOrEmpty(server);

            var recordedRequest = assertPresentAndGet(recordedRequestOpt);
            assertRequest(recordedRequest, path);
        }

        @Test
        void shouldReturnNull_WhenNoRequestIsAvailable() {
            assertThat(RecordedRequests.takeRequestOrNull(server)).isNull();
        }

        @Test
        void shouldThrowUncheckedInterruptedException_IfInterruptedExceptionIsThrown() throws InterruptedException {
            var mockMockWebServer = mock(MockWebServer.class);
            when(mockMockWebServer.takeRequest(anyLong(), any(TimeUnit.class)))
                    .thenThrow(new InterruptedException("I interrupt you!"));

            // Execute in separate thread so that the "Thread.currentThread().interrupt()" call
            // does not interrupt the test thread.
            var executor = Executors.newSingleThreadExecutor();
            try {
                Callable<RecordedRequest> callable = () -> RecordedRequests.takeRequestOrNull(mockMockWebServer);
                var future = executor.submit(callable);
                await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).until(future::isDone);

                assertThatThrownBy(future::get)
                        .cause()
                        .isExactlyInstanceOf(UncheckedInterruptedException.class)
                        .hasMessageEndingWith("I interrupt you!");

                verify(mockMockWebServer, only()).takeRequest(10, TimeUnit.MILLISECONDS);
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private static String randomPath() {
        return "/" + RandomStringUtils.secure().nextAlphabetic(10);
    }

    private void makeRequest(String path) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(100))
                .build();

        var uri = serverExtension.uri(path);

        try {
            var request = HttpRequest.newBuilder().GET().uri(uri).build();
            httpClient.send(request, BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void assertRequest(RecordedRequest request, String expectedPath) {
        assertAll(
            () -> assertThat(request.getMethod()).isEqualTo("GET"),
            () -> assertThat(request.getPath()).isEqualTo(expectedPath)
        );
    }
}
