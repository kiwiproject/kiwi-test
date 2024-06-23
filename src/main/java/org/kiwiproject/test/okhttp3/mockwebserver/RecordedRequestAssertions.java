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

    public RecordedRequestAssertions isGET() {
        return hasMethod("GET");
    }

    public RecordedRequestAssertions isPOST() {
        return hasMethod("POST");
    }

    public RecordedRequestAssertions isPUT() {
        return hasMethod("PUT");
    }

    public RecordedRequestAssertions isDELETE() {
        return hasMethod("DELETE");
    }

    public RecordedRequestAssertions isHEAD() {
        return hasMethod("HEAD");
    }

    public RecordedRequestAssertions isCONNECT() {
        return hasMethod("CONNECT");
    }

    public RecordedRequestAssertions isOPTIONS() {
        return hasMethod("OPTIONS");
    }

    public RecordedRequestAssertions isTRACE() {
        return hasMethod("TRACE");
    }

    public RecordedRequestAssertions isPATCH() {
        return hasMethod("PATCH");
    }

    public RecordedRequestAssertions hasMethod(String method) {
        Assertions.assertThat(recordedRequest.getMethod())
                .describedAs("Expected method to be %s", method)
                .isEqualTo(method);

        return this;
    }

    public RecordedRequestAssertions hasRequestLine(String requestLine) {
        Assertions.assertThat(recordedRequest.getRequestLine())
                .describedAs("Expected request line to be: %s", requestLine)
                .isEqualTo(requestLine);

        return this;
    }

    public RecordedRequestAssertions hasRequestUrl(URI requestUrl) {
        return hasRequestUrl(requestUrl.toString());
    }

    public RecordedRequestAssertions hasRequestUrl(String requestUrl) {
        Assertions.assertThat(recordedRequest.getRequestUrl())
                .describedAs("Expected request URL to be: %s", requestUrl)
                .isNotNull()
                .hasToString(requestUrl);

        return this;
    }

    public RecordedRequestAssertions hasPath(String path) {
        Assertions.assertThat(recordedRequest.getPath())
                .describedAs("Expected path to be: %s", path)
                .isEqualTo(path);

        return this;
    }

    public RecordedRequestAssertions hasHeader(String name, Object value) {
        Assertions.assertThat(recordedRequest.getHeader(name))
                .describedAs("Expected %s header to have value: %s", name, value)
                .isEqualTo(value);

        return this;
    }

    public RecordedRequestAssertions hasNoBody() {
        var bodyBuffer = recordedRequest.getBody();
        Assertions.assertThat(bodyBuffer.size())
                .describedAs("Expected there not to be a request body but found: %s",
                        bodyBuffer.readUtf8())
                .isZero();

        return this;
    }

    public RecordedRequestAssertions hasBody(String body) {
        checkMethodAllowsBody();

        var bodyBuffer = recordedRequest.getBody();
        var actualBodyUtf8 = bodyBuffer.readUtf8();
        Assertions.assertThat(actualBodyUtf8)
                .describedAs("Expected body as UTF-8 to be: %s", body)
                .isEqualTo(body);

        return this;
    }

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

    public RecordedRequestAssertions isNotTls() {
        Assertions.assertThat(recordedRequest.getTlsVersion())
                .describedAs("Expected request not to use TLS")
                .isNull();

        return this;
    }

    public RecordedRequestAssertions hasTlsVersion(TlsVersion tlsVersion) {
        Assertions.assertThat(recordedRequest.getTlsVersion())
                .describedAs("Expected TLS version to be %s", tlsVersion)
                .isEqualTo(tlsVersion);

        return this;
    }

    public RecordedRequestAssertions hasNoFailure() {
        Assertions.assertThat(recordedRequest.getFailure())
                .describedAs("Expected request not to have failure")
                .isNull();

        return this;
    }

    public RecordedRequestAssertions hasFailureMessage(String failureMessage) {
        Assertions.assertThat(recordedRequest.getFailure())
                .describedAs("Expected a failure with message: %s", failureMessage)
                .hasMessage(failureMessage);

        return this;
    }

    public RecordedRequestAssertions hasFailureMessageContaining(String failureMessage) {
        Assertions.assertThat(recordedRequest.getFailure())
                .describedAs("Expected a failure with message that contains: %s", failureMessage)
                .hasMessageContaining(failureMessage);

        return this;
    }

    public RecordedRequestAssertions hasFailureMessageStartingWith(String failureMessage) {
        Assertions.assertThat(recordedRequest.getFailure())
                .describedAs("Expected a failure with message that starts with: %s", failureMessage)
                .hasMessageStartingWith(failureMessage);

        return this;
    }

    public RecordedRequestAssertions hasFailureMessageEndingWith(String failureMessage) {
        Assertions.assertThat(recordedRequest.getFailure())
                .describedAs("Expected a failure with message that ends with: %s", failureMessage)
                .hasMessageEndingWith(failureMessage);

        return this;
    }

    public RecordedRequestAssertions hasFailureCauseInstanceOf(Class<?> causeType) {
        Assertions.assertThat(recordedRequest.getFailure())
                .describedAs("Expected request to have failure of type: %s", causeType.getName())
                .isInstanceOf(causeType);

        return this;
    }

    public RecordedRequestAssertions hasFailure(Consumer<IOException> failureConsumer) {
        failureConsumer.accept(recordedRequest.getFailure());

        return this;
    }
}
