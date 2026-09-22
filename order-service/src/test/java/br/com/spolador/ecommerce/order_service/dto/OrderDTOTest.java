package br.com.spolador.ecommerce.order_service.dto;

import br.com.spolador.ecommerce.order_service.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for Order DTOs")
class OrderDTOTest {

    @Test
    @DisplayName("OrderRequestDTO tests")
    void testOrderRequestDTO() {
        OrderLineItemRequestDTO item = new OrderLineItemRequestDTO("SKU-1", BigDecimal.valueOf(10.0), 2);
        OrderRequestDTO dto1 = new OrderRequestDTO(List.of(item), "user@test.com");
        OrderRequestDTO dto2 = new OrderRequestDTO(List.of(item), "user@test.com");
        OrderRequestDTO dto3 = new OrderRequestDTO(List.of(), "other@test.com");

        assertThat(dto1.getEmail()).isEqualTo("user@test.com");
        assertThat(dto1.getOrderLineItemList()).hasSize(1);

        dto1.setEmail("updated@test.com");
        assertThat(dto1.getEmail()).isEqualTo("updated@test.com");

        OrderRequestDTO noArgs = new OrderRequestDTO();
        noArgs.setEmail("noargs@test.com");
        assertThat(noArgs.getEmail()).isEqualTo("noargs@test.com");

        assertThat(dto2).isEqualTo(new OrderRequestDTO(List.of(item), "user@test.com"));
        assertThat(dto2).isNotEqualTo(dto3);
        assertThat(dto2.hashCode()).isNotEqualTo(dto3.hashCode());
        assertThat(dto2.toString()).contains("user@test.com");
    }

    @Test
    @DisplayName("OrderLineItemRequestDTO tests")
    void testOrderLineItemRequestDTO() {
        OrderLineItemRequestDTO dto1 = new OrderLineItemRequestDTO("SKU-1", BigDecimal.valueOf(10.0), 2);
        OrderLineItemRequestDTO dto2 = new OrderLineItemRequestDTO("SKU-1", BigDecimal.valueOf(10.0), 2);
        OrderLineItemRequestDTO dto3 = new OrderLineItemRequestDTO("SKU-2", BigDecimal.valueOf(20.0), 1);

        assertThat(dto1.getSku()).isEqualTo("SKU-1");
        assertThat(dto1.getPrice()).isEqualTo(BigDecimal.valueOf(10.0));
        assertThat(dto1.getQuantity()).isEqualTo(2);

        dto1.setSku("SKU-NEW");
        dto1.setPrice(BigDecimal.valueOf(15.0));
        dto1.setQuantity(5);
        assertThat(dto1.getSku()).isEqualTo("SKU-NEW");

        OrderLineItemRequestDTO noArgs = new OrderLineItemRequestDTO();
        noArgs.setSku("SKU-NO");
        assertThat(noArgs.getSku()).isEqualTo("SKU-NO");

        assertThat(dto2).isEqualTo(new OrderLineItemRequestDTO("SKU-1", BigDecimal.valueOf(10.0), 2));
        assertThat(dto2).isNotEqualTo(dto3);
        assertThat(dto2.hashCode()).isNotEqualTo(dto3.hashCode());
        assertThat(dto2.toString()).contains("SKU-1");
    }

    @Test
    @DisplayName("OrderResponseDTO tests")
    void testOrderResponseDTO() {
        OrderLineItemResponseDTO item = new OrderLineItemResponseDTO(1L, "SKU-1", BigDecimal.valueOf(10.0), 2);
        OrderResponseDTO dto1 = new OrderResponseDTO(1L, "ORD-1", OrderStatus.CREATED, List.of(item));
        OrderResponseDTO dto2 = new OrderResponseDTO(1L, "ORD-1", OrderStatus.CREATED, List.of(item));
        OrderResponseDTO dto3 = new OrderResponseDTO(2L, "ORD-2", OrderStatus.CONFIRMED, List.of());

        assertThat(dto1.getId()).isEqualTo(1L);
        assertThat(dto1.getOrderNumber()).isEqualTo("ORD-1");
        assertThat(dto1.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(dto1.getOrderLineItemList()).hasSize(1);

        dto1.setId(10L);
        dto1.setOrderNumber("ORD-10");
        dto1.setOrderStatus(OrderStatus.CANCELLED);
        assertThat(dto1.getId()).isEqualTo(10L);

        OrderResponseDTO built = OrderResponseDTO.builder()
                .id(5L)
                .orderNumber("ORD-5")
                .orderStatus(OrderStatus.CONFIRMED)
                .orderLineItemList(List.of())
                .build();
        assertThat(built.getId()).isEqualTo(5L);

        OrderResponseDTO noArgs = new OrderResponseDTO();
        noArgs.setId(20L);
        assertThat(noArgs.getId()).isEqualTo(20L);

        assertThat(dto2).isEqualTo(new OrderResponseDTO(1L, "ORD-1", OrderStatus.CREATED, List.of(item)));
        assertThat(dto2).isNotEqualTo(dto3);
        assertThat(dto2.hashCode()).isNotEqualTo(dto3.hashCode());
        assertThat(dto2.toString()).contains("ORD-1");
    }

    @Test
    @DisplayName("OrderLineItemResponseDTO tests")
    void testOrderLineItemResponseDTO() {
        OrderLineItemResponseDTO dto1 = new OrderLineItemResponseDTO(1L, "SKU-1", BigDecimal.valueOf(10.0), 2);
        OrderLineItemResponseDTO dto2 = new OrderLineItemResponseDTO(1L, "SKU-1", BigDecimal.valueOf(10.0), 2);
        OrderLineItemResponseDTO dto3 = new OrderLineItemResponseDTO(2L, "SKU-2", BigDecimal.valueOf(20.0), 1);

        assertThat(dto1.getId()).isEqualTo(1L);
        assertThat(dto1.getSku()).isEqualTo("SKU-1");
        assertThat(dto1.getPrice()).isEqualTo(BigDecimal.valueOf(10.0));
        assertThat(dto1.getQuantity()).isEqualTo(2);

        dto1.setId(10L);
        dto1.setSku("SKU-MOD");
        dto1.setPrice(BigDecimal.valueOf(50.0));
        dto1.setQuantity(4);
        assertThat(dto1.getId()).isEqualTo(10L);

        OrderLineItemResponseDTO noArgs = new OrderLineItemResponseDTO();
        noArgs.setId(30L);
        assertThat(noArgs.getId()).isEqualTo(30L);

        assertThat(dto2).isEqualTo(new OrderLineItemResponseDTO(1L, "SKU-1", BigDecimal.valueOf(10.0), 2));
        assertThat(dto2).isNotEqualTo(dto3);
        assertThat(dto2.hashCode()).isNotEqualTo(dto3.hashCode());
        assertThat(dto2.toString()).contains("SKU-1");
    }
}
