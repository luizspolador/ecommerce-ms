package br.com.spolador.ecommerce.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDTO {
    @NotEmpty(message = "the order must have at least an item")
    @Valid
    private List<OrderLineItemRequestDTO> orderLineItemList;

    @NotBlank(message = "Email is required")
    @Email(message = "Email invalid format")
    private String email;
}
