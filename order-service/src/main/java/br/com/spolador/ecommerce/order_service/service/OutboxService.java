package br.com.spolador.ecommerce.order_service.service;

import br.com.spolador.ecommerce.order_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.order_service.model.OutboxEvent;

import java.util.List;

public interface OutboxService {
    void saveOrderCreatedEvent(OrderCreatedEvent event, boolean isProcessed);
    List<OutboxEvent> getPendingEvents();
    void markAsProcessed(Long id);
}
