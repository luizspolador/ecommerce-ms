package br.com.spolador.ecommerce.order_service.event;

public record OrderCreatedDomainEvent(Long outboxEventId, OrderCreatedEvent event) {
}
