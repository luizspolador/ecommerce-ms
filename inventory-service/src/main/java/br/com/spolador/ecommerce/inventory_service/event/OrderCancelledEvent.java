package br.com.spolador.ecommerce.inventory_service.event;

public record OrderCancelledEvent(
        String orderNumber,
        String email,
        String reason
) {
}
