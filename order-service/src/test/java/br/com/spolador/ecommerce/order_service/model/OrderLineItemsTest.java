package br.com.spolador.ecommerce.order_service.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for OrderLineItems entity")
class OrderLineItemsTest {

    @Test
    @DisplayName("Should test getters, setters, builder and constructors")
    void testOrderLineItemsEntity() {
        OrderLineItems item = new OrderLineItems();
        item.setId(5L);
        item.setSku("SKU-1");
        item.setPrice(BigDecimal.valueOf(50.0));
        item.setQuantity(3);

        assertThat(item.getId()).isEqualTo(5L);
        assertThat(item.getSku()).isEqualTo("SKU-1");
        assertThat(item.getPrice()).isEqualTo(BigDecimal.valueOf(50.0));
        assertThat(item.getQuantity()).isEqualTo(3);

        OrderLineItems built = OrderLineItems.builder()
                .id(1L)
                .sku("SKU-2")
                .price(BigDecimal.valueOf(10.0))
                .quantity(1)
                .build();
        assertThat(built.getId()).isEqualTo(1L);

        OrderLineItems allArgs = new OrderLineItems(2L, "SKU-3", BigDecimal.valueOf(20.0), 2);
        assertThat(allArgs.getId()).isEqualTo(2L);
    }
}
