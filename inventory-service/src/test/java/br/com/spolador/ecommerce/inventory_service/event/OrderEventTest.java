package br.com.spolador.ecommerce.inventory_service.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for Order Events in inventory-service")
class OrderEventTest {

    @Test
    @DisplayName("OrderCreatedEvent and OrderItemEvent records test")
    void testOrderCreatedEvent() {
        OrderCreatedEvent.OrderItemEvent item = new OrderCreatedEvent.OrderItemEvent("SKU-1", "99.99", 2);
        assertThat(item.sku()).isEqualTo("SKU-1");
        assertThat(item.price()).isEqualTo("99.99");
        assertThat(item.quantity()).isEqualTo(2);

        OrderCreatedEvent event1 = new OrderCreatedEvent("ORD-1", "user@test.com", List.of(item));
        OrderCreatedEvent event2 = new OrderCreatedEvent("ORD-1", "user@test.com", List.of(item));
        OrderCreatedEvent event3 = new OrderCreatedEvent("ORD-2", "user@test.com", List.of(item));

        assertThat(event1.orderNumber()).isEqualTo("ORD-1");
        assertThat(event1.email()).isEqualTo("user@test.com");
        assertThat(event1.items()).hasSize(1);
        assertThat(event1).isEqualTo(event2);
        assertThat(event1).isNotEqualTo(event3);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
        assertThat(event1.toString()).contains("ORD-1");
    }

    @Test
    @DisplayName("OrderConfirmedEvent record test")
    void testOrderConfirmedEvent() {
        OrderConfirmedEvent event1 = new OrderConfirmedEvent("ORD-1", "user@test.com");
        OrderConfirmedEvent event2 = OrderConfirmedEvent.builder().orderNumber("ORD-1").email("user@test.com").build();

        assertThat(event1.orderNumber()).isEqualTo("ORD-1");
        assertThat(event1.email()).isEqualTo("user@test.com");
        assertThat(event1).isEqualTo(event2);
        assertThat(event1.toString()).contains("ORD-1");
    }

    @Test
    @DisplayName("OrderCancelledEvent record test")
    void testOrderCancelledEvent() {
        OrderCancelledEvent event1 = new OrderCancelledEvent("ORD-1", "user@test.com", "Out of stock");
        OrderCancelledEvent event2 = new OrderCancelledEvent("ORD-1", "user@test.com", "Out of stock");

        assertThat(event1.orderNumber()).isEqualTo("ORD-1");
        assertThat(event1.email()).isEqualTo("user@test.com");
        assertThat(event1.reason()).isEqualTo("Out of stock");
        assertThat(event1).isEqualTo(event2);
        assertThat(event1.toString()).contains("Out of stock");
    }
}
