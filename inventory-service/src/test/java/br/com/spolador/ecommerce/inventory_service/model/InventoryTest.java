package br.com.spolador.ecommerce.inventory_service.model;

import br.com.spolador.ecommerce.inventory_service.factory.InventoryFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for Inventory entity")
class InventoryTest {

    @Test
    @DisplayName("Should test getters, setters, builder and constructors")
    void testInventoryEntity() {
        Inventory inventory = new Inventory();
        inventory.setId(10L);
        inventory.setSku("SKU-TEST");
        inventory.setQuantity(20);

        assertThat(inventory.getId()).isEqualTo(10L);
        assertThat(inventory.getSku()).isEqualTo("SKU-TEST");
        assertThat(inventory.getQuantity()).isEqualTo(20);

        Inventory allArgs = new Inventory(20L, "SKU-20", 30);
        assertThat(allArgs.getId()).isEqualTo(20L);
        assertThat(allArgs.getSku()).isEqualTo("SKU-20");
        assertThat(allArgs.getQuantity()).isEqualTo(30);

        Inventory built = Inventory.builder()
                .id(1L)
                .sku("SKU-BUILDER")
                .quantity(5)
                .build();
        assertThat(built.getId()).isEqualTo(1L);
        assertThat(built.getSku()).isEqualTo("SKU-BUILDER");
        assertThat(built.getQuantity()).isEqualTo(5);
    }
}
