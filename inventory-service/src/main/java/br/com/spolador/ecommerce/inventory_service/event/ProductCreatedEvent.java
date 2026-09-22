package br.com.spolador.ecommerce.inventory_service.event;

import java.math.BigDecimal;

public record ProductCreatedEvent(
        String id,
        String sku,
        String name,
        BigDecimal price
) {
}
