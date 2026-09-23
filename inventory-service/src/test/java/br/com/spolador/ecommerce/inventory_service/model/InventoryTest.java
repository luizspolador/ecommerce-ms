package br.com.spolador.ecommerce.inventory_service.model;

import br.com.spolador.ecommerce.inventory_service.factory.InventoryFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for Inventory entity")
class InventoryTest {

    @Test
    @DisplayName("Should test getters, setters, builder and constructors")
    void testInventoryEntity() {
        final LocalDateTime now = LocalDateTime.now();
        final Inventory inventory = new Inventory();
        inventory.setId(10L);
        inventory.setSku("SKU-TEST");
        inventory.setQuantity(20);
        inventory.setCreatedAt(now);
        inventory.setUpdatedAt(now);

        assertThat(inventory.getId()).isEqualTo(10L);
        assertThat(inventory.getSku()).isEqualTo("SKU-TEST");
        assertThat(inventory.getQuantity()).isEqualTo(20);
        assertThat(inventory.getCreatedAt()).isEqualTo(now);
        assertThat(inventory.getUpdatedAt()).isEqualTo(now);

        final Inventory allArgs = new Inventory(20L, "SKU-20", 30, now, now);
        assertThat(allArgs.getId()).isEqualTo(20L);
        assertThat(allArgs.getSku()).isEqualTo("SKU-20");
        assertThat(allArgs.getQuantity()).isEqualTo(30);
        assertThat(allArgs.getCreatedAt()).isEqualTo(now);
        assertThat(allArgs.getUpdatedAt()).isEqualTo(now);

        final Inventory built = Inventory.builder()
                .id(1L)
                .sku("SKU-BUILDER")
                .quantity(5)
                .createdAt(now)
                .updatedAt(now)
                .build();
        assertThat(built.getId()).isEqualTo(1L);
        assertThat(built.getSku()).isEqualTo("SKU-BUILDER");
        assertThat(built.getQuantity()).isEqualTo(5);
        assertThat(built.getCreatedAt()).isEqualTo(now);
        assertThat(built.getUpdatedAt()).isEqualTo(now);
    }
}
