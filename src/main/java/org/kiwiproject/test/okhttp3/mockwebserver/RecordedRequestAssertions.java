package org.kiwiproject.test.okhttp3.mockwebserver;

import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import okhttp3.TlsVersion;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.Assertions;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

/**
 * Provides for fluent {@link RecordedRequest} tests using AssertJ assertions.
 * <p>
 * All methods return 'this' to facilitate a fluent API via method chaining.
 * <p>
 * Note that MockWebServer (com.squareup.okhttp3:mockwebserver) and OkHttp
 * dependencies must be available at runtime. OkHttp is a transitive dependency
 * of mockwebserver, so you should only need to add mockwebserver.
 */
@CanIgnoreReturnValue
public class RecordedRequestAssertions {

    // reference: https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods
    static final List<String> METHODS_ALLOWING_BODY = List.of("DELETE", "PATCH", "POST", "PUT");

    private final RecordedRequest recordedRequest;

    private RecordedRequestAssertions(RecordedRequest recordedRequest) {
        this.recordedRequest = requireNotNull(recordedRequest);
    }

    /**
     * Starting point for fluent assertions on {@link RecordedRequest}.
     *
     * @param recordedRequest the {@link RecordedRequest} to assert upon
     * @return this instance
     */
    public static RecordedRequestAssertions assertThat(RecordedRequest recordedRequest) {
        return assertThatRecordedRequest(recordedRequest);
    }

    /**
     * Starting point for fluent assertions on {@link RecordedRequest}.
     * <p>
     * This method is provided as an alias of {@link #assertThat(RecordedRequest)} to avoid conflicts
     * when statically importing AssertJ's {@code Assertions#assertThat}, and therefore allow both
     * to be statically imported.
     *
     * @param recordedRequest the {@link RecordedRequest} to assert upon
     * @return this instance
     */
    public static RecordedRequestAssertions assertThatRecordedRequest(RecordedRequest recordedRequest) {
        return new RecordedRequestAssertions(recordedRequest);
    }

    // References on HTTP methods:
    //  - Request Line:    https://datatracker.ietf.org/doc/html/rfc7230#section-3.1.1
    //  - Request Methods: https://datatracker.ietf.org/doc/html/rfc7231#section-4
    //  - PATCH method:    https://datatracker.ietf.org/doc/html/rfc5789
    //  - Mozilla - HTTP request methods - https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods

    /**
     * Asserts the recorded request is a GET request.
     *
     * @return this instance
     */
    public RecordedRequestAssertions isGET() {
        return hasMethod("GET");
    }

    /**
     * Asserts the recorded request is a POST request.
     *
     * @return this instance
     */
    public RecordedRequestAssertions isPOST() {
        return hasMethod("POST");
    }

    /**
     * Asserts the recorded request is a PUT request.
     *
     * @return this instance
     */
    public RecordedRequestAssertions isPUT() {
        return hasMethod("PUT");
    }

    /**
     * Asserts the recorded request is a DELETE request.
     *
     * @return this instance
     */
    public RecordedRequestAssertions isDELETE() {
        return hasMethod("DELETE");
    }

    /**
     * Asserts the recorded request is a HEAD request.
     *
     * @return this instance
     */
    public RecordedRequestAssertions isHEAD() {
        return hasMethod("HEAD");
    }

    /**
     * Asserts the recorded request is a CONNECT request.
     *
     * @return this instance
     */
    public RecordedRequestAssertions isCONNECT() {
        return hasMethod("CONNECT");
    }

    /**
     * Asserts the recorded request is an OPTIONS request.
     *
     * @return this instance
     */
    public RecordedRequestAssertions isOPTIONS() {
        return hasMethod("OPTIONS");
    }

    /**
     * Asserts the recorded request is a TRACE request.
     *
     * @return this instance
     */
    public RecordedRequestAssertions isTRACE() {
        return hasMethod("TRACE");
    }

    /**
     * Asserts the recorded request is a PATCH request.
     *
     * @return this instance
     */
    public RecordedRequestAssertions isPATCH() {
        return hasMethod("PATCH");
    }

    /**
     * Asserts the recorded request has the expected HTTP method.
     *
     * @param method the expected request method
     * @return this instance
     */
    public RecordedRequestAssertions hasMethod(String method) {
        Assertions.assertThat(recordedRequest.getMethod())
                .describedAs("Expected method to be %s", method)
                .isEqualTo(method);

        return this;
    }

    /**
     * Asserts the recorded request has the expected HTTP request line.
     *
     * @param requestLine the expected request line
     * @return this instance
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Messages">HTTP Messages</a>
     */
    public RecordedRequestAssertions hasRequestLine(String requestLine) {
        Assertions.assertThat(recordedRequest.getRequestLine())
                .describedAs("Expected request line to be: %s", requestLine)
                .isEqualTo(requestLine);

        return this;
    }

    /**
     * Asserts the recorded request has the expected request URL.
     *
     * @param requestUrl the expected request URL (as a {@link URI})
     * @return this instance
     */
    public RecordedRequestAssertions hasRequestUrl(URI requestUrl) {
        return hasRequestUrl(requestUrl.toString());
    }

    /**
     * Asserts the recorded request has the expected request URL.
     *
     * @param requestUrl the expected request URL
     * @return this instance
     */
    public RecordedRequestAssertions hasRequestUrl(String requestUrl) {
        Assertions.assertThat(recordedRequest.getRequestUrl())
                .describedAs("Expected request URL to be: %s", requestUrl)
                .isNotNull()
                .hasToString(requestUrl);

        return this;
    }

    /**
     * Asserts the recorded request has the expected path.
     *
     * @param path the expected path
     * @return this instance
     */
    public RecordedRequestAssertions hasPath(String path) {
        Assertions.assertThat(recordedRequest.getPath())
                .describedAs("Expected path to be: %s", path)
                .isEqualTo(path);

        return this;
    }

    /**
     * Asserts the recorded request has the expected header name and value.
     *
     * @param name  the expected HTTP header name
     * @param value the expected HTTP header value
     * @return this instance
     */
    public RecordedRequestAssertions hasHeader(String name, Object value) {
        Assertions.assertThat(recordedRequest.getHeader(name))
                .describedAs("Expected %s header to have value: %s", name, value)
                .isEqualTo(value);

        return this;
    }

    /**
     * Asserts the recorded request does not have a request body.
     * <p>
     * Only DELETE, PATCH, POST, and PUT may have a body.
     *
     * @return this instance
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods">HTTP request methods</a>
     */
    public RecordedRequestAssertions hasNoBody() {
        var bodyBuffer = recordedRequest.getBody();
        Assertions.assertThat(bodyBuffer.size())
                .describedAs("Expected there not to be a request body but found: %s",
                        bodyBuffer.readUtf8())
                .isZero();

        return this;
    }

    /**
     * Asserts the recorded request has the expected body.
     * <p>
     * Only DELETE, PATCH, POST, and PUT may have a body.
     *
     * @param body the expected request body (assumes UTF-8 character encoding)
     * @return this instance
     * @see okio.Buffer#readUtf8()
     */
    public RecordedRequestAssertions hasBody(String body) {
        checkMethodAllowsBody();

        var bodyBuffer = recordedRequest.getBody();
        var actualBodyUtf8 = bodyBuffer.readUtf8();
        Assertions.assertThat(actualBodyUtf8)
                .describedAs("Expected body as UTF-8 to be: %s", body)
                .isEqualTo(body);

        return this;
    }

    /**
     * Asserts the recorded request has a body of the given size.
     *
     * @param size the expected body size
     * @return this instance
     */
    public RecordedRequestAssertions hasBodySize(long size) {
        checkMethodAllowsBody();

        Assertions.assertThat(recordedRequest.getBodySize())
                .describedAs("Expected body size: %d bytes", size)
                .isEqualTo(size);

        return this;
    }

    private void checkMethodAllowsBody() {
        var method = recordedRequest.getMethod();

        Assertions.assertThat(method)
                .describedAs("Body not allowed for method %s. The request method should be one of %s",
                        method, METHODS_ALLOWING_BODY)
                .isIn(METHODS_ALLOWING_BODY);
    }

    /**
     * Asserts the recorded request is not TLS, i.e., is an HTTP request not HTTPS.
     *
     * @return this instance
     */
    public RecordedRequestAssertions isNotTls() {
        Assertions.assertThat(recordedRequest.getTlsVersion())
                .describedAs("Expected request not to use TLS")
                .isNull();

        return this;
    }

    /**
     * Asserts the recorded request is TLS with the expected version.
     *
     * @param tlsVersion the expected TLS version
     * @return this instance
     */
    public RecordedRequestAssertions hasTlsVersion(TlsVersion tlsVersion) {
        Assertions.assertThat(recordedRequest.getTlsVersion())
                .describedAs("Expected TLS version to be %s", tlsVersion)
                .isEqualTo(tlsVersion);

        return this;
    }

    /**
     * Asserts the recorded request does not have a failure, which is an {@link IOException}.
     * <p>
     * Note that usually an {@link IOException} is thrown by the code making the HTTP request.
     * For example, if you set the {@link okhttp3.mockwebserver.SocketPolicy SocketPolicy} to
     * {@link okhttp3.mockwebserver.SocketPolicy#DISCONNECT_AT_START DISCONNECT_AT_START} or
     * {@link okhttp3.mockwebserver.SocketPolicy#DISCONNECT_DURING_REQUEST_BODY DISCONNECT_DURING_REQUEST_BODY},
     * then the HTTP client code throws {@link IOException} but the {@link RecordedRequest}
     * <em>usually</em> does not contain that failure.
     *
     * @return this instance
     * @see RecordedRequest#getFailure()
     */
    public RecordedRequestAssertions hasNoFailure() {
        Assertions.assertThat(recordedRequest.getFailure())
                .describedAs("Expected request not to have failure")
                .isNull();

        return this;
    }

    /**
     * Asserts the recorded request has a failure with the given message.
     *
     * @param failureMessage the expected message from the {@link IOException}
     * @return this instance
     * @see RecordedRequest#getFailure()
     */
    public RecordedRequestAssertions hasFailureMessage(String failureMessage) {
        Assertions.assertThat(recordedRequest.getFailure())
                .describedAs("Expected a failure with message: %s", failureMessage)
                .hasMessage(failureMessage);

        return this;
    }

    /**
     * Asserts the recorded request has a failure whose message contains the given value.
     *
     * @param failureMessage the expected partial message from the {@link IOException}
     * @return this instance
     * @see RecordedRequest#getFailure()
     */
    public RecordedRequestAssertions hasFailureMessageContaining(String failureMessage) {
        Assertions.assertThat(recordedRequest.getFailure())
                .describedAs("Expected a failure with message that contains: %s", failureMessage)
                .hasMessageContaining(failureMessage);

        return this;
    }

    /**
     * Asserts the recorded request has a failure whose message starts with the given value.
     *
     * @param failureMessage the expected partial message from the {@link IOException}
     * @return this instance
     * @see RecordedRequest#getFailure()
     */
    public RecordedRequestAssertions hasFailureMessageStartingWith(String failureMessage) {
        Assertions.assertThat(recordedRequest.getFailure())
                .describedAs("Expected a failure with message that starts with: %s", failureMessage)
                .hasMessageStartingWith(failureMessage);

        return this;
    }

    /**
     * Asserts the recorded request has a failure whose message ends with the given value.
     *
     * @param failureMessage the expected partial message from the {@link IOException}
     * @return this instance
     * @see RecordedRequest#getFailure()
     */
    public RecordedRequestAssertions hasFailureMessageEndingWith(String failureMessage) {
        Assertions.assertThat(recordedRequest.getFailure())
                .describedAs("Expected a failure with message that ends with: %s", failureMessage)
                .hasMessageEndingWith(failureMessage);

        return this;
    }

    /**
     * Asserts the recorded request has a failure ({@link IOException}) with the given cause.
     *
     * @param causeType the cause, obtained via {@link IOException#getCause()}
     * @return this instance
     * @see RecordedRequest#getFailure()
     */
    public RecordedRequestAssertions hasFailureCauseInstanceOf(Class<?> causeType) {
        Assertions.assertThat(recordedRequest.getFailure())
                .describedAs("Expected request to have failure of type: %s", causeType.getName())
                .isInstanceOf(causeType);

        return this;
    }

    /**
     * Asserts the recorded request has a failure that satisfies assertions
     * provided by the {@code failureConsumer}.
     *
     * @param failureConsumer the {@link Consumer} containing the assertions on the recorded request failure.
     * @return this instance
     * @see RecordedRequest#getFailure()
     */
    public RecordedRequestAssertions hasFailure(Consumer<IOException> failureConsumer) {
        failureConsumer.accept(recordedRequest.getFailure());

        return this;
    }
}
