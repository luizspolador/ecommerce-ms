package br.com.spolador.ecommerce.product_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductRequestDTO(
        @NotBlank(message = "product name is required")
        String name,

        String description,

        @NotNull(message = "product price is can not be null")
        @Positive(message = "product price must be greater than zero")
        BigDecimal price
) {
}
