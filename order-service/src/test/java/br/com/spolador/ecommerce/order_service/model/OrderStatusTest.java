package br.com.spolador.ecommerce.order_service.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for OrderStatus enum")
class OrderStatusTest {

    @Test
    @DisplayName("Should contain all expected enum constants")
    void testOrderStatusValues() {
        assertThat(OrderStatus.values()).containsExactly(
                OrderStatus.CREATED,
                OrderStatus.CONFIRMED,
                OrderStatus.CANCELLED
        );
        assertThat(OrderStatus.valueOf("CREATED")).isEqualTo(OrderStatus.CREATED);
        assertThat(OrderStatus.valueOf("CONFIRMED")).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(OrderStatus.valueOf("CANCELLED")).isEqualTo(OrderStatus.CANCELLED);
    }
}
