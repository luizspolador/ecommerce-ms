package br.com.spolador.ecommerce.inventory_service.listener;

import br.com.spolador.ecommerce.inventory_service.event.OrderCancelledEvent;
import br.com.spolador.ecommerce.inventory_service.event.OrderConfirmedEvent;
import br.com.spolador.ecommerce.inventory_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.inventory_service.factory.InventoryFactory;
import br.com.spolador.ecommerce.inventory_service.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for OrderEventListener in inventory-service")
class OrderEventListenerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderEventListener orderEventListener;

    @Nested
    @DisplayName("handleOrderCreatedEvent()")
    class HandleOrderCreatedEventTests {

        @Test
        @DisplayName("When stock reduction succeeds, should publish OrderConfirmedEvent")
        void whenStockReductionSucceeds_shouldConfirmOrder() {
            OrderCreatedEvent event = InventoryFactory.createOrderCreatedEvent();
            when(inventoryService.processOrderStockReduction(eq(event.orderNumber()), anyList())).thenReturn(true);

            orderEventListener.handleOrderCreatedEvent(event);

            verify(inventoryService).processOrderStockReduction(eq(event.orderNumber()), anyList());

            ArgumentCaptor<OrderConfirmedEvent> captor = ArgumentCaptor.forClass(OrderConfirmedEvent.class);
            verify(rabbitTemplate).convertAndSend(eq("order-events"), eq("order.confirmed"), captor.capture());

            OrderConfirmedEvent publishedEvent = captor.getValue();
            assertThat(publishedEvent.orderNumber()).isEqualTo(event.orderNumber());
            assertThat(publishedEvent.email()).isEqualTo(event.email());
        }

        @Test
        @DisplayName("When order was already processed, should resend OrderConfirmedEvent without re-reducing")
        void whenAlreadyProcessed_shouldResendConfirmation() {
            OrderCreatedEvent event = InventoryFactory.createOrderCreatedEvent();
            when(inventoryService.processOrderStockReduction(eq(event.orderNumber()), anyList())).thenReturn(false);

            orderEventListener.handleOrderCreatedEvent(event);

            verify(inventoryService).processOrderStockReduction(eq(event.orderNumber()), anyList());

            ArgumentCaptor<OrderConfirmedEvent> captor = ArgumentCaptor.forClass(OrderConfirmedEvent.class);
            verify(rabbitTemplate).convertAndSend(eq("order-events"), eq("order.confirmed"), captor.capture());

            OrderConfirmedEvent publishedEvent = captor.getValue();
            assertThat(publishedEvent.orderNumber()).isEqualTo(event.orderNumber());
            assertThat(publishedEvent.email()).isEqualTo(event.email());
        }

        @Test
        @DisplayName("When insufficient stock occurs, should cancel order with insufficient stock reason")
        void whenInsufficientStock_shouldCancelOrder() {
            OrderCreatedEvent event = InventoryFactory.createOrderCreatedEvent();
            when(inventoryService.processOrderStockReduction(eq(event.orderNumber()), anyList()))
                    .thenThrow(new br.com.spolador.ecommerce.inventory_service.exception.InsufficientStockException("IPHONE_15_BLACK", 5, 2));

            orderEventListener.handleOrderCreatedEvent(event);

            ArgumentCaptor<OrderCancelledEvent> captor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
            verify(rabbitTemplate).convertAndSend(eq("order-events"), eq("order.cancelled"), captor.capture());

            OrderCancelledEvent cancelledEvent = captor.getValue();
            assertThat(cancelledEvent.orderNumber()).isEqualTo(event.orderNumber());
            assertThat(cancelledEvent.email()).isEqualTo(event.email());
            assertThat(cancelledEvent.reason()).isEqualTo("Insufficient stock for SKU 'IPHONE_15_BLACK'. Requested: 5, Available: 2");
        }

        @Test
        @DisplayName("When product not found, should cancel order with inventory not found reason")
        void whenProductNotFound_shouldCancelOrder() {
            OrderCreatedEvent event = InventoryFactory.createOrderCreatedEvent();
            when(inventoryService.processOrderStockReduction(eq(event.orderNumber()), anyList()))
                    .thenThrow(new br.com.spolador.ecommerce.inventory_service.exception.ResourceNotFoundException("Inventory", "sku", "UNKNOWN"));

            orderEventListener.handleOrderCreatedEvent(event);

            ArgumentCaptor<OrderCancelledEvent> captor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
            verify(rabbitTemplate).convertAndSend(eq("order-events"), eq("order.cancelled"), captor.capture());

            OrderCancelledEvent cancelledEvent = captor.getValue();
            assertThat(cancelledEvent.orderNumber()).isEqualTo(event.orderNumber());
            assertThat(cancelledEvent.email()).isEqualTo(event.email());
            assertThat(cancelledEvent.reason()).isEqualTo("Inventory not found with sku: 'UNKNOWN'");
        }

        @Test
        @DisplayName("When unexpected exception occurs, should cancel order with technical error reason")
        void whenExceptionOccurs_shouldCancelOrderWithTechnicalError() {
            OrderCreatedEvent event = InventoryFactory.createOrderCreatedEvent();
            when(inventoryService.processOrderStockReduction(eq(event.orderNumber()), anyList()))
                    .thenThrow(new RuntimeException("Database timeout"));

            orderEventListener.handleOrderCreatedEvent(event);

            ArgumentCaptor<OrderCancelledEvent> captor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
            verify(rabbitTemplate).convertAndSend(eq("order-events"), eq("order.cancelled"), captor.capture());

            OrderCancelledEvent cancelledEvent = captor.getValue();
            assertThat(cancelledEvent.orderNumber()).isEqualTo(event.orderNumber());
            assertThat(cancelledEvent.email()).isEqualTo(event.email());
            assertThat(cancelledEvent.reason()).isEqualTo("Technical error during inventory processing");
        }
    }
}
