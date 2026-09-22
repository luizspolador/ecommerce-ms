package br.com.spolador.ecommerce.product_service.mapper;

import br.com.spolador.ecommerce.product_service.dto.ProductRequestDTO;
import br.com.spolador.ecommerce.product_service.dto.ProductResponseDTO;
import br.com.spolador.ecommerce.product_service.factory.ProductFactory;
import br.com.spolador.ecommerce.product_service.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for ProductMapperImpl")
class ProductMapperTest {

    private ProductMapper productMapper;

    @BeforeEach
    void setUp() {
        productMapper = new ProductMapperImpl();
    }

    @Test
    @DisplayName("toProduct should map ProductRequestDTO to Product correctly")
    void toProduct_shouldMapProductRequestDTOToProduct() {
        // Arrange
        ProductRequestDTO requestDTO = ProductFactory.createProductRequestDTO();

        // Act
        Product product = productMapper.toProduct(requestDTO);

        // Assert
        assertThat(product).isNotNull();
        assertThat(product.getId()).isNull();
        assertThat(product.getName()).isEqualTo(requestDTO.name());
        assertThat(product.getDescription()).isEqualTo(requestDTO.description());
        assertThat(product.getPrice()).isEqualTo(requestDTO.price());
    }

    @Test
    @DisplayName("toProduct should return null when ProductRequestDTO is null")
    void toProduct_shouldReturnNullWhenInputIsNull() {
        // Act
        Product product = productMapper.toProduct(null);

        // Assert
        assertThat(product).isNull();
    }

    @Test
    @DisplayName("toProductResponseDTO should map Product to ProductResponseDTO correctly")
    void toProductResponseDTO_shouldMapProductToProductResponseDTO() {
        // Arrange
        Product product = ProductFactory.createProduct();

        // Act
        ProductResponseDTO responseDTO = productMapper.toProductResponseDTO(product);

        // Assert
        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.id()).isEqualTo(product.getId());
        assertThat(responseDTO.name()).isEqualTo(product.getName());
        assertThat(responseDTO.description()).isEqualTo(product.getDescription());
        assertThat(responseDTO.price()).isEqualTo(product.getPrice());
    }

    @Test
    @DisplayName("toProductResponseDTO should return null when Product is null")
    void toProductResponseDTO_shouldReturnNullWhenInputIsNull() {
        // Act
        ProductResponseDTO responseDTO = productMapper.toProductResponseDTO(null);

        // Assert
        assertThat(responseDTO).isNull();
    }

    @Test
    @DisplayName("updateProductFromRequest should update Product fields from ProductRequestDTO")
    void updateProductFromRequest_shouldUpdateProduct() {
        // Arrange
        Product product = ProductFactory.createProduct();
        ProductRequestDTO updateDTO = ProductFactory.createCustomProductRequestDTO("Updated Name", "Updated Desc", BigDecimal.valueOf(1999.99));

        // Act
        productMapper.updateProductFromRequest(updateDTO, product);

        // Assert
        assertThat(product.getId()).isEqualTo(ProductFactory.DEFAULT_ID); // ID remains untouched
        assertThat(product.getName()).isEqualTo("Updated Name");
        assertThat(product.getDescription()).isEqualTo("Updated Desc");
        assertThat(product.getPrice()).isEqualTo(BigDecimal.valueOf(1999.99));
    }

    @Test
    @DisplayName("updateProductFromRequest should not change Product when ProductRequestDTO is null")
    void updateProductFromRequest_shouldDoNothingWhenInputIsNull() {
        // Arrange
        Product product = ProductFactory.createProduct();

        // Act
        productMapper.updateProductFromRequest(null, product);

        // Assert
        assertThat(product.getName()).isEqualTo(ProductFactory.DEFAULT_NAME);
        assertThat(product.getDescription()).isEqualTo(ProductFactory.DEFAULT_DESCRIPTION);
        assertThat(product.getPrice()).isEqualTo(ProductFactory.DEFAULT_PRICE);
    }
}
