package org.kiwiproject.test.okhttp3.mockwebserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.kiwiproject.base.KiwiStrings.f;
import static org.kiwiproject.test.okhttp3.mockwebserver.RecordedRequestAssertions.METHODS_ALLOWING_BODY;
import static org.kiwiproject.test.okhttp3.mockwebserver.RecordedRequestAssertions.assertThatRecordedRequest;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import okhttp3.TlsVersion;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kiwiproject.base.UncheckedInterruptedException;
import org.kiwiproject.test.constants.KiwiTestConstants;

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

@DisplayName("RecordedRequestAssertions (mockwebserver)")
class RecordedRequestAssertionsTest {

    @RegisterExtension
    private final MockWebServerExtension serverExtension = new MockWebServerExtension();

    private MockWebServer server;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        server = serverExtension.server();

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(100))
                .build();
    }

    @Test
    void shouldRequireNonNullRecordedRequest() {
        assertAll(
                () -> assertThatIllegalArgumentException()
                        .isThrownBy(() -> RecordedRequestAssertions.assertThat(null).isGET())
                        .withMessage("recordedRequest must not be null"),

                () -> assertThatIllegalArgumentException()
                        .isThrownBy(() -> RecordedRequestAssertions.assertThatRecordedRequest(null).isPOST())
                        .withMessage("recordedRequest must not be null")
        );
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
        void shouldCheckBody_WhenSatisfiesConsumer() {
            assertThatCode(() ->
                    assertThatRecordedRequest(recordedRequest)
                            .hasBodySatisfying(theBody -> {
                                assertThat(theBody).contains("alice");
                                assertThat(theBody).contains("peaches");
                            })).doesNotThrowAnyException();
        }

        @Test
        void shouldCheckBody_WhenDoesNotSatisfyConsumer() {
            assertThatThrownBy(() ->
                    assertThatRecordedRequest(recordedRequest)
                            .hasBodySatisfying(theBody -> {
                                assertThat(theBody).contains("alice");
                                assertThat(theBody).contains("apples");
                            }))
                    .hasMessageContaining("Expecting")
                    .hasMessageContaining("alice")
                    .hasMessageContaining("peaches");
        }

        @Test
        void shouldCheckInvalidBodySize() {
            assertThatThrownBy(() -> RecordedRequestAssertions.assertThat(recordedRequest).hasBodySize(2_567))
                    .isNotNull()
                    .hasMessageContaining("Expected body size: 2567 byte");
        }

        @Test
        void shouldCheckHasJsonBodyWithEntity() {
            var expectedEntity = new UserCredentials("alice", "peaches");
            assertThatCode(() ->
                    assertThatRecordedRequest(recordedRequest).hasJsonBodyWithEntity(expectedEntity)
            ).doesNotThrowAnyException();
        }

        @Test
        void shouldCheckHasJsonBodyWithEntity_WithCustomObjectMapper() {
            var expectedEntity = new UserCredentials("alice", "peaches");
            var objectMapper = KiwiTestConstants.OBJECT_MAPPER;
            assertThatCode(() ->
                    assertThatRecordedRequest(recordedRequest).hasJsonBodyWithEntity(expectedEntity, objectMapper)
            ).doesNotThrowAnyException();
        }

        @Test
        void shouldCheckHasJsonBodyWithEntity_WhenDoesNotMatchExpectedEntity() {
            var expectedEntity = new UserCredentials("alice", "oranges");
            assertThatThrownBy(() ->
                    assertThatRecordedRequest(recordedRequest).hasJsonBodyWithEntity(expectedEntity))
                    .hasMessageContaining("Expecting actual")
                    .hasMessageContaining(new UserCredentials("alice", "peaches").toString())
                    .hasMessageContaining("to be equal to")
                    .hasMessageContaining(expectedEntity.toString())
            ;
        }

        @Test
        void shouldCheckHasJsonBodyWithEntity_WhenBodyIsNotJson() {
            var badRecordedRequest = mock(RecordedRequest.class);
            when(badRecordedRequest.getMethod()).thenReturn("POST");

            var buffer = mock(Buffer.class);
            when(badRecordedRequest.getBody()).thenReturn(buffer);
            when(buffer.readUtf8()).thenReturn("this is not json");

            var expectedEntity = new UserCredentials("alice", "peaches");
            assertThatThrownBy(() ->
                    assertThatRecordedRequest(badRecordedRequest).hasJsonBodyWithEntity(expectedEntity))
                    .hasMessageContaining("Body content expected to be JSON");
        }
    }

    record UserCredentials(String username, String password) {
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
    class Headers {

        private RecordedRequest recordedRequest;

        @BeforeEach
        void setUp() {
            server.enqueue(new MockResponse());
            JdkHttpClients.send(httpClient, HttpRequest.newBuilder()
                    .GET()
                    .uri(uri("/"))
                    .header("Accept", "text/html; charset=utf-8")
                    .build());
            recordedRequest = takeRequest();
        }

        @Test
        void shouldCheckHeaderValueStartingWith() {
            assertThatCode(() -> assertThatRecordedRequest(recordedRequest)
                    .hasHeaderValueStartingWith("Accept", "text/html"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCheckInvalidHeaderValueStartingWith() {
            assertThatThrownBy(() -> assertThatRecordedRequest(recordedRequest)
                    .hasHeaderValueStartingWith("Accept", "application/json"))
                    .isNotNull()
                    .hasMessageContaining("Expected Accept header to have value starting with: application/json");
        }

        @Test
        void shouldCheckHeaderValueStartingWith_WhenHeaderIsAbsent() {
            assertThatThrownBy(() -> assertThatRecordedRequest(recordedRequest)
                    .hasHeaderValueStartingWith("Authorization", "Bearer"))
                    .isNotNull()
                    .hasMessageContaining("Expected header Authorization to be present");
        }

        @Test
        void shouldCheckHeaderValueContaining() {
            assertThatCode(() -> assertThatRecordedRequest(recordedRequest)
                    .hasHeaderValueContaining("Accept", "charset"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCheckInvalidHeaderValueContaining() {
            assertThatThrownBy(() -> assertThatRecordedRequest(recordedRequest)
                    .hasHeaderValueContaining("Accept", "application/json"))
                    .isNotNull()
                    .hasMessageContaining("Expected Accept header to have value containing: application/json");
        }

        @Test
        void shouldCheckHeaderValueContaining_WhenHeaderIsAbsent() {
            assertThatThrownBy(() -> assertThatRecordedRequest(recordedRequest)
                    .hasHeaderValueContaining("Authorization", "Bearer"))
                    .isNotNull()
                    .hasMessageContaining("Expected header Authorization to be present");
        }

        @Test
        void shouldCheckHeaderValueEndingWith() {
            assertThatCode(() -> assertThatRecordedRequest(recordedRequest)
                    .hasHeaderValueEndingWith("Accept", "charset=utf-8"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCheckInvalidHeaderValueEndingWith() {
            assertThatThrownBy(() -> assertThatRecordedRequest(recordedRequest)
                    .hasHeaderValueEndingWith("Accept", "application/json"))
                    .isNotNull()
                    .hasMessageContaining("Expected Accept header to have value ending with: application/json");
        }

        @Test
        void shouldCheckHeaderValueEndingWith_WhenHeaderIsAbsent() {
            assertThatThrownBy(() -> assertThatRecordedRequest(recordedRequest)
                    .hasHeaderValueEndingWith("Authorization", "token"))
                    .isNotNull()
                    .hasMessageContaining("Expected header Authorization to be present");
        }

        @Test
        void shouldCheckHeader() {
            assertThatCode(() -> assertThatRecordedRequest(recordedRequest)
                    .hasHeader("Accept", "text/html; charset=utf-8"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCheckInvalidHeader() {
            assertThatThrownBy(() -> assertThatRecordedRequest(recordedRequest)
                    .hasHeader("Accept", "application/json"))
                    .isNotNull()
                    .hasMessageContaining("Expected Accept header to have value: application/json");
        }

        @Test
        void shouldCheckHeaderPresence() {
            assertThatCode(() -> assertThatRecordedRequest(recordedRequest)
                    .hasHeader("Accept"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCheckHeaderPresence_WhenHeaderIsAbsent() {
            assertThatThrownBy(() -> assertThatRecordedRequest(recordedRequest)
                    .hasHeader("Authorization"))
                    .isNotNull()
                    .hasMessageContaining("Expected header Authorization to be present");
        }

        @Test
        void shouldCheckNoHeader_WhenHeaderIsAbsent() {
            assertThatCode(() -> assertThatRecordedRequest(recordedRequest)
                    .hasNoHeader("Authorization"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCheckNoHeader_WhenHeaderIsPresent() {
            assertThatThrownBy(() -> assertThatRecordedRequest(recordedRequest)
                    .hasNoHeader("Accept"))
                    .isNotNull()
                    .hasMessageContaining("Expected header Accept not to be present");
        }

        @Test
        void shouldCheckHeaderSatisfying_WhenValueSatisfiesCondition() {
            assertThatCode(() -> assertThatRecordedRequest(recordedRequest)
                    .hasHeaderSatisfying("Accept", value -> {
                        assertThat(value).startsWith("text/html");
                        assertThat(value).contains("charset");
                    }))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCheckHeaderSatisfying_WhenValueDoesNotSatisfyCondition() {
            assertThatThrownBy(() -> assertThatRecordedRequest(recordedRequest)
                    .hasHeaderSatisfying("Accept", value ->
                            assertThat(value).isEqualTo("application/json")))
                    .isNotNull()
                    .hasMessageContaining("application/json");
        }

        @Test
        void shouldCheckHeaderSatisfying_WhenHeaderIsAbsent() {
            assertThatThrownBy(() -> assertThatRecordedRequest(recordedRequest)
                    .hasHeaderSatisfying("Authorization", value ->
                            assertThat(value).isNotNull()))
                    .isNotNull()
                    .hasMessageContaining("Expected header Authorization to be present");
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
    }

    @Nested
    class Tls {

        @Test
        void shouldPassWhenRequestIsTls() {
            var tlsRequest = mock(RecordedRequest.class);
            when(tlsRequest.getTlsVersion()).thenReturn(TlsVersion.TLS_1_3);

            assertThatCode(() -> assertThatRecordedRequest(tlsRequest).isTls())
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldFailWhenRequestIsNotTls_ButTlsExpected() {
            var nonTlsRequest = mock(RecordedRequest.class);
            when(nonTlsRequest.getTlsVersion()).thenReturn(null);

            assertThatThrownBy(() -> assertThatRecordedRequest(nonTlsRequest).isTls())
                    .isNotNull()
                    .hasMessageContaining("Expected request to use TLS");
        }

        @Test
        void shouldFailWhenRequestIsTls_ButNotTlsExpected() {
            var tlsRequest = mock(RecordedRequest.class);
            when(tlsRequest.getTlsVersion()).thenReturn(TlsVersion.TLS_1_3);

            assertThatThrownBy(() -> assertThatRecordedRequest(tlsRequest).isNotTls())
                    .isNotNull()
                    .hasMessageContaining("Expected request not to use TLS");
        }
    }

    @Nested
    class HasPath {

        @Test
        void shouldPass_WhenGivenTemplateAndValidArguments() {
            server.enqueue(new MockResponse());
            JdkHttpClients.get(httpClient, uri("/users/42"));

            var recordedRequest = takeRequest();

            var assertions = RecordedRequestAssertions.assertThat(recordedRequest);

            assertThatCode(() -> assertions.hasPath("/users/{}", 42))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldFailsWhenGiven_TemplateAndInvalidArguments() {
            server.enqueue(new MockResponse());
            JdkHttpClients.get(httpClient, uri("/users/42"));

            var recordedRequest = takeRequest();

            var assertions = RecordedRequestAssertions.assertThat(recordedRequest);

            assertThatThrownBy(() -> assertions.hasPath("/users/{}", 84))
                    .isNotNull()
                    .hasMessageContaining("Expected path to be: /users/84");
        }

        @Test
        void shouldCheckPathStartingWith() {
            server.enqueue(new MockResponse());
            JdkHttpClients.get(httpClient, uri("/users/42"));

            var recordedRequest = takeRequest();

            assertThatCode(() -> assertThatRecordedRequest(recordedRequest)
                    .hasPathStartingWith("/users"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCheckInvalidPathStartingWith() {
            server.enqueue(new MockResponse());
            JdkHttpClients.get(httpClient, uri("/users/42"));

            var recordedRequest = takeRequest();

            assertThatThrownBy(() -> assertThatRecordedRequest(recordedRequest)
                    .hasPathStartingWith("/orders"))
                    .isNotNull()
                    .hasMessageContaining("Expected path to start with: /orders");
        }

        @Test
        void shouldCheckPathContaining() {
            server.enqueue(new MockResponse());
            JdkHttpClients.get(httpClient, uri("/users/42/settings"));

            var recordedRequest = takeRequest();

            assertThatCode(() -> assertThatRecordedRequest(recordedRequest)
                    .hasPathContaining("/42/"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCheckInvalidPathContaining() {
            server.enqueue(new MockResponse());
            JdkHttpClients.get(httpClient, uri("/users/42/settings"));

            var recordedRequest = takeRequest();

            assertThatThrownBy(() -> assertThatRecordedRequest(recordedRequest)
                    .hasPathContaining("/99/"))
                    .isNotNull()
                    .hasMessageContaining("Expected path to contain: /99/");
        }

        @Test
        void shouldCheckPathEndingWith() {
            server.enqueue(new MockResponse());
            JdkHttpClients.get(httpClient, uri("/users/42"));

            var recordedRequest = takeRequest();

            assertThatCode(() -> assertThatRecordedRequest(recordedRequest)
                    .hasPathEndingWith("/42"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldCheckInvalidPathEndingWith() {
            server.enqueue(new MockResponse());
            JdkHttpClients.get(httpClient, uri("/users/42"));

            var recordedRequest = takeRequest();

            assertThatThrownBy(() -> assertThatRecordedRequest(recordedRequest)
                    .hasPathEndingWith("/99"))
                    .isNotNull()
                    .hasMessageContaining("Expected path to end with: /99");
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
        return serverExtension.uri(path);
    }
}
