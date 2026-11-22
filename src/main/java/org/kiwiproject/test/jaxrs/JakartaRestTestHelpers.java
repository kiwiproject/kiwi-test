package org.kiwiproject.test.jaxrs;

import static org.kiwiproject.base.KiwiStrings.f;

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import lombok.experimental.UtilityClass;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Test utilities for Jakarta RESTful Web Services.
 */
@UtilityClass
public class JakartaRestTestHelpers {

    /**
     * Converts an <em>outbound</em> {@link Response} built as if from inside a Jakarta REST resource, e.g.,
     * something like {@code Response.ok(entity).build()}, into an <em>inbound</em>-style Response on which
     * {@link Response#readEntity(Class)} and {@link Response#readEntity(GenericType)} can be called. Inbound
     * responses are what Jakarta REST clients return, and the {@code readEntity} methods are how client code
     * gets the response entity.
     * <p>
     * This is useful when testing client code, and you want to mock the response returned by an endpoint
     * to test how the client responds to various responses, e.g., different error conditions. If you don't
     * do this and your test attempts to read an entity from an outbound response, an
     * {@link IllegalStateException} is thrown.
     * <p>
     * The returned response delegates everything to the outbound response, except that
     * {@link Response#readEntity(Class)} and {@link Response#readEntity(GenericType)} return the
     * <em>same</em> object returned by {@link Response#getEntity()} (no serialization or deserialization
     * occurs). If the requested type is not compatible with the entity, an {@link IllegalStateException}
     * is thrown. Once the response is closed, the {@code readEntity} methods throw
     * {@link IllegalStateException}, as they do for a real inbound response. The two {@code readEntity}
     * methods that accept an array of {@link Annotation} throw {@link UnsupportedOperationException}.
     * <p>
     * Unlike an approach based on spying on the outbound response, no mocking framework is involved. This means
     * it is safe to call inline as an argument, e.g. {@code when(...).thenReturn(toInboundResponse(...))},
     * without triggering Mockito's {@code UnfinishedStubbingException} caused by nested stubbing.
     *
     * @param outboundResponse the outbound response
     * @return a simulated inbound response
     */
    public static Response toInboundResponse(Response outboundResponse) {
        return new InboundResponse(outboundResponse);
    }

    private static class InboundResponse extends Response {

        private final Response delegate;
        private boolean closed;

        InboundResponse(Response delegate) {
            this.delegate = delegate;
        }

        @Override
        public <T> T readEntity(Class<T> entityType) {
            return readEntityOfType(entityType);
        }

        @Override
        public <T> T readEntity(GenericType<T> entityType) {
            return readEntityOfType(entityType.getRawType());
        }

        @Override
        public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
            throw new UnsupportedOperationException("readEntity with annotations is not supported");
        }

        @Override
        public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
            throw new UnsupportedOperationException("readEntity with annotations is not supported");
        }

        @SuppressWarnings("unchecked")
        private <T> T readEntityOfType(Class<?> rawType) {
            if (closed) {
                throw new IllegalStateException("Response is closed");
            }

            var entity = delegate.getEntity();
            if (entity != null && !isAssignable(rawType, entity)) {
                throw new IllegalStateException(
                        f("Entity of type {} cannot be read as {}", entity.getClass().getName(), rawType.getName()));
            }
            return (T) entity;
        }

        private static boolean isAssignable(Class<?> rawType, Object entity) {
            return rawType.isPrimitive() || rawType.isInstance(entity);
        }

        @Override
        public void close() {
            closed = true;
            delegate.close();
        }

        @Override
        public int getStatus() {
            return delegate.getStatus();
        }

        @Override
        public StatusType getStatusInfo() {
            return delegate.getStatusInfo();
        }

        @Override
        public Object getEntity() {
            return delegate.getEntity();
        }

        @Override
        public boolean hasEntity() {
            return delegate.hasEntity();
        }

        @Override
        public boolean bufferEntity() {
            return delegate.bufferEntity();
        }

        @Override
        public MediaType getMediaType() {
            return delegate.getMediaType();
        }

        @Override
        public Locale getLanguage() {
            return delegate.getLanguage();
        }

        @Override
        public int getLength() {
            return delegate.getLength();
        }

        @Override
        public Set<String> getAllowedMethods() {
            return delegate.getAllowedMethods();
        }

        @Override
        public Map<String, NewCookie> getCookies() {
            return delegate.getCookies();
        }

        @Override
        public EntityTag getEntityTag() {
            return delegate.getEntityTag();
        }

        @Override
        public Date getDate() {
            return delegate.getDate();
        }

        @Override
        public Date getLastModified() {
            return delegate.getLastModified();
        }

        @Override
        public URI getLocation() {
            return delegate.getLocation();
        }

        @Override
        public Set<Link> getLinks() {
            return delegate.getLinks();
        }

        @Override
        public boolean hasLink(String relation) {
            return delegate.hasLink(relation);
        }

        @Override
        public Link getLink(String relation) {
            return delegate.getLink(relation);
        }

        @Override
        public Link.Builder getLinkBuilder(String relation) {
            return delegate.getLinkBuilder(relation);
        }

        @Override
        public MultivaluedMap<String, Object> getMetadata() {
            return delegate.getMetadata();
        }

        @Override
        public MultivaluedMap<String, String> getStringHeaders() {
            return delegate.getStringHeaders();
        }

        @Override
        public String getHeaderString(String name) {
            return delegate.getHeaderString(name);
        }
    }
}
