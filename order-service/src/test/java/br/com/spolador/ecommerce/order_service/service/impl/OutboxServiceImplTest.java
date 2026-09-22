package br.com.spolador.ecommerce.order_service.service.impl;

import br.com.spolador.ecommerce.order_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.order_service.factory.OrderFactory;
import br.com.spolador.ecommerce.order_service.model.OutboxEvent;
import br.com.spolador.ecommerce.order_service.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for OutboxServiceImpl")
class OutboxServiceImplTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxServiceImpl outboxService;

    @Nested
    @DisplayName("saveOrderCreatedEvent()")
    class SaveOrderCreatedEventTests {

        @Test
        @DisplayName("Should serialize event and save OutboxEvent")
        void shouldSerializeAndSave() {
            OrderCreatedEvent event = OrderFactory.createOrderCreatedEvent();
            when(objectMapper.writeValueAsString(event)).thenReturn("{\"orderNumber\":\"ORD-1\"}");

            OutboxEvent mockSaved = OrderFactory.createOutboxEvent(true);
            when(outboxRepository.save(any(OutboxEvent.class))).thenReturn(mockSaved);

            OutboxEvent result = outboxService.saveOrderCreatedEvent(event, true);

            assertThat(result).isEqualTo(mockSaved);
            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxRepository).save(captor.capture());

            OutboxEvent saved = captor.getValue();
            assertThat(saved.getAggregateId()).isEqualTo(event.orderNumber());
            assertThat(saved.getType()).isEqualTo("ORDER_CREATED");
            assertThat(saved.getPayload()).isEqualTo("{\"orderNumber\":\"ORD-1\"}");
            assertThat(saved.isProcessed()).isTrue();
            assertThat(saved.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("getPendingEvents()")
    class GetPendingEventsTests {

        @Test
        @DisplayName("Should return pending events from repository")
        void shouldReturnPendingEvents() {
            List<OutboxEvent> events = List.of(OrderFactory.createOutboxEvent(false));
            when(outboxRepository.findByProcessedFalse()).thenReturn(events);

            List<OutboxEvent> result = outboxService.getPendingEvents();

            assertThat(result).isEqualTo(events);
            verify(outboxRepository).findByProcessedFalse();
        }
    }

    @Nested
    @DisplayName("markAsProcessed()")
    class MarkAsProcessedTests {

        @Test
        @DisplayName("When event found, should set processed to true and save")
        void whenFound_shouldMarkAsProcessed() {
            OutboxEvent event = OrderFactory.createOutboxEvent(false);
            when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

            outboxService.markAsProcessed(1L);

            assertThat(event.isProcessed()).isTrue();
            verify(outboxRepository).save(event);
        }

        @Test
        @DisplayName("When event not found, should do nothing")
        void whenNotFound_shouldDoNothing() {
            when(outboxRepository.findById(999L)).thenReturn(Optional.empty());

            outboxService.markAsProcessed(999L);

            verify(outboxRepository, never()).save(any());
        }
    }
}
