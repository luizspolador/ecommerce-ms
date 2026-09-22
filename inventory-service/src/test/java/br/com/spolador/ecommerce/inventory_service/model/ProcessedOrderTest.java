package br.com.spolador.ecommerce.inventory_service.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for ProcessedOrder entity")
class ProcessedOrderTest {

    @Test
    @DisplayName("Should test getters, setters, builder and constructors")
    void testProcessedOrderEntity() {
        LocalDateTime now = LocalDateTime.now();
        ProcessedOrder order = new ProcessedOrder();
        order.setId(1L);
        order.setOrderNumber("ORD-100");
        order.setProcessedAt(now);

        assertThat(order.getId()).isEqualTo(1L);
        assertThat(order.getOrderNumber()).isEqualTo("ORD-100");
        assertThat(order.getProcessedAt()).isEqualTo(now);

        ProcessedOrder allArgs = new ProcessedOrder(2L, "ORD-200", now);
        assertThat(allArgs.getId()).isEqualTo(2L);
        assertThat(allArgs.getOrderNumber()).isEqualTo("ORD-200");
        assertThat(allArgs.getProcessedAt()).isEqualTo(now);

        ProcessedOrder built = ProcessedOrder.builder()
                .id(3L)
                .orderNumber("ORD-300")
                .processedAt(now)
                .build();
        assertThat(built.getId()).isEqualTo(3L);
        assertThat(built.getOrderNumber()).isEqualTo("ORD-300");
        assertThat(built.getProcessedAt()).isEqualTo(now);
    }
}
