package org.kiwiproject.test.jaxrs.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.kiwiproject.jaxrs.exception.ErrorMessage;
import org.kiwiproject.jaxrs.exception.JaxrsBadRequestException;
import org.kiwiproject.jaxrs.exception.JaxrsException;
import org.kiwiproject.jaxrs.exception.JaxrsExceptionMapper;
import org.kiwiproject.jaxrs.exception.JaxrsNotFoundException;
import org.kiwiproject.test.jaxrs.RequestResponseLogger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@DisplayName("JaxrsExceptionTestHelper")
class JaxrsExceptionTestHelperTest {

    @Nested
    class ToJaxrsExceptionOutboundResponse {

        @Test
        void shouldConvert() {
            var originalException = new JaxrsNotFoundException("MyEntity", 42);
            var response = JaxrsExceptionMapper.buildResponse(originalException);

            var jaxrsException = JaxrsExceptionTestHelper.toJaxrsException(response);

            assertThat(jaxrsException).isEqualToComparingOnlyGivenFields(originalException,
                    "statusCode", "message", "errors", "otherData");
        }

        @Test
        void shouldConvert_WhenNullResponseEntity() {
            var response = Response.serverError().build();
            var jaxrsException = JaxrsExceptionTestHelper.toJaxrsException(response);

            assertThat(jaxrsException.getStatusCode()).isEqualTo(500);
            var reasonPhrase = Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase();
            assertThat(jaxrsException.getMessage()).isEqualTo(reasonPhrase);
            assertThat(jaxrsException.getOtherData()).isEmpty();
            assertThat(jaxrsException.getErrors()).containsExactly(
                    new ErrorMessage(500, reasonPhrase)
            );
        }

        @Test
        void shouldConvert_WithNonMapEntity() {
            var response = Response.status(404).entity("It was not found").build();
            var jaxrsException = JaxrsExceptionTestHelper.toJaxrsException(response);

            assertThat(jaxrsException.getStatusCode()).isEqualTo(404);
            assertThat(jaxrsException.getMessage()).isEqualTo("It was not found");
            assertThat(jaxrsException.getOtherData()).isEmpty();
            assertThat(jaxrsException.getErrors()).containsExactly(
                    new ErrorMessage(404, "It was not found")
            );
        }
    }

    @Path("/inbound")
    @Produces(MediaType.APPLICATION_JSON)
    public static class ResourceForInboundResponseTests {

        @GET
        @Path("/forbidden")
        public Response forbidden() {
            return Response.status(403).build();
        }

        @GET
        @Path("/forbidden/entity")
        public Response forbiddenWithEntity() {
            return Response.status(403)
                    .entity(Map.of(
                            "errors", List.of(
                                    new ErrorMessage(403, "Forbidden to access Foo"),
                                    new ErrorMessage(403, "Forbidden to access Bar")
                            ),
                            "otherData1", "value1",
                            "otherData2", 42
                    ))
                    .build();
        }
    }

    @Nested
    @ExtendWith(DropwizardExtensionsSupport.class)
    class ToJaxrsExceptionInboundResponse {

        private final ResourceExtension resources = ResourceExtension.builder()
                .bootstrapLogging(false)
                .addResource(new ResourceForInboundResponseTests())
                .build();

        private Client client;

        @BeforeEach
        void setUp() {
            client = resources.client();
            RequestResponseLogger.turnOnRequestResponseLogging(client);
        }

        @Test
        void shouldConvert_WhenNoEntityInResponse() {
            var response = client.target("/inbound/forbidden").request().get();
            var jaxrsException = JaxrsExceptionTestHelper.toJaxrsException(response);

            assertThat(jaxrsException.getStatusCode()).isEqualTo(403);
            assertThat(jaxrsException.getMessage()).isEqualTo("Forbidden");
            assertThat(jaxrsException.getOtherData()).isEmpty();
            assertThat(jaxrsException.getErrors()).containsExactly(
                    new ErrorMessage(403, "Forbidden")
            );
        }

        @Test
        void shouldConvert_WhenResponseHasEntity() {
            var response = client.target("/inbound/forbidden/entity").request().get();
            var jaxrsException = JaxrsExceptionTestHelper.toJaxrsException(response);

            assertThat(jaxrsException.getStatusCode()).isEqualTo(403);
            assertThat(jaxrsException.getMessage()).isEqualTo("Forbidden to access Foo");
            assertThat(jaxrsException.getOtherData()).containsOnly(
                    entry("otherData1", "value1"),
                    entry("otherData2", 42)
            );
            assertThat(jaxrsException.getErrors()).containsExactly(
                    new ErrorMessage(403, "Forbidden to access Foo"),
                    new ErrorMessage(403, "Forbidden to access Bar")
            );
        }
    }

    @Test
    void shouldGetFirstErrorMessage() {
        var jaxrsException = new JaxrsBadRequestException("This was a bad request");
        var response = JaxrsExceptionMapper.buildResponse(jaxrsException);

        assertThat(JaxrsExceptionTestHelper.getFirstErrorMessage(response))
                .isEqualTo(new ErrorMessage(400, "This was a bad request"));
    }

    @Test
    void shouldGetAllMessages() {
        var statusCode = 422;
        var firstNameError = new ErrorMessage(statusCode, "firstName must not be blank");
        var lastNameError = new ErrorMessage(statusCode, "lastName must not be blank");
        var addressError = new ErrorMessage(statusCode, "address must be supplied");
        var jaxrsException = new JaxrsException(List.of(firstNameError, lastNameError, addressError), statusCode);
        var response = JaxrsExceptionMapper.buildResponse(jaxrsException);

        assertThat(JaxrsExceptionTestHelper.getErrorMessages(response))
                .containsExactlyInAnyOrder(firstNameError, lastNameError, addressError);
    }

    @Nested
    class AssertHasError {

        @Test
        void shouldNotThrow_WhenResponseContainsError() {
            var error = new ErrorMessage("Oops");
            var jaxrsException = new JaxrsException(error);
            var response = JaxrsExceptionMapper.buildResponse(jaxrsException);

            assertThatCode(() -> JaxrsExceptionTestHelper.assertHasError(response, error))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldThrow_WhenResponseDoesNotContainError() {
            var jaxrsException = new JaxrsNotFoundException("Not found");
            var response = JaxrsExceptionMapper.buildResponse(jaxrsException);

            var otherError = new ErrorMessage("Other error");
            assertThatThrownBy(() -> JaxrsExceptionTestHelper.assertHasError(response, otherError))
                    .isInstanceOf(AssertionError.class);
        }
    }

    @Nested
    class AssertHasErrors {

        @Test
        void shouldNotThrow_WhenResponseContainsAllTheErrors() {
            var statusCode = 422;
            var firstNameError = new ErrorMessage(statusCode, "firstName must not be blank");
            var lastNameError = new ErrorMessage(statusCode, "lastName must not be blank");
            var jaxrsException = new JaxrsException(List.of(firstNameError, lastNameError), statusCode);
            var response = JaxrsExceptionMapper.buildResponse(jaxrsException);

            var expectedErrors = List.of(lastNameError, firstNameError);
            assertThatCode(() -> JaxrsExceptionTestHelper.assertHasErrors(response, expectedErrors))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldThrow_WhenResponseDoesNotContainAllTheErrors() {
            var statusCode = 422;
            var firstNameError = new ErrorMessage(statusCode, "firstName must not be blank");
            var addressError = new ErrorMessage(statusCode, "address must be supplied");
            var jaxrsException = new JaxrsException(List.of(firstNameError, addressError), statusCode);
            var response = JaxrsExceptionMapper.buildResponse(jaxrsException);

            var lastNameError = new ErrorMessage(statusCode, "lastName must not be blank");
            var expectedErrors = List.of(addressError, lastNameError, firstNameError);
            assertThatThrownBy(() -> JaxrsExceptionTestHelper.assertHasErrors(response, expectedErrors))
                    .isInstanceOf(AssertionError.class);
        }
    }

    @Nested
    class AssertContainsError {

        @ParameterizedTest
        @CsvSource({
                "500,Something bad happened",
                "500,Something bad",
                "401,You shall not pass!",
                "401,shall not pass",
                "400,Invalid request",
                "400,Invalid"
        })
        void shouldNotThrow_WhenResponseContainsTheError(int statusCode, String substring) {
            var errorMessages = List.of(
                    new ErrorMessage(400, "Invalid request"),
                    new ErrorMessage(401, "You shall not pass!"),
                    new ErrorMessage(500, "Something bad happened")
            );
            var jaxrsException = new JaxrsException(errorMessages, 500);
            var response = JaxrsExceptionMapper.buildResponse(jaxrsException);

            var errorMessage = JaxrsExceptionTestHelper.assertContainsError(response, statusCode, substring);
            assertThat(errorMessage.getCode()).isEqualTo(statusCode);
            assertThat(errorMessage.getMessage()).contains(substring);
        }

        @ParameterizedTest
        @CsvSource({
                "401,A different message",
                "403,You shall not pass!",
                "500,Exception was thrown",
        })
        void shouldThrow_WhenResponseDoesNotContainTheError(int statusCode, String substring) {
            var jaxrsException = new JaxrsException(new ErrorMessage(401, "You shall not pass!"));
            var response = JaxrsExceptionMapper.buildResponse(jaxrsException);

            assertThatThrownBy(() ->
                    JaxrsExceptionTestHelper.assertContainsError(response, statusCode, substring))
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Response does not contain an ErrorMessage having status %d and message containing: %s",
                            statusCode, substring);
        }
    }
}
