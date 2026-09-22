package br.com.spolador.ecommerce.order_service.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for OutboxEvent entity")
class OutboxEventTest {

    @Test
    @DisplayName("Should test getters, setters, builder, equals, hashCode and toString")
    void testOutboxEventEntity() {
        LocalDateTime now = LocalDateTime.now();
        OutboxEvent event1 = new OutboxEvent(1L, "ORD-1", "ORDER_CREATED", "{}", now, false);
        OutboxEvent event2 = new OutboxEvent(1L, "ORD-1", "ORDER_CREATED", "{}", now, false);
        OutboxEvent event3 = new OutboxEvent(2L, "ORD-2", "ORDER_CREATED", "{}", now, true);

        assertThat(event1.getId()).isEqualTo(1L);
        assertThat(event1.getAggregateId()).isEqualTo("ORD-1");
        assertThat(event1.getType()).isEqualTo("ORDER_CREATED");
        assertThat(event1.getPayload()).isEqualTo("{}");
        assertThat(event1.getCreatedAt()).isEqualTo(now);
        assertThat(event1.isProcessed()).isFalse();

        event1.setProcessed(true);
        assertThat(event1.isProcessed()).isTrue();

        OutboxEvent noArgs = new OutboxEvent();
        noArgs.setId(99L);
        assertThat(noArgs.getId()).isEqualTo(99L);

        OutboxEvent built = OutboxEvent.builder()
                .id(5L)
                .aggregateId("ORD-5")
                .type("ORDER_CREATED")
                .payload("{\"key\":\"val\"}")
                .createdAt(now)
                .processed(false)
                .build();
        assertThat(built.getId()).isEqualTo(5L);

        assertThat(event2).isEqualTo(new OutboxEvent(1L, "ORD-1", "ORDER_CREATED", "{}", now, false));
        assertThat(event2).isNotEqualTo(event3);
        assertThat(event2.hashCode()).isNotEqualTo(event3.hashCode());
        assertThat(event2.toString()).contains("ORD-1");
    }
}
