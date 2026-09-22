package br.com.spolador.ecommerce.inventory_service.mapper;

import br.com.spolador.ecommerce.inventory_service.dto.InventoryRequestDTO;
import br.com.spolador.ecommerce.inventory_service.dto.InventoryResponseDTO;
import br.com.spolador.ecommerce.inventory_service.factory.InventoryFactory;
import br.com.spolador.ecommerce.inventory_service.model.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for InventoryMapper")
class InventoryMapperTest {

    private InventoryMapper inventoryMapper;

    @BeforeEach
    void setUp() {
        inventoryMapper = new InventoryMapperImpl();
    }

    @Test
    @DisplayName("toModel should map InventoryRequestDTO to Inventory")
    void toModel_shouldMapRequestToModel() {
        InventoryRequestDTO requestDTO = InventoryFactory.createInventoryRequestDTO();

        Inventory model = inventoryMapper.toModel(requestDTO);

        assertThat(model).isNotNull();
        assertThat(model.getId()).isNull();
        assertThat(model.getSku()).isEqualTo(requestDTO.getSku());
        assertThat(model.getQuantity()).isEqualTo(requestDTO.getQuantity());
    }

    @Test
    @DisplayName("toModel should return null when requestDTO is null")
    void toModel_shouldReturnNullWhenInputIsNull() {
        Inventory model = inventoryMapper.toModel(null);
        assertThat(model).isNull();
    }

    @Test
    @DisplayName("toResponse should map Inventory to InventoryResponseDTO with inStock true when quantity > 0")
    void toResponse_shouldMapInventoryInStockTrue() {
        Inventory inventory = InventoryFactory.createInventory();

        InventoryResponseDTO responseDTO = inventoryMapper.toResponse(inventory);

        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.getId()).isEqualTo(inventory.getId());
        assertThat(responseDTO.getSku()).isEqualTo(inventory.getSku());
        assertThat(responseDTO.getQuantity()).isEqualTo(inventory.getQuantity());
        assertThat(responseDTO.isInStock()).isTrue();
    }

    @Test
    @DisplayName("toResponse should map inStock false when quantity is 0")
    void toResponse_shouldMapInventoryInStockFalseWhenZero() {
        Inventory inventory = InventoryFactory.createCustomInventory(2L, "SKU-ZERO", 0);

        InventoryResponseDTO responseDTO = inventoryMapper.toResponse(inventory);

        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.isInStock()).isFalse();
    }

    @Test
    @DisplayName("toResponse should return null when inventory is null")
    void toResponse_shouldReturnNullWhenInputIsNull() {
        InventoryResponseDTO responseDTO = inventoryMapper.toResponse(null);
        assertThat(responseDTO).isNull();
    }
}
