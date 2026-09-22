package br.com.spolador.ecommerce.order_service.listener;

import br.com.spolador.ecommerce.order_service.event.OrderCreatedDomainEvent;
import br.com.spolador.ecommerce.order_service.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderTransactionEventListener {

    private final RabbitTemplate rabbitTemplate;
    private final OutboxService outboxService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleOrderCreatedAfterCommit(OrderCreatedDomainEvent domainEvent) {
        log.info("Transaction committed successfully for order: {}. Dispatching event to RabbitMQ.", domainEvent.event().orderNumber());
        try {
            rabbitTemplate.convertAndSend("order-events", "order.created", domainEvent.event());
            if (domainEvent.outboxEventId() != null) {
                outboxService.markAsProcessed(domainEvent.outboxEventId());
            }
            log.info("Order created event sent to RabbitMQ and marked as processed for order: {}", domainEvent.event().orderNumber());
        } catch (AmqpException e) {
            log.warn("RabbitMQ unavailable immediately after commit for order: {}. Outbox relayer will ensure delivery. Cause: {}",
                    domainEvent.event().orderNumber(), e.getMessage());
        }
    }
}
