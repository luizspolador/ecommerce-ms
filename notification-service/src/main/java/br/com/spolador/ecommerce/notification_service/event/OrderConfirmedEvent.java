package br.com.spolador.ecommerce.notification_service.event;

import lombok.Builder;

@Builder
public record OrderConfirmedEvent(
        String orderNumber,
        String email
) {
}
