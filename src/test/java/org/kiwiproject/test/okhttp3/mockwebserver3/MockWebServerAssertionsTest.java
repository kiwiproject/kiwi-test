package org.kiwiproject.test.okhttp3.mockwebserver3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okio.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

@DisplayName("MockWebServerAssertions (mockwebserver3)")
class MockWebServerAssertionsTest {

    private MockWebServer server;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(100))
                .build();
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void shouldCreateUsingAssertThat() {
        MockWebServerAssertions.assertThat(server).hasRequestCount(0);
    }

    @Test
    void shouldPassSuccessfulGETRequests() {
        server.enqueue(new MockResponse.Builder().code(200).build());

        var path = "/status";
        JdkHttpClients.get(httpClient, server.url(path).uri());

        assertThatCode(() ->
                    MockWebServerAssertions.assertThatMockWebServer(server)
                        .hasRequestCount(1)
                        .recordedRequest()
                            .isGET()
                            .hasTarget(path))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowCheckingMultipleRecordedRequests() {
        server.enqueue(new MockResponse.Builder().code(200).build());
        server.enqueue(new MockResponse.Builder().code(201).build());

        var path1 = "/status";
        JdkHttpClients.get(httpClient, server.url(path1).uri());

        var path2 = "/create";
        var body = """
                    { "name": "Bob" }
                    """;
        JdkHttpClients.post(httpClient, server.url(path2).uri(), body);

        assertThatCode(() ->
                    MockWebServerAssertions.assertThatMockWebServer(server)
                        .hasRequestCount(2)
                        .hasRecordedRequest(recordedRequest -> {
                            assertThat(recordedRequest.getMethod()).isEqualTo("GET");
                            assertThat(recordedRequest.getTarget()).isEqualTo(path1);
                        })
                        .hasRecordedRequest(recordedRequest -> {
                            assertThat(recordedRequest.getMethod()).isEqualTo("POST");
                            assertThat(recordedRequest.getTarget()).isEqualTo(path2);
                            assertThat(recordedRequest.getBody())
                                    .isNotNull()
                                    .extracting(ByteString::utf8)
                                    .isEqualTo(body);
                        }))
                .doesNotThrowAnyException();
    }
}
