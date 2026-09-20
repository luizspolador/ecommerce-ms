package br.com.spolador.ecommerce.inventory_service.listener;

import br.com.spolador.ecommerce.inventory_service.event.OrderCancelledEvent;
import br.com.spolador.ecommerce.inventory_service.event.OrderCreatedEvent;
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
            boolean allProductsInStock = event.items().stream()
                            .allMatch(item -> inventoryService.isInStock(item.sku(), item.quantity()));
            if(!allProductsInStock){
                cancelOrder(event, "Insufficient stock for one or more products");
                return;
            }
            event.items().forEach(item -> {
                inventoryService.reduceStock(item.sku(), item.quantity());
            });
            rabbitTemplate.convertAndSend("order-events", "order.confirmed", event);
            log.info("Discounted stock for order number {}", event.orderNumber());
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
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
