package br.com.spolador.ecommerce.order_service.service.impl;

import br.com.spolador.ecommerce.order_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.order_service.model.OutboxEvent;
import br.com.spolador.ecommerce.order_service.repository.OutboxRepository;
import br.com.spolador.ecommerce.order_service.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public OutboxEvent saveOrderCreatedEvent(OrderCreatedEvent event, boolean isProcessed) {
        String payload = objectMapper.writeValueAsString(event);
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateId(event.orderNumber())
                .type("ORDER_CREATED")
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .processed(isProcessed)
                .build();
        OutboxEvent saved = outboxRepository.save(outboxEvent);
        log.info("Event saved in outbox: {}", event.orderNumber());
        return saved;
    }

    @Override
    public List<OutboxEvent> getPendingEvents() {
        return outboxRepository.findByProcessedFalse();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsProcessed(Long id) {
        outboxRepository.findById(id).ifPresent(event -> {
            event.setProcessed(true);
            outboxRepository.save(event);
            log.info("Event {} as marked as processed", id);
        });
    }
}
