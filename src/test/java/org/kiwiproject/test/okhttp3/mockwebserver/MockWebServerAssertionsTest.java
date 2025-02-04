package org.kiwiproject.test.okhttp3.mockwebserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.http.HttpClient;
import java.time.Duration;

@DisplayName("MockWebServerAssertions")
class MockWebServerAssertionsTest {

    @RegisterExtension
    private final MockWebServerExtension mockWebServerExtension = new MockWebServerExtension();

    private MockWebServer server;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        server = mockWebServerExtension.server();

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(100))
                .build();
    }

    @Test
    void shouldCreateUsingAssertThat() {
        MockWebServerAssertions.assertThat(server).hasRequestCount(0);
    }

    @Test
    void shouldPassSuccessfulGETRequests() {
        server.enqueue(new MockResponse());

        var path = "/status";
        var uri = mockWebServerExtension.uri(path);
        JdkHttpClients.get(httpClient, uri);

        assertThatCode(() ->
                    MockWebServerAssertions.assertThatMockWebServer(server)
                        .hasRequestCount(1)
                        .recordedRequest()
                            .isGET()
                            .hasPath(path))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowCheckingMultipleRecordedRequests() {
        server.enqueue(new MockResponse());
        server.enqueue(new MockResponse().setResponseCode(201));

        var path1 = "/status";
        var uri1 = mockWebServerExtension.uri(path1);
        JdkHttpClients.get(httpClient, uri1);

        var path2 = "/create";
        var uri2 = mockWebServerExtension.uri(path2);
        var body = """
                    { "name": "Bob" }
                    """;
        JdkHttpClients.post(httpClient, uri2, body);

        assertThatCode(() ->
                    MockWebServerAssertions.assertThatMockWebServer(server)
                        .hasRequestCount(2)
                        .hasRecordedRequest(recordedRequest -> {
                            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                            assertThat(recordedRequest.getPath()).isEqualTo(path1);
                        })
                        .hasRecordedRequest(recordedRequest -> {
                            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
                            assertThat(recordedRequest.getPath()).isEqualTo(path2);
                            assertThat(recordedRequest.getBody().readUtf8()).isEqualTo(body);
                        }))
                .doesNotThrowAnyException();
    }
}
