package br.com.spolador.ecommerce.order_service.listener;

import br.com.spolador.ecommerce.order_service.event.OrderCreatedDomainEvent;
import br.com.spolador.ecommerce.order_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.order_service.factory.OrderFactory;
import br.com.spolador.ecommerce.order_service.service.OutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for OrderTransactionEventListener")
class OrderTransactionEventListenerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private OrderTransactionEventListener listener;

    @Nested
    @DisplayName("handleOrderCreatedAfterCommit()")
    class HandleOrderCreatedAfterCommitTests {

        @Test
        @DisplayName("When RabbitMQ succeeds, should publish event and mark outbox as processed")
        void whenRabbitSucceeds_shouldPublishAndMarkProcessed() {
            OrderCreatedEvent event = OrderFactory.createOrderCreatedEvent();
            OrderCreatedDomainEvent domainEvent = new OrderCreatedDomainEvent(1L, event);

            listener.handleOrderCreatedAfterCommit(domainEvent);

            verify(rabbitTemplate).convertAndSend(eq("order-events"), eq("order.created"), eq(event));
            verify(outboxService).markAsProcessed(1L);
        }

        @Test
        @DisplayName("When outboxEventId is null, should publish event without marking outbox")
        void whenOutboxIdNull_shouldPublishWithoutMarkingProcessed() {
            OrderCreatedEvent event = OrderFactory.createOrderCreatedEvent();
            OrderCreatedDomainEvent domainEvent = new OrderCreatedDomainEvent(null, event);

            listener.handleOrderCreatedAfterCommit(domainEvent);

            verify(rabbitTemplate).convertAndSend(eq("order-events"), eq("order.created"), eq(event));
            verify(outboxService, never()).markAsProcessed(any());
        }

        @Test
        @DisplayName("When RabbitMQ throws AmqpException, should catch exception and not mark outbox as processed")
        void whenRabbitFails_shouldCatchAndNotMarkProcessed() {
            OrderCreatedEvent event = OrderFactory.createOrderCreatedEvent();
            OrderCreatedDomainEvent domainEvent = new OrderCreatedDomainEvent(1L, event);

            doThrow(new AmqpException("Broker down")).when(rabbitTemplate)
                    .convertAndSend(eq("order-events"), eq("order.created"), eq(event));

            listener.handleOrderCreatedAfterCommit(domainEvent);

            verify(rabbitTemplate).convertAndSend(eq("order-events"), eq("order.created"), eq(event));
            verify(outboxService, never()).markAsProcessed(any());
        }
    }
}
