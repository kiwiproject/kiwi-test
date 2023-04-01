package org.kiwiproject.test.jaxrs.exception;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.kiwiproject.base.KiwiStrings.f;
import static org.kiwiproject.collect.KiwiLists.first;

import lombok.experimental.UtilityClass;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.kiwiproject.jaxrs.exception.ErrorMessage;
import org.kiwiproject.jaxrs.exception.JaxrsException;
import org.kiwiproject.jaxrs.exception.JaxrsExceptionMapper;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * Test utility methods for testing {@link JaxrsException} objects.
 */
@UtilityClass
public class JaxrsExceptionTestHelper {

    /**
     * Converts an error {@link Response} to the {@link JaxrsException} or an appropriate subclass.
     *
     * @param response a JAX-RS {@link OutboundJaxrsResponse} or {@code InboundJaxrsResponse} (not public)
     * @return JaxrsException equivalent of error response
     */
    public static JaxrsException toJaxrsException(Response response) {
        if (response instanceof OutboundJaxrsResponse) {
            return jaxrsExceptionForOutboundResponse(response);
        }

        return JaxrsExceptionMapper.toJaxrsException(response);
    }

    private static JaxrsException jaxrsExceptionForOutboundResponse(Response outboundResponse) {
        verify(outboundResponse instanceof OutboundJaxrsResponse, "outboundResponse must be an OutboundJaxrsResponse");

        var status = outboundResponse.getStatus();
        var entity = outboundResponse.getEntity();

        if (isNull(entity)) {
            return new JaxrsException(outboundResponse.getStatusInfo().getReasonPhrase(), status);
        }

        if (entity instanceof Map) {
            // we are assuming the map has String keys
            //noinspection unchecked
            var errors = (Map<String, Object>) entity;
            return JaxrsExceptionMapper.toJaxrsException(status, errors);
        }

        return new JaxrsException(entity.toString(), status);
    }

    /**
     * Returns the first {@link ErrorMessage} in the given response.
     * <p>
     * The response is expected to conform to the structure of a {@link JaxrsException}, i.e. it has a list of
     * {@link ErrorMessage} under the "errors" key in the JSON response entity.
     *
     * @param response a JAX-RS {@link OutboundJaxrsResponse} or {@code InboundJaxrsResponse} (not public)
     * @return first {@link ErrorMessage} in the response entity
     * @throws IllegalArgumentException if there isn't at least one ErrorMessage
     */
    public static ErrorMessage getFirstErrorMessage(Response response) {
        var jaxrsException = toJaxrsException(response);
        return first(jaxrsException.getErrors());
    }

    /**
     * Returns a list of the {@link ErrorMessage} objects in the given response.
     * <p>
     * The response is expected to conform to the structure of a {@link JaxrsException}, i.e. it has a list of
     * {@link ErrorMessage} under the "errors" key in the JSON response entity.
     *
     * @param response a JAX-RS {@link OutboundJaxrsResponse} or {@code InboundJaxrsResponse} (not public)
     * @return List containing all {@link ErrorMessage} in the response entity
     */
    public static List<ErrorMessage> getErrorMessages(Response response) {
        return toJaxrsException(response).getErrors();
    }

    /**
     * Asserts that the response contains the given {@link ErrorMessage}.
     *
     * @param response     a JAX-RS {@link OutboundJaxrsResponse} or {@code InboundJaxrsResponse} (not public)
     * @param errorMessage the ErrorMessage that should be in response
     */
    public static void assertHasError(Response response, ErrorMessage errorMessage) {
        var jaxrsException = toJaxrsException(response);
        assertThat(jaxrsException.getErrors()).contains(errorMessage);
    }

    /**
     * Asserts that the response contains all the given {@link ErrorMessage} objects.
     *
     * @param response      a JAX-RS {@link OutboundJaxrsResponse} or {@code InboundJaxrsResponse} (not public)
     * @param errorMessages the ErrorMessage objects that should be in response
     */
    public static void assertHasErrors(Response response, List<ErrorMessage> errorMessages) {
        var jaxrsException = toJaxrsException(response);
        assertThat(jaxrsException.getErrors()).containsAll(errorMessages);
    }

    /**
     * Asserts that the response contains an {@link ErrorMessage} with the given status code and having a message that
     * contains the given substring.
     *
     * @param response   a JAX-RS {@link OutboundJaxrsResponse} or {@code InboundJaxrsResponse} (not public)
     * @param statusCode the status code expected to be in the {@link ErrorMessage}
     * @param substring  the substring expected to be in {@link ErrorMessage#getMessage()}
     * @return the {@link ErrorMessage} matching the given status code and substring
     */
    public static ErrorMessage assertContainsError(Response response, int statusCode, String substring) {
        var jaxrsException = toJaxrsException(response);

        var errors = jaxrsException.getErrors();
        return errors.stream()
                .filter(error -> matchesArguments(error, statusCode, substring))
                .findFirst()
                .orElseThrow(() -> {
                    var assertErrorMessage =
                            f("Response does not contain an ErrorMessage having status {} and message containing: '{}'. Actual errors: {}",
                                    statusCode, substring, errors);
                    return new AssertionError(assertErrorMessage);
                });
    }

    private static boolean matchesArguments(ErrorMessage error, int statusCode, String substring) {
        return statusCode == error.getCode() &&
                nonNull(error.getMessage()) &&
                error.getMessage().contains(substring);
    }

}
