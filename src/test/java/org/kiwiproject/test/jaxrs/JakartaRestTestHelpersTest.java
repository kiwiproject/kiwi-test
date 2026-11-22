package org.kiwiproject.test.jaxrs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.List;

@DisplayName("JakartaRestTestHelpers")
class JakartaRestTestHelpersTest {

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

            var orderListType = new GenericType<List<Order>>() {
            };
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

            assertThatCode(() ->
                    when(invocation.invoke())
                            .thenReturn(JakartaRestTestHelpers.toInboundResponse(Response.ok(order).build())))
                    .doesNotThrowAnyException();

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

            assertThatThrownBy(() -> inboundResponse.readEntity(new GenericType<List<Order>>() {
            }))
                    .isExactlyInstanceOf(IllegalStateException.class);
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

            assertThatThrownBy(() -> inboundResponse.readEntity(new GenericType<Order>() {
            }, annotations))
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
    }
}
