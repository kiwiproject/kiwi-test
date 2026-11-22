package org.kiwiproject.test.jaxrs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@DisplayName("JakartaRestTestHelpers")
class JakartaRestTestHelpersTest {

    private static final GenericType<List<Order>> ORDER_LIST_GENERIC_TYPE = new GenericType<>() {
    };

    record Order(String id, String customerId) {
    }

    @Nested
    class ToInboundResponse {

        @Test
        void shouldConvertOutboundToInboundResponse_ForClass() {
            var order = new Order("12345", "7890");
            var outboundResponse = Response.ok(order).build();

            assertThatThrownBy(() -> outboundResponse.readEntity(Order.class))
                    .describedAs("precondition: outbound response readEntity(Class) fails")
                    .isExactlyInstanceOf(IllegalStateException.class);

            var inboundResponse = JakartaRestTestHelpers.toInboundResponse(outboundResponse);

            assertThat(inboundResponse.readEntity(Order.class)).isSameAs(order);
        }

        @Test
        void shouldConvertOutboundToInboundResponse_ForGenericType() {
            var customerOrders = List.of(
                    new Order("12345", "1"),
                    new Order("23456", "1"),
                    new Order("78901", "1")
            );
            var outboundResponse = Response.ok(customerOrders).build();

            var orderListType = ORDER_LIST_GENERIC_TYPE;
            assertThatThrownBy(() -> outboundResponse.readEntity(orderListType))
                    .describedAs("precondition: outbound response readEntity(GenericType) fails")
                    .isExactlyInstanceOf(IllegalStateException.class);

            var inboundResponse = JakartaRestTestHelpers.toInboundResponse(outboundResponse);

            assertThat(inboundResponse.readEntity(orderListType)).isSameAs(customerOrders);
        }

        @Test
        void shouldBeUsableInlineWithinMockitoStubbing() {
            var order = new Order("12345", "7890");
            var invocation = mock(Invocation.class);

            // nested stubbing: this would throw Mockito's UnfinishedStubbingException
            // if toInboundResponse used Mockito
            when(invocation.invoke())
                    .thenReturn(JakartaRestTestHelpers.toInboundResponse(Response.ok(order).build()));

            assertThat(invocation.invoke().readEntity(Order.class)).isSameAs(order);
        }

        @Test
        void shouldReturnNull_WhenThereIsNoEntity() {
            var inboundResponse = JakartaRestTestHelpers.toInboundResponse(Response.noContent().build());

            assertThat(inboundResponse.hasEntity()).isFalse();
            assertThat(inboundResponse.readEntity(Order.class)).isNull();
        }

        @Test
        void shouldThrowIllegalState_WhenEntityIsNotOfRequestedType() {
            var inboundResponse = JakartaRestTestHelpers.toInboundResponse(Response.ok("a string").build());

            assertThatThrownBy(() -> inboundResponse.readEntity(Order.class))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("java.lang.String")
                    .hasMessageContaining(Order.class.getName());

            assertThatThrownBy(() -> inboundResponse.readEntity(ORDER_LIST_GENERIC_TYPE))
                    .isExactlyInstanceOf(IllegalStateException.class);
        }

        @Test
        void shouldAllowReadingEntityAsPrimitiveType() {
            var inboundResponse = JakartaRestTestHelpers.toInboundResponse(Response.ok(42).build());

            assertThat(inboundResponse.readEntity(int.class)).isEqualTo(42);
        }

        @Test
        void shouldThrowIllegalState_WhenReadingEntityAfterClose() {
            var order = new Order("12345", "7890");
            var inboundResponse = JakartaRestTestHelpers.toInboundResponse(Response.ok(order).build());

            inboundResponse.close();

            assertThatThrownBy(() -> inboundResponse.readEntity(Order.class))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("Response is closed");
        }

        @Test
        void shouldNotSupportReadEntityWithAnnotations() {
            var inboundResponse = JakartaRestTestHelpers.toInboundResponse(Response.ok(new Order("1", "2")).build());
            var annotations = new Annotation[0];

            assertThatThrownBy(() -> inboundResponse.readEntity(Order.class, annotations))
                    .isExactlyInstanceOf(UnsupportedOperationException.class);

            assertThatThrownBy(() -> inboundResponse.readEntity(ORDER_LIST_GENERIC_TYPE, annotations))
                    .isExactlyInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void shouldDelegateOtherMethodsToOutboundResponse() {
            var location = URI.create("https://example.org/orders/12345");
            var outboundResponse = Response.created(location)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .header("X-Custom", "custom-value")
                    .entity(new Order("12345", "7890"))
                    .build();

            var inboundResponse = JakartaRestTestHelpers.toInboundResponse(outboundResponse);

            assertThat(inboundResponse.getStatus()).isEqualTo(201);
            assertThat(inboundResponse.getStatusInfo()).isEqualTo(Response.Status.CREATED);
            assertThat(inboundResponse.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
            assertThat(inboundResponse.getLocation()).isEqualTo(location);
            assertThat(inboundResponse.getHeaderString("X-Custom")).isEqualTo("custom-value");
            assertThat(inboundResponse.getStringHeaders().getFirst("X-Custom")).isEqualTo("custom-value");
            assertThat(inboundResponse.getMetadata()).containsKey("X-Custom");
            assertThat(inboundResponse.getEntity()).isSameAs(outboundResponse.getEntity());
            assertThat(inboundResponse.hasEntity()).isTrue();
        }

        @Test
        void shouldDelegateAllRemainingMethodsToOutboundResponse() {
            var delegate = mock(Response.class);
            var entityTag = new EntityTag("abc");
            var date = new Date(1_000L);
            var lastModified = new Date(2_000L);
            var cookies = Map.of("session", new NewCookie.Builder("session").value("123").build());
            var link = Link.fromUri("https://example.org/orders").rel("orders").build();
            var metadata = new MultivaluedHashMap<String, Object>();
            var stringHeaders = new MultivaluedHashMap<String, String>();

            when(delegate.getLanguage()).thenReturn(Locale.CANADA);
            when(delegate.getLength()).thenReturn(42);
            when(delegate.getAllowedMethods()).thenReturn(Set.of("GET", "POST"));
            when(delegate.getCookies()).thenReturn(cookies);
            when(delegate.getEntityTag()).thenReturn(entityTag);
            when(delegate.getDate()).thenReturn(date);
            when(delegate.getLastModified()).thenReturn(lastModified);
            when(delegate.getLinks()).thenReturn(Set.of(link));
            when(delegate.hasLink("orders")).thenReturn(true);
            when(delegate.getLink("orders")).thenReturn(link);
            when(delegate.getLinkBuilder("orders")).thenReturn(Link.fromUri("https://example.org/orders"));
            when(delegate.getMetadata()).thenReturn(metadata);
            when(delegate.getStringHeaders()).thenReturn(stringHeaders);
            when(delegate.bufferEntity()).thenReturn(true);

            var inboundResponse = JakartaRestTestHelpers.toInboundResponse(delegate);

            assertThat(inboundResponse.getLanguage()).isEqualTo(Locale.CANADA);
            assertThat(inboundResponse.getLength()).isEqualTo(42);
            assertThat(inboundResponse.getAllowedMethods()).containsExactlyInAnyOrder("GET", "POST");
            assertThat(inboundResponse.getCookies()).isSameAs(cookies);
            assertThat(inboundResponse.getEntityTag()).isSameAs(entityTag);
            assertThat(inboundResponse.getDate()).isSameAs(date);
            assertThat(inboundResponse.getLastModified()).isSameAs(lastModified);
            assertThat(inboundResponse.getLinks()).containsExactly(link);
            assertThat(inboundResponse.hasLink("orders")).isTrue();
            assertThat(inboundResponse.getLink("orders")).isSameAs(link);
            assertThat(inboundResponse.getLinkBuilder("orders")).isNotNull();
            assertThat(inboundResponse.getMetadata()).isSameAs(metadata);
            assertThat(inboundResponse.getStringHeaders()).isSameAs(stringHeaders);
            assertThat(inboundResponse.bufferEntity()).isTrue();

            inboundResponse.close();
            verify(delegate).close();
        }
    }
}
