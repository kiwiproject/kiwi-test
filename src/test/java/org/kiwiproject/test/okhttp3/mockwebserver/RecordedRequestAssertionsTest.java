package org.kiwiproject.test.okhttp3.mockwebserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.kiwiproject.base.KiwiStrings.f;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kiwiproject.base.UncheckedInterruptedException;
import org.kiwiproject.io.KiwiIO;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

@DisplayName("RecordedRequestAssertions")
class RecordedRequestAssertionsTest {

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
        KiwiIO.closeQuietly(server);
    }

    @Test
    void shouldCreateUsingAssertThat() throws InterruptedException {
        server.enqueue(new MockResponse());

        var path = "/";
        JdkHttpClients.get(httpClient, uri(path));

        var recordedRequest = takeRequest();

        RecordedRequestAssertions.assertThat(recordedRequest)
                .isGET()
                .hasPath(path);
    }

    @Test
    void shouldPassSuccessfulGETRequests() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/plain")
                .setBody("Hello, world!")
        );

        var path = "/status";
        var uri = uri(path);

        JdkHttpClients.send(httpClient,  HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .header("Accept", "text/plain")
                .build());

        var recordedRequest = takeRequest();

        assertThatCode(() ->
                    RecordedRequestAssertions.assertThatRecordedRequest(recordedRequest)
                        .isGET()
                        .isNotTls()
                        .hasTlsVersion(null)
                        .hasNoFailure()
                        .hasFailure(failure -> assertThat(failure).isNull())
                        .hasNoBody()
                        .hasBodySize(0)
                        .hasHeader("Accept", "text/plain")
                        .hasRequestUrl(uri)
                        .hasRequestLine(f("GET {} HTTP/1.1", path))
                        .hasPath(path))
                .doesNotThrowAnyException();
    }

    @Nested
    class Bodies {

        private String path;
        private String body;
        private RecordedRequest recordedRequest;

        @BeforeEach
        void setUp() {
            path = "/users";
            body = """
                    {
                        "username": "alice",
                        "password": "peaches"
                    }
                    """;

            server.enqueue(new MockResponse().setResponseCode(201));
            JdkHttpClients.post(httpClient, uri(path), body);
            recordedRequest = takeRequest();
        }

        @Test
        void shouldCheckRequestBody() {
            assertThatCode(() ->
                        RecordedRequestAssertions.assertThatRecordedRequest(recordedRequest)
                            .hasBodySize(body.length())
                            .hasBody(body)
                    ).doesNotThrowAnyException();
        }

        @Test
        void shouldCheckInvalidBody() {
            var expectedBody = """
                {
                    "username": "alice",
                    "password": "hacked"
                }
                """;
            assertThatThrownBy(() -> RecordedRequestAssertions.assertThat(recordedRequest) .hasBody(expectedBody))
                    .isNotNull()
                    .hasMessageContaining("Expected body as UTF-8 to be: " + expectedBody);
        }

        @Test
        void shouldCheckInvalidBodySize() {
            assertThatThrownBy(() -> RecordedRequestAssertions.assertThat(recordedRequest).hasBodySize(2_567))
                    .isNotNull()
                    .hasMessageContaining("Expected body size: 2567 byte");
        }
    }

    @Nested
    class NoBodies {

        private String path;
        private RecordedRequest recordedRequest;

        @BeforeEach
        void setUp() {
            path = "/users/42";
        }

        @Test
        void shouldCheckWhenExpectNoBody() {
            server.enqueue(new MockResponse().setResponseCode(204));
            JdkHttpClients.delete(httpClient, uri(path));
            recordedRequest = takeRequest();

            assertThatCode(() ->
                        RecordedRequestAssertions.assertThatRecordedRequest(recordedRequest).hasNoBody()
                    ).doesNotThrowAnyException();
        }

        @Test
        void shouldCheckWhenHasBodyButNoneIsExpected() {
            server.enqueue(new MockResponse().setResponseCode(200));
            JdkHttpClients.put(httpClient, uri(path), """
                    { "foo": "bar" }
                    """);
            recordedRequest = takeRequest();

            assertThatThrownBy(() -> RecordedRequestAssertions.assertThatRecordedRequest(recordedRequest).hasNoBody())
                    .isNotNull()
                    .hasMessageContaining("Expected there not to be a request body but found: {}");
        }
    }

    @Nested
    class RequestMethods {

        @Test
        void shouldCheckGETRequests() {
            server.enqueue(new MockResponse());
            JdkHttpClients.get(httpClient, uri("/"));

            var recordedRequest = takeRequest();

            assertAll(
                () -> assertExpectedMethod(recordedRequest, RecordedRequestAssertions::isGET),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPOST, "POST"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPUT, "PUT"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isDELETE, "DELETE"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isHEAD, "HEAD"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isCONNECT, "CONNECT"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isOPTIONS, "OPTIONS"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isTRACE, "TRACE"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPATCH, "PATCH")
            );
        }

        @Test
        void shouldCheckPOSTRequests() {
            server.enqueue(new MockResponse());
            JdkHttpClients.post(httpClient, uri("/"), "{}");

            var recordedRequest = takeRequest();

            assertAll(
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isGET, "GET"),
                () -> assertExpectedMethod(recordedRequest, RecordedRequestAssertions::isPOST),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPUT, "PUT"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isDELETE, "DELETE"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isHEAD, "HEAD"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isCONNECT, "CONNECT"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isOPTIONS, "OPTIONS"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isTRACE, "TRACE"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPATCH, "PATCH")
            );
        }

        @Test
        void shouldCheckPUTRequests() {
            server.enqueue(new MockResponse());

            JdkHttpClients.put(httpClient, uri("/"), "{}");

            var recordedRequest = takeRequest();

            assertAll(
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isGET, "GET"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPOST, "POST"),
                () -> assertExpectedMethod(recordedRequest, RecordedRequestAssertions::isPUT),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isDELETE, "DELETE"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isHEAD, "HEAD"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isCONNECT, "CONNECT"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isOPTIONS, "OPTIONS"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isTRACE, "TRACE"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPATCH, "PATCH")
            );
        }

        @Test
        void shouldCheckDELETERequests() {
            server.enqueue(new MockResponse());

            JdkHttpClients.delete(httpClient, uri("/"));

            var recordedRequest = takeRequest();

            assertAll(
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isGET, "GET"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPOST, "POST"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPUT, "PUT"),
                () -> assertExpectedMethod(recordedRequest, RecordedRequestAssertions::isDELETE),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isHEAD, "HEAD"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isCONNECT, "CONNECT"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isOPTIONS, "OPTIONS"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isTRACE, "TRACE"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPATCH, "PATCH")
            );
        }

        @Test
        void shouldCheckHEADRequests() {
            server.enqueue(new MockResponse());

            JdkHttpClients.send(httpClient, HttpRequest.newBuilder()
                    .method("HEAD", BodyPublishers.noBody())
                    .uri(uri("/"))
                    .build());

            var recordedRequest = takeRequest();

            assertAll(
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isGET, "GET"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPOST, "POST"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPUT, "PUT"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isDELETE, "DELETE"),
                () -> assertExpectedMethod(recordedRequest, RecordedRequestAssertions::isHEAD),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isCONNECT, "CONNECT"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isOPTIONS, "OPTIONS"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isTRACE, "TRACE"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPATCH, "PATCH")
            );
        }

        @Test
        void shouldCheckOPTIONSRequests() {
            server.enqueue(new MockResponse());

            JdkHttpClients.send(httpClient, HttpRequest.newBuilder()
                    .method("OPTIONS", BodyPublishers.noBody())
                    .uri(uri("/"))
                    .build());

            var recordedRequest = takeRequest();

            assertAll(
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isGET, "GET"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPOST, "POST"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPUT, "PUT"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isDELETE, "DELETE"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isHEAD, "HEAD"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isCONNECT, "CONNECT"),
                () -> assertExpectedMethod(recordedRequest, RecordedRequestAssertions::isOPTIONS),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isTRACE, "TRACE"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPATCH, "PATCH")
            );
        }

        @Test
        void shouldCheckCONNECTRequests() {
            server.enqueue(new MockResponse());

            // JDK HttpClient may not support CONNECT, so mock the RecordedRequest
            var recordedRequest = mock(RecordedRequest.class);
            when(recordedRequest.getMethod()).thenReturn("CONNECT");

            assertAll(
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isGET, "GET"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPOST, "POST"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPUT, "PUT"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isDELETE, "DELETE"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isHEAD, "HEAD"),
                () -> assertExpectedMethod(recordedRequest, RecordedRequestAssertions::isCONNECT),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isOPTIONS, "OPTIONS"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isTRACE, "TRACE"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPATCH, "PATCH")
            );
        }

        @Test
        void shouldCheckTRACERequests() {
            server.enqueue(new MockResponse());

            JdkHttpClients.send(httpClient, HttpRequest.newBuilder()
                    .method("TRACE", BodyPublishers.noBody())
                    .uri(uri("/"))
                    .build());

            var recordedRequest = takeRequest();

            assertAll(
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isGET, "GET"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPOST, "POST"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPUT, "PUT"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isDELETE, "DELETE"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isHEAD, "HEAD"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isCONNECT, "CONNECT"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isOPTIONS, "OPTIONS"),
                () -> assertExpectedMethod(recordedRequest, RecordedRequestAssertions::isTRACE),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPATCH, "PATCH")
            );
        }

        @Test
        void shouldCheckPATCHRequests() {
            server.enqueue(new MockResponse());

            JdkHttpClients.send(httpClient, HttpRequest.newBuilder()
                    .method("PATCH", BodyPublishers.ofString("{}"))
                    .uri(uri("/"))
                    .build());

            var recordedRequest = takeRequest();

            assertAll(
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isGET, "GET"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPOST, "POST"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isPUT, "PUT"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isDELETE, "DELETE"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isHEAD, "HEAD"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isCONNECT, "CONNECT"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isOPTIONS, "OPTIONS"),
                () -> assertNotExpectedMethod(recordedRequest, RecordedRequestAssertions::isTRACE, "TRACE"),
                () -> assertExpectedMethod(recordedRequest, RecordedRequestAssertions::isPATCH)
            );
        }

        private void assertExpectedMethod(RecordedRequest recordedRequest,
                UnaryOperator<RecordedRequestAssertions> fn) {

            var assertions = RecordedRequestAssertions.assertThat(recordedRequest);
            assertThatCode(() -> fn.apply(assertions)).doesNotThrowAnyException();
        }

        private void assertNotExpectedMethod(RecordedRequest recordedRequest,
                UnaryOperator<RecordedRequestAssertions> fn,
                String method) {

            var assertions = RecordedRequestAssertions.assertThat(recordedRequest);
            assertThatThrownBy(() -> fn.apply(assertions))
                    .isNotNull()
                    .hasMessageContaining("Expected method to be " + method);
        }
    }

    @Nested
    class RequestFailures {

        @Test
        void shouldCheckFailureMessage() {
            server.enqueue(new MockResponse()
                    .throttleBody(1, 1, TimeUnit.SECONDS)
                    .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_REQUEST_BODY));

            String expectedMessage = null;
            try {
                JdkHttpClients.put(httpClient, uri("/"), "{}");
            } catch (Exception e) {
                expectedMessage = e.getCause().getMessage();
            }

            // TODO assert we have an expected message

            var recordedRequest = takeRequest();

            // TODO - why is the failure null?
            //RecordedRequestAssertions.assertThat(recordedRequest).hasFailureMessage(expectedMessage);
    }

        // TODO
    }

    private RecordedRequest takeRequest() {
        try {
            return server.takeRequest(10, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }
    }

    private URI uri(String path) {
        return MockWebServers.uri(server, path);
    }
}
