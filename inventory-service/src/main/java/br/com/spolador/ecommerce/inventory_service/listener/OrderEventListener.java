package br.com.spolador.ecommerce.inventory_service.listener;

import br.com.spolador.ecommerce.inventory_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class OrderEventListener {

    private final InventoryService inventoryService;

    @RabbitListener(queues = "inventory-queue")
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Event received in Inventory to Order: {}", event.orderNumber());
        event.items().forEach(item -> {
            try {
                inventoryService.reduceStock(item.sku(), item.quantity());
                log.info("Discounted stock for SKU: {} - Quantity: {}", item.sku(), item.quantity());
            } catch(Exception e) {
                log.error("Error to reduce stock for SKU: {}: {}", item.sku(), e.getMessage());
            }
        });
    }
}
