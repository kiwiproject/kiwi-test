package org.kiwiproject.test.jaxrs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.kiwiproject.base.KiwiStrings.f;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import lombok.Value;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kiwiproject.jaxrs.KiwiResponses;
import org.opentest4j.AssertionFailedError;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

@DisplayName("JaxrsTestHelper")
class JaxrsTestHelperTest {

    @Test
    void shouldAssertSameResponseEntity() {
        var alice = new Person("Alice", "Johns");
        var response = Response.ok(alice).build();
        assertThatCode(() -> JaxrsTestHelper.assertResponseEntity(response, alice))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertSameResponseEntity_AndThrow_WhenNotSame() {
        var alice = new Person("Alice", "Johns");
        var response = Response.ok(alice).build();
        var otherAlice = new Person("Alice", "Johns");
        assertThatThrownBy(() -> JaxrsTestHelper.assertResponseEntity(response, otherAlice))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertEqualResponseEntity() {
        var alice = new Person("Alice", "Johns");
        var response = Response.ok(alice).build();
        assertThatCode(() -> JaxrsTestHelper.assertEqualResponseEntity(response, new Person("Alice", "Johns")))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertEqualResponseEntity_AndThrow_WhenNotEqual() {
        var alice = new Person("Alice", "Johns");
        var response = Response.ok(alice).build();
        var expectedEntity = new Person("Alice", "Smith");
        assertThatThrownBy(() -> JaxrsTestHelper.assertEqualResponseEntity(response, expectedEntity))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldCreateMockUriInfoWithRequestUriBuilder() {
        var mock = JaxrsTestHelper.createMockUriInfoWithRequestUriBuilder("/base");
        assertThat(mock.getRequestUriBuilder().path("foo")).hasToString("/base/foo");
    }

    @Test
    void shouldAssertJsonResponseType() {
        var response = Response.ok(new Person("Bob", "Sackamano"), "application/json").build();
        assertThatCode(() -> JaxrsTestHelper.assertJsonResponse(response))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertJsonResponseType_AndThrow_WhenNotJson() {
        var response = Response.ok(new Person("Bob", "Sackamano"), "application/xml").build();
        assertThatThrownBy(() -> JaxrsTestHelper.assertJsonResponse(response))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertXmlResponseType() {
        var response = Response.ok(new Person("Bob", "Sackamano"), "application/xml").build();
        assertThatCode(() -> JaxrsTestHelper.assertXmlResponse(response))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertXmlResponseType_AndThrow_WhenNotXml() {
        var response = Response.ok(new Person("Bob", "Sackamano"), "application/json").build();
        assertThatThrownBy(() -> JaxrsTestHelper.assertXmlResponse(response))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertPlainTextResponseType() {
        var response = Response.ok(new Person("Bob", "Sackamano"), "text/plain").build();
        assertThatCode(() -> JaxrsTestHelper.assertPlainTextResponse(response))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertPlainTextResponseType_AndThrow_WhenIsNotPlainText() {
        var response = Response.ok(new Person("Bob", "Sackamano"), "text/html").build();
        assertThatThrownBy(() -> JaxrsTestHelper.assertPlainTextResponse(response))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertResponseType() {
        var response = Response.ok(new Person("Bob", "Sackamano"), "application/json").build();
        assertThatCode(() -> JaxrsTestHelper.assertResponseType(response, "application/json"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertResponseType_AndThrow_WhenNotEqual() {
        var response = Response.ok(new Person("Bob", "Sackamano"),
                "application/json").build();
        assertThatThrownBy(() -> JaxrsTestHelper.assertResponseType(response, "text/html"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertBadRequest() {
        var response = Response.status(Response.Status.BAD_REQUEST).build();
        assertThatCode(() -> JaxrsTestHelper.assertBadRequest(response))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertBadRequest_AndThrow_WhenNotBadRequest() {
        var response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        assertThatThrownBy(() -> JaxrsTestHelper.assertBadRequest(response))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertConflict() {
        var response = Response.status(Response.Status.CONFLICT).build();
        assertThatCode(() -> JaxrsTestHelper.assertConflict(response))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertConflict_AndThrow_WhenNotConflict() {
        var response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        assertThatThrownBy(() -> JaxrsTestHelper.assertConflict(response))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertUnprocessableEntity() {
        var response = Response.status(JaxrsTestHelper.UNPROCESSABLE_ENTITY_STATUS).build();
        assertThatCode(() -> JaxrsTestHelper.assertUnprocessableEntity(response))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertUnprocessableEntity_AndThrow_WhenNotUnprocessableEntity() {
        var response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        assertThatThrownBy(() ->
                JaxrsTestHelper.assertUnprocessableEntity(response))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertCreated() {
        var response = Response.status(Response.Status.CREATED).build();
        assertThatCode(() -> JaxrsTestHelper.assertCreatedResponse(response))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertCreated_AndThrow_WhenNotCreated() {
        var response = Response.ok().build();
        assertThatThrownBy(() -> JaxrsTestHelper.assertCreatedResponse(response))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertCreatedWithLocation() {
        var response = Response.created(URI.create("http://acme.com/widgets/42")).build();
        assertThatCode(() -> JaxrsTestHelper.assertCreatedResponseWithLocation(response, "http://acme.com/widgets", "42"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertCreatedWithLocation_AndThrow_WhenNotEqual() {
        var response = Response.created(URI.create("http://acme.com/widgets/42")).build();
        assertThatThrownBy(() ->
                JaxrsTestHelper.assertCreatedResponseWithLocation(response, "http://acme.com/gadgets", "94"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertCreatedResponseWithLocationEndingWith() {
        var response = Response.created(URI.create("http://acme.com/widgets/42")).build();
        assertThatCode(() -> JaxrsTestHelper.assertCreatedResponseWithLocationEndingWith(response, "/widgets/42"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertCreatedResponseWithLocationEndingWith_AndThrow_WhenNotEqual() {
        var response = Response.created(URI.create("http://acme.com/widgets/42")).build();
        assertThatThrownBy(() ->
                JaxrsTestHelper.assertCreatedResponseWithLocationEndingWith(response, "/widgets/2"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertOkResponse() {
        var response = Response.ok().build();
        assertThatCode(() -> JaxrsTestHelper.assertOkResponse(response))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertOkResponse_AndThrow_WhenNotOk() {
        var response = Response.noContent().build();
        assertThatThrownBy(() -> JaxrsTestHelper.assertOkResponse(response))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertAcceptedResponse() {
        var response = Response.accepted().build();
        assertThatCode(() -> JaxrsTestHelper.assertAcceptedResponse(response))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertAcceptedResponse_AndThrow_WhenNotOk() {
        var response = Response.ok().build();
        assertThatThrownBy(() ->
                JaxrsTestHelper.assertAcceptedResponse(response))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertNoContentResponse() {
        var response = Response.noContent().build();
        assertThatCode(() -> JaxrsTestHelper.assertNoContentResponse(response))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertNoContentResponse_AndThrow_WhenNotNoContent() {
        var response = Response.ok().build();
        assertThatThrownBy(() ->
                JaxrsTestHelper.assertNoContentResponse(response))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertUnauthorizedResponse() {
        var response = Response.status(Response.Status.UNAUTHORIZED).build();
        assertThatCode(() -> JaxrsTestHelper.assertUnauthorizedResponse(response))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertUnauthorizedResponse_AndThrow_WhenNotUnauthorized() {
        var response = Response.status(Response.Status.CONFLICT).build();
        assertThatThrownBy(() ->
                JaxrsTestHelper.assertUnauthorizedResponse(response))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertForbiddenResponse() {
        var response = Response.status(Response.Status.FORBIDDEN).build();
        assertThatCode(() -> JaxrsTestHelper.assertForbiddenResponse(response))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertForbiddenResponse_AndThrow_WhenNotForbidden() {
        var response = Response.status(Response.Status.PRECONDITION_FAILED).build();
        assertThatThrownBy(() ->
                JaxrsTestHelper.assertForbiddenResponse(response))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertNotFoundResponse() {
        var response = Response.status(Response.Status.NOT_FOUND).build();
        assertThatCode(() -> JaxrsTestHelper.assertNotFoundResponse(response))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertNotFoundResponse_AndThrow_WhenIsNotANotFound() {
        var response = Response.status(Response.Status.FOUND).build();
        assertThatThrownBy(() ->
                JaxrsTestHelper.assertNotFoundResponse(response))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertInternalServerErrorResponse() {
        var response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        assertThatCode(() -> JaxrsTestHelper.assertInternalServerErrorResponse(response))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertInternalServerErrorResponse_AndThrow_WhenNotInternalServerError() {
        var response = Response.status(Response.Status.BAD_GATEWAY).build();
        assertThatThrownBy(() ->
                JaxrsTestHelper.assertInternalServerErrorResponse(response))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertCustomHeaderFirstValue() {
        var response = Response.ok()
                .header("myCustomHeader", "firstValue")
                .header("myCustomHeader", "secondValue")
                .build();

        assertThatCode(() ->
                JaxrsTestHelper.assertCustomHeaderFirstValue(response, "myCustomHeader", "firstValue"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertCustomHeaderFirstValue_AndThrow_WhenNoMatch() {
        var response = Response.ok()
                .header("myCustomHeader", "firstValue")
                .header("myCustomHeader", "secondValue")
                .build();

        assertThatThrownBy(() ->
                JaxrsTestHelper.assertCustomHeaderFirstValue(response, "myCustomHeader", "notTheRightValue"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertNonNullResponseEntity() {
        var response = Response.ok(new Person("Alice", "Smith")).build();
        assertThatCode(() -> JaxrsTestHelper.assertNonNullResponseEntity(response, Person.class))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertNonNullResponseEntity_AndThrow_WhenNull() {
        var response = Response.ok().build();
        assertThatThrownBy(() ->
                JaxrsTestHelper.assertNonNullResponseEntity(response, Person.class))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertResponseEntity_IsExactSameInstance() {
        var entity = new Person("Alice", "Smith");
        var response = Response.ok(entity).build();
        assertThatCode(() -> JaxrsTestHelper.assertResponseEntity(response, entity))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAssertResponseEntity_AndThrow_WhenNotExactSameInstance() {
        var entity = new Person("Alice", "Smith");
        var response = Response.ok(entity).build();
        var expectedEntity = new Person("Alice", "Smith");
        assertThatThrownBy(() ->
                JaxrsTestHelper.assertResponseEntity(response, expectedEntity))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertResponseEntity_IsExactSameInstance_AndClass() {
        var entity = new Person("Alice", "Smith");
        var response = Response.ok(entity).build();
        var assertedEntity = JaxrsTestHelper.assertResponseEntity(response, Person.class, entity);
        assertThat(assertedEntity).isSameAs(entity);
    }

    @Test
    void shouldAssertResponseEntity_AndThrow_WhenNotExactSameInstance_AndClass() {
        var entity = new Person("Alice", "Smith");
        var response = Response.ok(entity).build();
        var expectedEntity = new Person("Alice", "Smith");
        assertThatThrownBy(() ->
                JaxrsTestHelper.assertResponseEntity(response, Person.class, expectedEntity))
                .isInstanceOf(AssertionError.class);
    }

    @Nested
    class AssertMapResponseEntity {

        @Test
        void shouldReturnMap_WhenEntityIsAMap() {
            var entity = Map.of(
                    "the answer", 42,
                    "foo", "bar"
            );
            var response = Response.serverError().entity(entity).build();

            var mapEntity = JaxrsTestHelper.assertMapResponseEntity(response);
            assertThat(mapEntity).isEqualTo(entity);
        }

        @Test
        void shouldThrow_WhenResponseHasNoEntity() {
            var response = Response.serverError().build();
            assertThatThrownBy(() -> JaxrsTestHelper.assertMapResponseEntity(response))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        void shouldThrow_WhenResponseEntityIsNotAMap() {
            var response = Response.serverError().entity(new Object()).build();
            assertThatThrownBy(() -> JaxrsTestHelper.assertMapResponseEntity(response))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        void shouldThrow_WhenResponseThrows() {
            var response = mock(Response.class);
            when(response.getEntity()).thenThrow(new IllegalStateException("response already consumed"));

            assertThatThrownBy(() -> JaxrsTestHelper.assertMapResponseEntity(response))
                    .isInstanceOf(AssertionError.class);
        }
    }

    @Value
    private static class Person {
        String firstName;
        String lastName;
    }

    @Nested
    class AssertXxxResponseFamily {

        @ParameterizedTest
        @MethodSource("org.kiwiproject.test.jaxrs.JaxrsTestHelperTest#generateAllResponses")
        void shouldAssertSuccessfulResponseFamily(Response response) {
            ThrowableAssert.ThrowingCallable assertionMethod = () ->
                    JaxrsTestHelper.assertSuccessfulResponseFamily(response);

            var assertionShouldPass = KiwiResponses.successful(response);
            assertResponseFamilyMethod(response, assertionMethod,
                    Response.Status.Family.SUCCESSFUL, assertionShouldPass);
        }

        @ParameterizedTest
        @MethodSource("org.kiwiproject.test.jaxrs.JaxrsTestHelperTest#generateAllResponses")
        void shouldAssertClientErrorResponseFamily(Response response) {
            ThrowableAssert.ThrowingCallable assertionMethod = () ->
                    JaxrsTestHelper.assertClientErrorResponseFamily(response);

            var assertionShouldPass = KiwiResponses.clientError(response);
            assertResponseFamilyMethod(response, assertionMethod,
                    Response.Status.Family.CLIENT_ERROR, assertionShouldPass);
        }

        @ParameterizedTest
        @MethodSource("org.kiwiproject.test.jaxrs.JaxrsTestHelperTest#generateAllResponses")
        void shouldAssertServerErrorResponseFamily(Response response) {
            ThrowableAssert.ThrowingCallable assertionMethod = () ->
                    JaxrsTestHelper.assertServerErrorResponseFamily(response);

            var assertionShouldPass = KiwiResponses.serverError(response);
            assertResponseFamilyMethod(response, assertionMethod,
                    Response.Status.Family.SERVER_ERROR, assertionShouldPass);
        }

        private void assertResponseFamilyMethod(Response response,
                                                ThrowableAssert.ThrowingCallable assertionMethod,
                                                Response.Status.Family
                                                        expectedFamily, boolean assertionShouldPass) {
            if (assertionShouldPass) {
                assertThatCode(assertionMethod)
                        .describedAs("Response with status %d should not have failed", response.getStatus())
                        .doesNotThrowAnyException();
            } else {
                var expectedMessage = f("Expected {} response to have family {}, but found family: {}",
                        response.getStatus(), expectedFamily,
                        response.getStatusInfo().getFamily());
                assertThatThrownBy(assertionMethod)
                        .describedAs("Response with status %d was expected to fail, but did not", response.getStatus())
                        .isInstanceOf(AssertionFailedError.class)
                        .hasMessageContaining(expectedMessage);
            }
        }
    }

    static Stream<Response> generateAllResponses() {
        return Arrays.stream(Response.Status.values())
                .map(status -> Response.status(status).build());
    }
}
