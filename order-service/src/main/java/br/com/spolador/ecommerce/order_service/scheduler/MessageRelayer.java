package br.com.spolador.ecommerce.order_service.scheduler;

import br.com.spolador.ecommerce.order_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.order_service.model.OutboxEvent;
import br.com.spolador.ecommerce.order_service.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageRelayer {
    private final RabbitTemplate rabbitTemplate;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 10000)
    public void relayMessage() {
        List<OutboxEvent> pendingEvents = outboxService.getPendingEvents();
        if(!pendingEvents.isEmpty()) {
            log.info("Relayer: {} pending messages.", pendingEvents.size());
            for(OutboxEvent event:pendingEvents){
                try {
                    OrderCreatedEvent originalEvent = objectMapper.readValue(
                            event.getPayload(), OrderCreatedEvent.class
                    );
                    rabbitTemplate.convertAndSend("order-events", "order.created", originalEvent);
                    outboxService.markAsProcessed(event.getId());
                    log.info("Message recovered and resent: {}", event.getAggregateId());
                } catch (JacksonException e) {
                    log.error("Error deserializing event {}: {}", event.getId(), e.getMessage());
                } catch (AmqpException e) {
                    log.error("Failed to resend {}: {}", event.getId(), e.getMessage());
                }
            }
        }
    }
}
