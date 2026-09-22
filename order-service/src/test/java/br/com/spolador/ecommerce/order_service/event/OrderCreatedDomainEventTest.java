package br.com.spolador.ecommerce.order_service.event;

import br.com.spolador.ecommerce.order_service.factory.OrderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for OrderCreatedDomainEvent")
class OrderCreatedDomainEventTest {

    @Test
    @DisplayName("Record accessors, equals, hashCode, and toString")
    void testRecord() {
        OrderCreatedEvent event1 = OrderFactory.createOrderCreatedEvent();
        OrderCreatedEvent event2 = OrderFactory.createOrderCreatedEvent();

        OrderCreatedDomainEvent domainEvent1 = new OrderCreatedDomainEvent(1L, event1);
        OrderCreatedDomainEvent domainEvent2 = new OrderCreatedDomainEvent(1L, event2);
        OrderCreatedDomainEvent domainEvent3 = new OrderCreatedDomainEvent(2L, event1);

        assertThat(domainEvent1.outboxEventId()).isEqualTo(1L);
        assertThat(domainEvent1.event()).isEqualTo(event1);

        assertThat(domainEvent1).isEqualTo(domainEvent2);
        assertThat(domainEvent1.hashCode()).isEqualTo(domainEvent2.hashCode());
        assertThat(domainEvent1).isNotEqualTo(domainEvent3);
        assertThat(domainEvent1.toString()).contains("1");
    }
}
