package br.com.spolador.ecommerce.inventory_service.listener;

import br.com.spolador.ecommerce.inventory_service.event.OrderCancelledEvent;
import br.com.spolador.ecommerce.inventory_service.event.OrderConfirmedEvent;
import br.com.spolador.ecommerce.inventory_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.inventory_service.exception.InsufficientStockException;
import br.com.spolador.ecommerce.inventory_service.exception.ResourceNotFoundException;
import br.com.spolador.ecommerce.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class OrderEventListener {

    private final InventoryService inventoryService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "inventory-queue")
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Event received in Inventory to Order: {}", event.orderNumber());
        try {
            boolean processed = inventoryService.processOrderStockReduction(event.orderNumber(), event.items());
            OrderConfirmedEvent confirmedEvent = new OrderConfirmedEvent(event.orderNumber(), event.email());
            rabbitTemplate.convertAndSend("order-events", "order.confirmed", confirmedEvent);
            if (processed) {
                log.info("Discounted stock for order number {}", event.orderNumber());
            } else {
                log.info("Order number {} was already processed. Resent confirmation event.", event.orderNumber());
            }
        } catch (InsufficientStockException | ResourceNotFoundException e) {
            log.warn("Insufficient stock or product not found for order {}: {}", event.orderNumber(), e.getMessage());
            cancelOrder(event, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during inventory processing for order {}: {}", event.orderNumber(), e.getMessage());
            cancelOrder(event, "Technical error during inventory processing");
        }
    }

    private void cancelOrder(OrderCreatedEvent event, String reason) {
        OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
                event.orderNumber(), event.email(), reason
        );
        rabbitTemplate.convertAndSend("order-events", "order.cancelled", cancelledEvent);
    }
}
