package org.kiwiproject.test.okhttp3.mockwebserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.kiwiproject.base.KiwiStrings.f;
import static org.kiwiproject.test.okhttp3.mockwebserver.RecordedRequestAssertions.METHODS_ALLOWING_BODY;
import static org.kiwiproject.test.okhttp3.mockwebserver.RecordedRequestAssertions.assertThatRecordedRequest;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kiwiproject.base.UncheckedInterruptedException;
import org.kiwiproject.io.KiwiIO;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import javax.net.ssl.SSLHandshakeException;

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
    void shouldCreateUsingAssertThat() {
        server.enqueue(new MockResponse());

        var path = "/";
        JdkHttpClients.get(httpClient, uri(path));

        var recordedRequest = takeRequest();

        RecordedRequestAssertions.assertThat(recordedRequest)
                .isGET()
                .hasPath(path);
    }

    @Test
    void shouldPassSuccessfulGETRequests() {
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
                assertThatRecordedRequest(recordedRequest)
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

        private String body;
        private RecordedRequest recordedRequest;

        @BeforeEach
        void setUp() {
            body = """
                    {
                        "username": "alice",
                        "password": "peaches"
                    }
                    """;

            server.enqueue(new MockResponse().setResponseCode(201));
            JdkHttpClients.post(httpClient, uri("/users"), body);
            recordedRequest = takeRequest();
        }

        @Test
        void shouldCheckRequestBody() {
            assertThatCode(() ->
                            assertThatRecordedRequest(recordedRequest)
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

        @ParameterizedTest
        @ValueSource(strings = {
                "CONNECT",
                "GET",
                "HEAD",
                "OPTIONS",
                "TRACE"
        })
        void shouldCheckMethodCanHaveBody(String method) {
            var badRecordedRequest = mock(RecordedRequest.class);
            when(badRecordedRequest.getMethod()).thenReturn(method);

            assertAll(
                    () -> assertThatThrownBy(() -> assertThatRecordedRequest(badRecordedRequest).hasBody("{}"))
                            .isNotNull()
                            .hasMessageContaining("Body not allowed for method %s. The request method should be one of %s",
                                    method, METHODS_ALLOWING_BODY),

                    () -> assertThatThrownBy(() -> assertThatRecordedRequest(badRecordedRequest).hasBodySize(42))
                            .isNotNull()
                            .hasMessageContaining("Body not allowed for method %s. The request method should be one of %s",
                                    method, METHODS_ALLOWING_BODY)
            );
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
                            assertThatRecordedRequest(recordedRequest).hasNoBody()
                    ).doesNotThrowAnyException();
        }

        @Test
        void shouldCheckWhenHasBodyButNoneIsExpected() {
            server.enqueue(new MockResponse().setResponseCode(200));
            JdkHttpClients.put(httpClient, uri(path), """
                    { "foo": "bar" }
                    """);
            recordedRequest = takeRequest();

            assertThatThrownBy(() -> assertThatRecordedRequest(recordedRequest).hasNoBody())
                    .isNotNull()
                    .hasMessageContaining("""
                            Expected there not to be a request body but found: { "foo": "bar" }
                            """);
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

        private RecordedRequest recordedRequest;
        private String message;
        private ConnectException connectException;

        @BeforeEach
        void setUp() {
            recordedRequest = mock(RecordedRequest.class);
            message = "Failed to connect to localhost/[0:0:0:0:0:0:0:1]:59881";
            connectException = new ConnectException(message);
            when(recordedRequest.getFailure()).thenReturn(connectException);
        }

        @Test
        void shouldCheckFailureMessage() {
            assertThatCode(() -> assertThatRecordedRequest(recordedRequest).hasFailureMessage(message))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCheckInvalidFailureMessage() {
            assertThatThrownBy(() ->
                    assertThatRecordedRequest(recordedRequest).hasFailureMessage("Connection refused"))
                    .isNotNull()
                    .hasMessageContaining("Expected a failure with message: Connection refused");
        }

        @Test
        void shouldCheckFailureMessageContains() {
            assertThatCode(() -> assertThatRecordedRequest(recordedRequest).hasFailureMessageContaining(message))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCheckInvalidFailureMessageContains() {
            assertThatThrownBy(() ->
                    assertThatRecordedRequest(recordedRequest).hasFailureMessage("TLS handshake failed"))
                    .isNotNull()
                    .hasMessageContaining("Expected a failure with message: TLS handshake failed");
        }

        @Test
        void shouldCheckFailureMessageStartingWith() {
            assertThatCode(() -> assertThatRecordedRequest(recordedRequest)
                    .hasFailureMessageStartingWith(message.substring(0, 5)))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCheckInvalidFailureMessageStartingWith() {
            assertThatThrownBy(() ->
                    assertThatRecordedRequest(recordedRequest).hasFailureMessageStartingWith("TLS handshake failed"))
                    .isNotNull()
                    .hasMessageContaining("Expected a failure with message that starts with: TLS handshake failed");
        }

        @Test
        void shouldCheckFailureMessageEndingWith() {
            assertThatCode(() -> assertThatRecordedRequest(recordedRequest)
                    .hasFailureMessageEndingWith(message.substring(5)))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCheckInvalidFailureMessageEndingWith() {
            assertThatThrownBy(() ->
                    assertThatRecordedRequest(recordedRequest).hasFailureMessageEndingWith("handshake failed"))
                    .isNotNull()
                    .hasMessageContaining("Expected a failure with message that ends with: handshake failed");
        }

        @Test
        void shouldCheckFailureCauseInstanceOf() {
            var recordedRequestWithFailureCause = mock(RecordedRequest.class);
            var exception = new IOException(connectException);
            when(recordedRequestWithFailureCause.getFailure()).thenReturn(exception);

            assertThatCode(() -> assertThatRecordedRequest(recordedRequest)
                    .hasFailureCauseInstanceOf(connectException.getClass()))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCheckInvalidFailureCauseInstanceOf() {
            var recordedRequestWithFailureCause = mock(RecordedRequest.class);
            var exception = new IOException(connectException);
            when(recordedRequestWithFailureCause.getFailure()).thenReturn(exception);

            assertThatThrownBy(() -> assertThatRecordedRequest(recordedRequest)
                    .hasFailureCauseInstanceOf(SSLHandshakeException.class))
                    .isNotNull()
                    .hasMessageContaining("Expected request to have failure of type: javax.net.ssl.SSLHandshakeException");
        }

        // TODO Delete if can't figure out (and also delete the okhttp-tls test dependency)
        @Disabled
        @SuppressWarnings("all")
        @Test
        void temporaryHacking() {
            var localhostCertificate = new HeldCertificate.Builder()
                    .addSubjectAlternativeName("localhost")
                    .build();
            var serverCertificates = new HandshakeCertificates.Builder()
                    .heldCertificate(localhostCertificate)
                    .build();

            server.useHttps(serverCertificates.sslSocketFactory(), false);

            System.out.println("server.getProtocolNegotiationEnabled() = " + server.getProtocolNegotiationEnabled());


            server.enqueue(new MockResponse()
                            .setBody("""
                                    { "echo": "hello" }
                                    """)
//                    .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_REQUEST_BODY)
                            .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
//                    .setSocketPolicy(SocketPolicy.FAIL_HANDSHAKE)
            );

            String expectedMessage = null;
            try {
                var uri = uri("/");
                System.out.println("uri = " + uri);

                var clientCertificates = new HandshakeCertificates.Builder()
                        .addTrustedCertificate(localhostCertificate.certificate())
                        .build();
                var client = new OkHttpClient.Builder()
                        .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager())
                        .build();

                RequestBody body = RequestBody.create("""
                        { "message": "hello" }
                        """, MediaType.get("application/json"));
                Request request = new Request.Builder()
                        .url(uri.toString())
                        .put(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    System.out.println("Response status: " + response.code());
                    System.out.println("Response body: " + response.body().string());
                    System.out.println("Response protocol: " + response.protocol());
                }

            } catch (Exception e) {
                System.out.println("Request failed: " + e.getMessage());
                System.out.println("Exception class: " + e.getClass().getName());

                assertThat(e).isInstanceOf(IOException.class);

                e.printStackTrace();
                expectedMessage = e.getCause().getMessage();
                System.out.println("expectedMessage = " + expectedMessage);
            }

            var theRecordedRequest = takeRequest();

            var failure = theRecordedRequest.getFailure();
            System.out.println("failure = " + failure);

            // TODO - why is the failure null?
            //RecordedRequestAssertions.assertThat(theRecordedRequest).hasFailureMessage(expectedMessage);
    }
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
