package br.com.spolador.ecommerce.notification_service.event;

import java.util.List;

public record OrderCreatedEvent(
        String orderNumber,
        String email,
        List<OrderItemEvent> items
) {
    public record OrderItemEvent(
         String sku,
         String price,
         Integer quantity
    ){
    }
}
