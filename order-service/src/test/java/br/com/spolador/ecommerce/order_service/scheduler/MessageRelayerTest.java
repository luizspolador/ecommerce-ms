package br.com.spolador.ecommerce.order_service.scheduler;

import br.com.spolador.ecommerce.order_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.order_service.factory.OrderFactory;
import br.com.spolador.ecommerce.order_service.model.OutboxEvent;
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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for MessageRelayer")
class MessageRelayerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private OutboxService outboxService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MessageRelayer messageRelayer;

    @Nested
    @DisplayName("relayMessage()")
    class RelayMessageTests {

        @Test
        @DisplayName("When no pending events, should do nothing")
        void whenNoPendingEvents_shouldDoNothing() {
            when(outboxService.getPendingEvents()).thenReturn(Collections.emptyList());

            messageRelayer.relayMessage();

            verifyNoInteractions(rabbitTemplate);
            verify(outboxService, never()).markAsProcessed(any());
        }

        @Test
        @DisplayName("When pending events present, should deserialize, send to rabbit and mark processed")
        void whenPendingEventsPresent_shouldProcessSuccessfully() {
            OutboxEvent event = OrderFactory.createOutboxEvent(false);
            OrderCreatedEvent createdEvent = OrderFactory.createOrderCreatedEvent();

            when(outboxService.getPendingEvents()).thenReturn(List.of(event));
            when(objectMapper.readValue(event.getPayload(), OrderCreatedEvent.class)).thenReturn(createdEvent);

            messageRelayer.relayMessage();

            verify(rabbitTemplate).convertAndSend("order-events", "order.created", createdEvent);
            verify(outboxService).markAsProcessed(event.getId());
        }

        @Test
        @DisplayName("When JacksonException occurs, should catch and continue")
        void whenJacksonException_shouldCatchAndContinue() {
            OutboxEvent event = OrderFactory.createOutboxEvent(false);
            JacksonException jacksonException = mock(JacksonException.class);

            when(outboxService.getPendingEvents()).thenReturn(List.of(event));
            when(objectMapper.readValue(event.getPayload(), OrderCreatedEvent.class)).thenThrow(jacksonException);

            messageRelayer.relayMessage();

            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
            verify(outboxService, never()).markAsProcessed(any());
        }

        @Test
        @DisplayName("When AmqpException occurs, should catch and continue without marking as processed")
        void whenAmqpException_shouldCatchAndContinue() {
            OutboxEvent event = OrderFactory.createOutboxEvent(false);
            OrderCreatedEvent createdEvent = OrderFactory.createOrderCreatedEvent();

            when(outboxService.getPendingEvents()).thenReturn(List.of(event));
            when(objectMapper.readValue(event.getPayload(), OrderCreatedEvent.class)).thenReturn(createdEvent);
            doThrow(new AmqpException("Broker down")).when(rabbitTemplate).convertAndSend(eq("order-events"), eq("order.created"), eq(createdEvent));

            messageRelayer.relayMessage();

            verify(rabbitTemplate).convertAndSend("order-events", "order.created", createdEvent);
            verify(outboxService, never()).markAsProcessed(event.getId());
        }
    }
}
