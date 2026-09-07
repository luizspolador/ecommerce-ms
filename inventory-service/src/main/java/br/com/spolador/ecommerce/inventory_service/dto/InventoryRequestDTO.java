package br.com.spolador.ecommerce.inventory_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class InventoryRequestDTO {
    @NotBlank(message = "The sku can not be null")
    private String sku;

    @Min(value = 0, message = "The quantity must not be lower than zero")
    private Integer quantity;
}
