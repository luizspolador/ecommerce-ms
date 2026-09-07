package br.com.spolador.ecommerce.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderLineItemResponseDTO {
    private Long id;
    private String sku;
    private BigDecimal price;
    private Integer quantity;
}
