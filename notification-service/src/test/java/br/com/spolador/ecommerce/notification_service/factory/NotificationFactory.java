package br.com.spolador.ecommerce.notification_service.factory;

import br.com.spolador.ecommerce.notification_service.event.OrderCancelledEvent;
import br.com.spolador.ecommerce.notification_service.event.OrderConfirmedEvent;

public class NotificationFactory {

    public static OrderConfirmedEvent createOrderConfirmedEvent() {
        return OrderConfirmedEvent.builder()
                .orderNumber("ORD-12345")
                .email("customer@example.com")
                .build();
    }

    public static OrderCancelledEvent createOrderCancelledEvent(String reason) {
        return new OrderCancelledEvent("ORD-12345", "customer@example.com", reason);
    }

    public static OrderCancelledEvent createOrderCancelledEvent() {
        return createOrderCancelledEvent("Insufficient stock for product PROD-001");
    }
}
