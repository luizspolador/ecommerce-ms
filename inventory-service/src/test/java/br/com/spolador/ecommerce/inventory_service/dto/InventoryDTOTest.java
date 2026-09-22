package br.com.spolador.ecommerce.inventory_service.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for Inventory DTOs")
class InventoryDTOTest {

    private final Validator validator;

    public InventoryDTOTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    @DisplayName("InventoryRequestDTO getters, setters, equals, hashCode, toString")
    void testInventoryRequestDTO() {
        InventoryRequestDTO dto1 = new InventoryRequestDTO("SKU-1", 10);
        InventoryRequestDTO dto2 = new InventoryRequestDTO("SKU-1", 10);
        InventoryRequestDTO dto3 = new InventoryRequestDTO("SKU-2", 20);

        assertThat(dto1.getSku()).isEqualTo("SKU-1");
        assertThat(dto1.getQuantity()).isEqualTo(10);

        dto1.setSku("SKU-MOD");
        dto1.setQuantity(15);
        assertThat(dto1.getSku()).isEqualTo("SKU-MOD");
        assertThat(dto1.getQuantity()).isEqualTo(15);

        InventoryRequestDTO noArgs = new InventoryRequestDTO();
        noArgs.setSku("SKU-NOARGS");
        noArgs.setQuantity(5);
        assertThat(noArgs.getSku()).isEqualTo("SKU-NOARGS");

        assertThat(dto2).isNotEqualTo(dto3);
        assertThat(dto2.hashCode()).isNotEqualTo(dto3.hashCode());
        assertThat(dto2.toString()).contains("SKU-1");
    }

    @Test
    @DisplayName("InventoryRequestDTO validation should fail when quantity is null or negative, and pass when valid")
    void testInventoryRequestDTOValidation() {
        InventoryRequestDTO validDTO = new InventoryRequestDTO("SKU-VALID", 5);
        Set<ConstraintViolation<InventoryRequestDTO>> violations = validator.validate(validDTO);
        assertThat(violations).isEmpty();

        InventoryRequestDTO nullQuantityDTO = new InventoryRequestDTO("SKU-VALID", null);
        Set<ConstraintViolation<InventoryRequestDTO>> nullViolations = validator.validate(nullQuantityDTO);
        assertThat(nullViolations).hasSize(1);
        assertThat(nullViolations.iterator().next().getMessage()).isEqualTo("The quantity cannot be null");

        InventoryRequestDTO negativeQuantityDTO = new InventoryRequestDTO("SKU-VALID", -1);
        Set<ConstraintViolation<InventoryRequestDTO>> negativeViolations = validator.validate(negativeQuantityDTO);
        assertThat(negativeViolations).hasSize(1);
        assertThat(negativeViolations.iterator().next().getMessage()).isEqualTo("The quantity must not be lower than zero");
    }

    @Test
    @DisplayName("InventoryResponseDTO getters, setters, builder, equals, hashCode, toString")
    void testInventoryResponseDTO() {
        InventoryResponseDTO dto1 = new InventoryResponseDTO(1L, "SKU-1", 10, true);
        InventoryResponseDTO dto2 = new InventoryResponseDTO(1L, "SKU-1", 10, true);
        InventoryResponseDTO dto3 = new InventoryResponseDTO(2L, "SKU-2", 0, false);

        assertThat(dto1.getId()).isEqualTo(1L);
        assertThat(dto1.getSku()).isEqualTo("SKU-1");
        assertThat(dto1.getQuantity()).isEqualTo(10);
        assertThat(dto1.isInStock()).isTrue();

        dto1.setId(5L);
        dto1.setSku("SKU-5");
        dto1.setQuantity(3);
        dto1.setInStock(true);
        assertThat(dto1.getId()).isEqualTo(5L);

        InventoryResponseDTO built = InventoryResponseDTO.builder()
                .id(10L)
                .sku("SKU-BUILT")
                .quantity(0)
                .inStock(false)
                .build();
        assertThat(built.getId()).isEqualTo(10L);
        assertThat(built.isInStock()).isFalse();

        InventoryResponseDTO noArgs = new InventoryResponseDTO();
        noArgs.setId(11L);
        assertThat(noArgs.getId()).isEqualTo(11L);

        assertThat(dto2).isEqualTo(dto2);
        assertThat(dto2).isEqualTo(new InventoryResponseDTO(1L, "SKU-1", 10, true));
        assertThat(dto2).isNotEqualTo(dto3);
        assertThat(dto2.hashCode()).isNotEqualTo(dto3.hashCode());
        assertThat(dto2.toString()).contains("SKU-1");
    }
}
