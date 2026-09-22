package br.com.spolador.ecommerce.order_service.model;

import br.com.spolador.ecommerce.order_service.factory.OrderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for Order entity")
class OrderTest {

    @Test
    @DisplayName("Should test getters, setters, builder and constructors")
    void testOrderEntity() {
        Order order = new Order();
        order.setId(10L);
        order.setOrderNumber("ORD-999");
        order.setUserId("user-1");
        order.setOrderStatus(OrderStatus.CREATED);
        order.setOrderLineItemList(List.of());

        assertThat(order.getId()).isEqualTo(10L);
        assertThat(order.getOrderNumber()).isEqualTo("ORD-999");
        assertThat(order.getUserId()).isEqualTo("user-1");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getOrderLineItemList()).isEmpty();

        Order built = Order.builder()
                .id(1L)
                .orderNumber("ORD-1")
                .userId("user-2")
                .orderStatus(OrderStatus.CONFIRMED)
                .orderLineItemList(List.of())
                .build();
        assertThat(built.getId()).isEqualTo(1L);
        assertThat(built.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);

        Order allArgs = new Order(2L, "ORD-2", "user-3", OrderStatus.CANCELLED, List.of());
        assertThat(allArgs.getId()).isEqualTo(2L);
        assertThat(allArgs.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
