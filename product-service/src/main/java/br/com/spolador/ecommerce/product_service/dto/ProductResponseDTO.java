package br.com.spolador.ecommerce.product_service.dto;

import java.math.BigDecimal;

public record ProductResponseDTO(
        String id,
        String name,
        String description,
        BigDecimal price
) {
}
