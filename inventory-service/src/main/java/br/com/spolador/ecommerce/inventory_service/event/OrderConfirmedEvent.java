package br.com.spolador.ecommerce.inventory_service.event;

import lombok.Builder;

@Builder
public record OrderConfirmedEvent(
        String orderNumber,
        String email
) {
}
