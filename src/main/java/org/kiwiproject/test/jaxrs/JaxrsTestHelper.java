package org.kiwiproject.test.jaxrs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kiwiproject.base.KiwiStrings.f;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import lombok.experimental.UtilityClass;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

/**
 * Helper class for testing JAX-RS code.
 */
@UtilityClass
public class JaxrsTestHelper {

    /**
     * Status code for (non-standard) 422 Unprocessable Entity responses.
     * <p>
     * Because it is defined as a WebDAV extension, the JAX-RS {@link Response.Status} does not
     * include it as an enum constant, which is...unfortunate.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/422">422 Unprocessable Entity</a>
     */
    public static final int UNPROCESSABLE_ENTITY_STATUS = 422;

    /**
     * Creates a Mockito mock {Kink javax.ws.rs.core.UriInfo} object with the specified base path, and which returns
     * a {@link UriBuilder} when the {@link UriInfo#getRequestUriBuilder()} method is called.
     * <p>
     * You are obviously free to record additional expectations as well.
     *
     * @param basePath the base path
     * @return a <i>mock</i> {@link UriInfo}
     */
    public static UriInfo createMockUriInfoWithRequestUriBuilder(String basePath) {
        var uriInfo = mock(UriInfo.class);
        var builder = UriBuilder.fromPath(basePath);
        when(uriInfo.getRequestUriBuilder()).thenReturn(builder);
        return uriInfo;
    }

    /**
     * Asserts the actual response type of {@code response} is {@code application/json}.
     *
     * @param response the response to check
     * @throws java.lang.AssertionError if the response type is not the expected value
     */
    public static void assertJsonResponse(Response response) {
        assertResponseType(response, MediaType.APPLICATION_JSON);
    }

    /**
     * Asserts the actual response type of {@code response} is {@code application/xml}.
     *
     * @param response the response to check
     * @throws AssertionError if the response type is not the expected value
     */
    public static void assertXmlResponse(Response response) {
        assertResponseType(response, MediaType.APPLICATION_XML);
    }

    /**
     * Asserts the actual response type of {@code response} is {@code text/plain}.
     *
     * @param response the response to check
     * @throws AssertionError if the response type is not the expected value
     */
    public static void assertPlainTextResponse(Response response) {
        assertResponseType(response, MediaType.TEXT_PLAIN);
    }

    /**
     * Asserts the actual response type of {@code response} is {@code expectedContentType}.
     *
     * @param response            the response to check
     * @param expectedContentType expected content type
     * @throws AssertionError if the response type is not the expected value
     */
    public static void assertResponseType(Response response, String expectedContentType) {
        assertThat(response.getMediaType()).hasToString(expectedContentType);
    }

    /**
     * Asserts the actual response expectedStatus code is {@code expectedStatus}.
     *
     * @param response       the response to check
     * @param expectedStatus expected response status
     * @throws AssertionError if the response expectedStatus code is not the expected value
     */
    public static void assertResponseStatusCode(Response response, Response.Status expectedStatus) {
        assertResponseStatusCode(response, expectedStatus.getStatusCode());
    }

    /**
     * Asserts the actual response status code is {@code expectedStatusCode}.
     *
     * @param response           the response to check
     * @param expectedStatusCode the expected status code
     * @throws AssertionError if the response status code is not the expected value
     */
    public static void assertResponseStatusCode(Response response, int expectedStatusCode) {
        assertThat(response.getStatus()).isEqualTo(expectedStatusCode);
    }

    /**
     * Asserts the specified {@code response} has a 400 Bad Request status.
     *
     * @param response the response to check
     * @throws AssertionError if the response status is not 400 Bad Request
     */
    public static void assertBadRequest(Response response) {
        assertResponseStatusCode(response, Response.Status.BAD_REQUEST);
    }

    /**
     * Asserts the specified {@code response} has a 409 Conflict status.
     *
     * @param response the response to check
     * @throws AssertionError if the response status is not 409 Conflict
     */
    public static void assertConflict(Response response) {
        assertResponseStatusCode(response, Response.Status.CONFLICT);
    }

    /**
     * Asserts the specified {@code response} has a 422 Unprocessable Entity status.
     *
     * @param response the response to check
     * @throws AssertionError if the response is not a 422 Unprocessable Entity
     */
    public static void assertUnprocessableEntity(Response response) {
        assertResponseStatusCode(response, UNPROCESSABLE_ENTITY_STATUS);
    }

    /**
     * Asserts that {@code response} is a 201 Created response.
     *
     * @param response the response to check
     * @throws AssertionError if the response is not a 201 Created
     */
    public static void assertCreatedResponse(Response response) {
        assertResponseStatusCode(response, Response.Status.CREATED);
    }

    /**
     * Asserts that {@code response} is a 201 Created response with the {@code Location} header set to
     * {@code expectedLocation}.
     *
     * @param response         the response to check
     * @param expectedLocation the expected value of the {@code Location} header
     * @throws AssertionError if the response isn't a 201 or if the location header is incorrect
     */
    public static void assertCreatedResponseWithLocation(Response response, String expectedLocation) {
        assertResponseStatusCode(response, Response.Status.CREATED);
        assertThat(response.getMetadata().getFirst("Location")).hasToString(expectedLocation);
    }

    /**
     * Asserts that {@code response} is a 201 Created response with the {@code Location} header having a location path
     * ending with {@code locationEndingWith}.
     * <p>
     * Use this method when you only have a <em>relative</em> location path (e.g. "/object/42") instead of
     * {@link #assertCreatedResponseWithLocation(Response, String)}, which requires a location to match exactly: i.e.
     * "http://localhost:4949/object/42"
     *
     * @param response           the response to check
     * @param locationEndingWith the expected substring at the end of the {@code Location} header
     * @throws AssertionError if the response isn't a 201 or if the location header is incorrect
     */
    public static void assertCreatedResponseWithLocationEndingWith(Response response, String locationEndingWith) {
        assertResponseStatusCode(response, Response.Status.CREATED);
        assertThat(response.getMetadata().getFirst("Location").toString()).endsWith(locationEndingWith);
    }

    /**
     * Asserts that {@code response} is a 201 Created response with the {@code Location} header set to
     * the value of {@code basePath} concatenated with {@code id}.
     *
     * @param response the response to check
     * @param basePath the base path, e.g. "/users" (do not add a trailing slash)
     * @param id       the identifier, e.g. "42" (do not add a leading slash)
     * @throws AssertionError if the response isn't a 201 or if the location header is incorrect
     */
    public static void assertCreatedResponseWithLocation(Response response,
                                                         String basePath, Object id) {
        var expectedLocation = f("%s/%s", basePath, id);
        assertCreatedResponseWithLocation(response, expectedLocation);
    }

    /**
     * Asserts that {@code response} is a 200 OK response.
     *
     * @param response the response to check
     * @throws AssertionError if the response isn't a 200
     */
    public static void assertOkResponse(Response response) {
        assertResponseStatusCode(response, Response.Status.OK);
    }

    /**
     * Asserts that {@code response} is a 202 Accepted response.
     *
     * @param response the response to check
     * @throws AssertionError if the response isn't a 202
     */
    public static void assertAcceptedResponse(Response response) {
        assertResponseStatusCode(response, Response.Status.ACCEPTED);
    }

    /**
     * Asserts that {@code response} is a 204 No Content response.
     *
     * @param response the response to check
     * @throws AssertionError if the response isn't a 204
     */
    public static void assertNoContentResponse(Response response) {
        assertResponseStatusCode(response, Response.Status.NO_CONTENT);
    }

    /**
     * Asserts that {@code response} is a 401 Unauthorized response.
     *
     * @param response the response to check
     * @throws AssertionError if the response isn't a 401
     */
    public static void assertUnauthorizedResponse(Response response) {
        assertResponseStatusCode(response, Response.Status.UNAUTHORIZED);
    }

    /**
     * Asserts that {@code response} is a 403 Forbidden response.
     *
     * @param response the response to check
     * @throws AssertionError if the response isn't a 403
     */
    public static void assertForbiddenResponse(Response response) {
        assertResponseStatusCode(response, Response.Status.FORBIDDEN);
    }

    /**
     * Asserts that {@code response} is a 404 Not Found response.
     *
     * @param response the response to check
     * @throws AssertionError if the response isn't a 404
     */
    public static void assertNotFoundResponse(Response response) {
        assertResponseStatusCode(response, Response.Status.NOT_FOUND);
    }

    /**
     * Asserts that {@code response} is a 500 Internal Server Error response.
     *
     * @param response the response to check
     * @throws AssertionError if the response isn't a 500
     */
    public static void assertInternalServerErrorResponse(Response response) {
        assertResponseStatusCode(response, Response.Status.INTERNAL_SERVER_ERROR);
    }

    /**
     * Asserts that {@code response} has a 2xx status code.
     *
     * @param response the response to check
     * @throws AssertionError if the response isn't a 2xx
     */
    public static void assertSuccessfulResponseFamily(Response response) {
        assertResponseFamily(response, Response.Status.Family.SUCCESSFUL);
    }

    /**
     * Asserts that {@code response} has a 4xx status code.
     *
     * @param response the response to check
     * @throws AssertionError if the response isn't a 4xx
     */
    public static void assertClientErrorResponseFamily(Response response) {
        assertResponseFamily(response, Response.Status.Family.CLIENT_ERROR);
    }

    /**
     * Asserts that {@code response} has a 5xx status code.
     *
     * @param response the response to check
     * @throws AssertionError if the response isn't a 5xx
     */
    public static void assertServerErrorResponseFamily(Response response) {
        assertResponseFamily(response, Response.Status.Family.SERVER_ERROR);
    }

    /**
     * Asserts that {@code response} is in the expected {@link Response.Status.Family}.
     *
     * @param response       the response to check
     * @param expectedFamily the expected {@link Response.Status.Family} of the given response
     * @throws AssertionError if the response isn't in the expected family
     */
    public static void assertResponseFamily(Response response, Response.Status.Family expectedFamily) {
        var actualFamily = response.getStatusInfo().getFamily();
        assertThat(actualFamily)
                .describedAs("Expected %d response to have family %s, but found family: %s",
                        response.getStatus(), expectedFamily, actualFamily)
                .isEqualTo(expectedFamily);
    }

    /**
     * Asserts that the response has a custom header named {@code headerName}
     * and whose <i>first</i> value is {@code expectedValue}.
     *
     * @param response      the response to check
     * @param headerName    the name of the header to check
     * @param expectedValue the expected value of the header
     * @throws AssertionError if the response doesn't have a header named {@code headerName} or if the value
     *                        doesn't match
     */
    public static void assertCustomHeaderFirstValue(Response response, String headerName, Object expectedValue) {
        assertThat(response.getHeaders().getFirst(headerName)).isEqualTo(expectedValue);
    }

    /**
     * Asserts that the response contains an entity using {@link Response#getEntity()} whose class is
     * {@code expectedClass}.
     *
     * @param response      the response to check
     * @param expectedClass the expected entity type
     * @param <T>           the entity type
     * @return the response entity, cast as a {@code T}
     * @throws AssertionError if the response entity doesn't exist or isn't the expected type
     * @see Response#getEntity()
     */
    public static <T> T assertNonNullResponseEntity(Response response, Class<T> expectedClass) {
        var entity = response.getEntity();
        assertThat(entity).isNotNull();
        assertThat(entity).isInstanceOf(expectedClass);
        return expectedClass.cast(entity);
    }

    /**
     * Asserts that the response contains an entity using {@link Response#getEntity()} that is
     * <i>the same instance as</i> the {@code expectedEntity}.
     *
     * @param response       the response to check
     * @param expectedEntity the expected response entity
     * @param <T>            the entity type
     * @throws AssertionError if the if the response entity doesn't exist or isn't the expected object
     * @see Response#getEntity()
     */
    public static <T> void assertResponseEntity(Response response, T expectedEntity) {
        var entity = assertNonNullResponseEntity(response, expectedEntity.getClass());
        assertThat(entity).isSameAs(expectedEntity);
    }

    /**
     * Asserts that the response contains an entity using {@link Response#getEntity()} whose class is
     * {@code expectedClass} and which is <i>the same instance as</i> the {@code expectedEntity}.
     *
     * @param response       the response to check
     * @param expectedClass  the expected entity type
     * @param expectedEntity the expected entity
     * @param <T>            the response entity type
     * @return the same entity assuming the assertion passed
     * @throws AssertionError if the response entity doesn't exist or isn't the expected class/object
     * @see Response#getEntity()
     */
    public static <T> T assertResponseEntity(Response response, Class<T> expectedClass, T expectedEntity) {
        T entity = assertNonNullResponseEntity(response, expectedClass);
        assertThat(entity).isSameAs(expectedEntity);
        return entity;
    }

    /**
     * Asserts that the response contains an entity using {@link Response#getEntity()} that is <em>equal to</em>
     * the {@code expectedEntity} using logical equality, e.g. using {@link Object#equals(Object)}.
     *
     * @param response       the response to check
     * @param expectedEntity the expected entity
     * @param <T>            the entity type
     * @throws AssertionError if the response entity doesn't exist or isn't equal to the expected object
     * @see Response#getEntity()
     */
    public static <T> void assertEqualResponseEntity(Response response, T expectedEntity) {
        var entity = assertNonNullResponseEntity(response, expectedEntity.getClass());
        assertThat(entity).isEqualTo(expectedEntity);
    }

    /**
     * Asserts that the response contains an entity using {@link Response#getEntity()} that is a {@link Map} and
     * specifically one whose keys are Strings and values are Objects.
     *
     * @param response the response to check
     * @return the response entity cast to a map
     * @throws AssertionError if the response doesn't have an entity, or is not a map
     * @implNote This method performs an unchecked cast of the map entity to {@code Map<String, Object>} after verifying
     * the entity is a {@link Map} instance. Unexpected results and/or exceptions may occur if the map does not
     * contain only String keys.
     */
    public static Map<String, Object> assertMapResponseEntity(Response response) {
        Object entityObj;
        try {
            entityObj = response.getEntity();
        } catch (Exception e) {
            throw new AssertionError("Error getting response entity", e);
        }

        assertThat(entityObj)
                .describedAs("entity is null")
                .isNotNull();

        assertThat(entityObj)
                .describedAs("entity is not a Map")
                .isInstanceOf(Map.class);

        //noinspection unchecked
        return (Map<String, Object>) entityObj;
    }
}
