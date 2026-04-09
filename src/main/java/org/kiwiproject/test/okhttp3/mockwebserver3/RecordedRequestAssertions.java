package org.kiwiproject.test.okhttp3.mockwebserver3;

import static org.kiwiproject.base.KiwiPreconditions.checkArgumentNotNull;
import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;
import static org.kiwiproject.base.KiwiStrings.f;
import static org.kiwiproject.test.constants.KiwiTestConstants.JSON_HELPER;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import mockwebserver3.RecordedRequest;
import org.assertj.core.api.Assertions;
import org.kiwiproject.json.JsonHelper;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Provides for fluent {@link RecordedRequest} tests using AssertJ assertions.
 * <p>
 * All methods return 'this' to facilitate a fluent API via method chaining.
 * <p>
 * Note that mockwebserver3 (com.squareup.okhttp3:mockwebserver3) and OkHttp
 * dependencies must be available at runtime. OkHttp is a transitive dependency
 * of mockwebserver3, so you should only need to add mockwebserver3.
 */
@CanIgnoreReturnValue
public class RecordedRequestAssertions {

    // reference: https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods
    static final List<String> METHODS_ALLOWING_BODY = List.of("DELETE", "PATCH", "POST", "PUT");

    private final RecordedRequest recordedRequest;

    private RecordedRequestAssertions(RecordedRequest recordedRequest) {
        this.recordedRequest = requireNotNull(recordedRequest, "recordedRequest must not be null");
    }

    /**
     * Starting point for fluent assertions on {@link RecordedRequest}.
     *
     * @param recordedRequest the {@link RecordedRequest} to assert upon
     * @return this instance
     * @throws IllegalArgumentException if {@code recordedRequest} is {@code null}
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
     * @throws IllegalArgumentException if {@code recordedRequest} is {@code null}
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
     * @param url the expected request URL (as a {@link URI})
     * @return this instance
     */
    public RecordedRequestAssertions hasUrl(URI url) {
        return hasUrl(url.toString());
    }

    /**
     * Asserts the recorded request has the expected request URL.
     *
     * @param url the expected request URL
     * @return this instance
     */
    public RecordedRequestAssertions hasUrl(String url) {
        Assertions.assertThat(recordedRequest.getUrl())
                .describedAs("Expected request URL to be: %s", url)
                .isNotNull()
                .hasToString(url);

        return this;
    }

    /**
     * Asserts the recorded request has the expected request target (path and optional query string).
     *
     * @param target the expected request target
     * @return this instance
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc7230#section-3.1.1">RFC 7230 - Request Line</a>
     */
    public RecordedRequestAssertions hasTarget(String target) {
        Assertions.assertThat(recordedRequest.getTarget())
                .describedAs("Expected target to be: %s", target)
                .isEqualTo(target);

        return this;
    }

    /**
     * Asserts the recorded request has the expected request target (path and optional query string).
     * <p>
     * Uses {@link org.kiwiproject.base.KiwiStrings#f(String, Object...)}
     * to produce the expected target from the template and arguments.
     *
     * @param targetTemplate the template to use when constructing the expected target
     * @param arguments      the arguments for the template
     * @return this instance
     */
    public RecordedRequestAssertions hasTarget(String targetTemplate, Object... arguments) {
        var expectedTarget = f(targetTemplate, arguments);
        return hasTarget(expectedTarget);
    }

    /**
     * Asserts the recorded request has the expected header name and value.
     *
     * @param name  the expected HTTP header name
     * @param value the expected HTTP header value
     * @return this instance
     */
    public RecordedRequestAssertions hasHeader(String name, Object value) {
        Assertions.assertThat(recordedRequest.getHeaders().get(name))
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
        Assertions.assertThat(recordedRequest.getBodySize())
                .describedAs("Expected there not to be a request body but found: %s", bodyUtf8())
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
     * @see okio.ByteString#utf8()
     */
    public RecordedRequestAssertions hasBody(String body) {
        checkMethodAllowsBody();

        var actualBodyUtf8 = bodyUtf8();
        Assertions.assertThat(actualBodyUtf8)
                .describedAs("Expected body as UTF-8 to be: %s", body)
                .isEqualTo(body);

        return this;
    }

    /**
     * Asserts the recorded request has a body that satisfies assertions in the given consumer.
     *
     * @param bodyConsumer a {@link Consumer} containing one or more assertions on the request body
     * @return this instance
     */
    public RecordedRequestAssertions hasBodySatisfying(Consumer<String> bodyConsumer) {
        checkMethodAllowsBody();

        var actualBodyUtf8 = bodyUtf8();
        bodyConsumer.accept(actualBodyUtf8);

        return this;
    }

    /**
     * Asserts the recorded request has a JSON body that deserializes to the given entity.
     *
     * @param entity the expected request entity
     * @return this instance
     * @implNote uses {@link org.kiwiproject.test.constants.KiwiTestConstants#JSON_HELPER KiwiTestConstants#JSON_HELPER}
     * to deserialize JSON. You can use the overloaded methods if you need more control over the JSON deserialization.
     * @see #hasJsonBodyWithEntity(Object, ObjectMapper)
     * @see #hasJsonBodyWithEntity(Object, JsonHelper)
     */
    public RecordedRequestAssertions hasJsonBodyWithEntity(Object entity) {
        return hasJsonBodyWithEntity(entity, JSON_HELPER);
    }

    /**
     * Asserts the recorded request has a JSON body that deserializes to the given entity.
     *
     * @param entity       the expected request entity
     * @param objectMapper the Jackson {@link ObjectMapper} to use for deserializing JSON
     * @return this instance
     */
    public RecordedRequestAssertions hasJsonBodyWithEntity(Object entity, ObjectMapper objectMapper) {
        return hasJsonBodyWithEntity(entity, new JsonHelper(objectMapper));
    }

    /**
     * Asserts the recorded request has a JSON body that deserializes to the given entity.
     *
     * @param entity     the expected request entity
     * @param jsonHelper the {@link JsonHelper} to use for deserializing JSON
     * @return this instance
     */
    public RecordedRequestAssertions hasJsonBodyWithEntity(Object entity, JsonHelper jsonHelper) {
        checkMethodAllowsBody();
        checkArgumentNotNull(entity, "entity must not be null");

        var actualBodyUtf8 = bodyUtf8();

        var jsonDetectionResult = jsonHelper.detectJson(actualBodyUtf8);
        Assertions.assertThat(jsonDetectionResult.isJson())
                .describedAs("Body content expected to be JSON")
                .isTrue();

        var actualEntity = jsonHelper.toObject(actualBodyUtf8, entity.getClass());
        Assertions.assertThat(actualEntity)
                .usingRecursiveComparison()
                .isEqualTo(entity);

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

    private String bodyUtf8() {
        var body = recordedRequest.getBody();
        return Objects.isNull(body) ? "" : body.utf8();
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
        Assertions.assertThat(recordedRequest.getHandshake())
                .describedAs("Expected request not to use TLS")
                .isNull();

        return this;
    }

    /**
     * Asserts the recorded request is TLS, i.e., is an HTTPS request.
     *
     * @return this instance
     */
    public RecordedRequestAssertions isTls() {
        Assertions.assertThat(recordedRequest.getHandshake())
                .describedAs("Expected request to use TLS")
                .isNotNull();

        return this;
    }

    /**
     * Asserts the recorded request does not have a failure, which is an {@link IOException}.
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
