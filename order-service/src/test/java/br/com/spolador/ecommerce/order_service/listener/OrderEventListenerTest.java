package br.com.spolador.ecommerce.order_service.listener;

import br.com.spolador.ecommerce.order_service.event.OrderCancelledEvent;
import br.com.spolador.ecommerce.order_service.event.OrderConfirmedEvent;
import br.com.spolador.ecommerce.order_service.factory.OrderFactory;
import br.com.spolador.ecommerce.order_service.model.OrderStatus;
import br.com.spolador.ecommerce.order_service.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for OrderEventListener in order-service")
class OrderEventListenerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderEventListener orderEventListener;

    @Nested
    @DisplayName("handleOrderConfirmed()")
    class HandleOrderConfirmedTests {

        @Test
        @DisplayName("When orderNumber is null, should return without updating status")
        void whenOrderNumberNull_shouldReturn() {
            OrderConfirmedEvent event = new OrderConfirmedEvent(null, "user@test.com");

            orderEventListener.handleOrderConfirmed(event);

            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("When orderNumber is valid, should update status to CONFIRMED")
        void whenOrderNumberValid_shouldUpdateStatusToConfirmed() {
            OrderConfirmedEvent event = OrderFactory.createOrderConfirmedEvent();

            orderEventListener.handleOrderConfirmed(event);

            verify(orderService).updateOrderStatus(event.orderNumber(), OrderStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("handleOrderCancelled()")
    class HandleOrderCancelledTests {

        @Test
        @DisplayName("When orderNumber is null, should return without updating status")
        void whenOrderNumberNull_shouldReturn() {
            OrderCancelledEvent event = new OrderCancelledEvent(null, "user@test.com", "reason");

            orderEventListener.handleOrderCancelled(event);

            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("When orderNumber is valid, should update status to CANCELLED")
        void whenOrderNumberValid_shouldUpdateStatusToCancelled() {
            OrderCancelledEvent event = OrderFactory.createOrderCancelledEvent("Out of stock");

            orderEventListener.handleOrderCancelled(event);

            verify(orderService).updateOrderStatus(event.orderNumber(), OrderStatus.CANCELLED, event.reason());
        }
    }
}
