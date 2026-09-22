package br.com.spolador.ecommerce.product_service.dto;

import br.com.spolador.ecommerce.product_service.factory.ProductFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for Product DTOs")
class ProductDTOTest {

    @Test
    @DisplayName("ProductRequestDTO record tests: accessors, equals, hashCode, toString")
    void testProductRequestDTO() {
        // Arrange & Act
        ProductRequestDTO dto1 = new ProductRequestDTO("Product A", "Desc A", BigDecimal.valueOf(100));
        ProductRequestDTO dto2 = new ProductRequestDTO("Product A", "Desc A", BigDecimal.valueOf(100));
        ProductRequestDTO dto3 = new ProductRequestDTO("Product B", "Desc B", BigDecimal.valueOf(200));

        // Assert
        assertThat(dto1.name()).isEqualTo("Product A");
        assertThat(dto1.description()).isEqualTo("Desc A");
        assertThat(dto1.price()).isEqualTo(BigDecimal.valueOf(100));

        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        assertThat(dto1).isNotEqualTo(dto3);
        assertThat(dto1.toString()).contains("Product A", "Desc A");
    }

    @Test
    @DisplayName("ProductResponseDTO record tests: accessors, equals, hashCode, toString")
    void testProductResponseDTO() {
        // Arrange & Act
        ProductResponseDTO dto1 = new ProductResponseDTO("id1", "Product A", "Desc A", BigDecimal.valueOf(100));
        ProductResponseDTO dto2 = new ProductResponseDTO("id1", "Product A", "Desc A", BigDecimal.valueOf(100));
        ProductResponseDTO dto3 = new ProductResponseDTO("id2", "Product B", "Desc B", BigDecimal.valueOf(200));

        // Assert
        assertThat(dto1.id()).isEqualTo("id1");
        assertThat(dto1.name()).isEqualTo("Product A");
        assertThat(dto1.description()).isEqualTo("Desc A");
        assertThat(dto1.price()).isEqualTo(BigDecimal.valueOf(100));

        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        assertThat(dto1).isNotEqualTo(dto3);
        assertThat(dto1.toString()).contains("id1", "Product A");
    }
}
