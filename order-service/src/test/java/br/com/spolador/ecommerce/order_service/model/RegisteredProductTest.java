package br.com.spolador.ecommerce.order_service.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for RegisteredProduct entity in order-service")
class RegisteredProductTest {

    @Test
    @DisplayName("Should test getters, setters, builder, equals, and hashCode")
    void testEntity() {
        LocalDateTime now = LocalDateTime.now();
        RegisteredProduct product1 = RegisteredProduct.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Keyboard")
                .registeredAt(now)
                .build();

        RegisteredProduct product2 = new RegisteredProduct(1L, "SKU-001", "Keyboard", now);
        RegisteredProduct product3 = new RegisteredProduct();
        product3.setId(2L);
        product3.setSku("SKU-002");
        product3.setName("Mouse");
        product3.setRegisteredAt(now);

        assertThat(product1.getId()).isEqualTo(1L);
        assertThat(product1.getSku()).isEqualTo("SKU-001");
        assertThat(product1.getName()).isEqualTo("Keyboard");
        assertThat(product1.getRegisteredAt()).isEqualTo(now);

        assertThat(product1).isEqualTo(product2);
        assertThat(product1.hashCode()).isEqualTo(product2.hashCode());
        assertThat(product1).isNotEqualTo(product3);
    }
}
