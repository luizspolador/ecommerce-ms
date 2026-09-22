package br.com.spolador.ecommerce.product_service.factory;

import br.com.spolador.ecommerce.product_service.dto.ProductRequestDTO;
import br.com.spolador.ecommerce.product_service.dto.ProductResponseDTO;
import br.com.spolador.ecommerce.product_service.model.Product;

import java.math.BigDecimal;
import java.util.List;

/**
 * Factory for creating test fixtures (Object Mother Pattern)
 * for Products, ProductRequestDTOs, and ProductResponseDTOs.
 */
public final class ProductFactory {

    private ProductFactory() {
        // Utility / Factory class
    }

    public static final String DEFAULT_ID = "65f1a2b3c4d5e6f7a8b9c0d1";
    public static final String DEFAULT_NAME = "Smartphone XYZ";
    public static final String DEFAULT_DESCRIPTION = "High-end smartphone with OLED display";
    public static final BigDecimal DEFAULT_PRICE = BigDecimal.valueOf(1499.99);

    public static Product createProduct() {
        return Product.builder()
                .id(DEFAULT_ID)
                .name(DEFAULT_NAME)
                .description(DEFAULT_DESCRIPTION)
                .price(DEFAULT_PRICE)
                .build();
    }

    public static Product createProductWithoutId() {
        return Product.builder()
                .name(DEFAULT_NAME)
                .description(DEFAULT_DESCRIPTION)
                .price(DEFAULT_PRICE)
                .build();
    }

    public static Product createCustomProduct(String id, String name, String description, BigDecimal price) {
        return Product.builder()
                .id(id)
                .name(name)
                .description(description)
                .price(price)
                .build();
    }

    public static ProductRequestDTO createProductRequestDTO() {
        return new ProductRequestDTO(DEFAULT_NAME, DEFAULT_DESCRIPTION, DEFAULT_PRICE);
    }

    public static ProductRequestDTO createCustomProductRequestDTO(String name, String description, BigDecimal price) {
        return new ProductRequestDTO(name, description, price);
    }

    public static ProductRequestDTO createInvalidProductRequestDTOWithBlankName() {
        return new ProductRequestDTO("", DEFAULT_DESCRIPTION, DEFAULT_PRICE);
    }

    public static ProductRequestDTO createInvalidProductRequestDTOWithNullPrice() {
        return new ProductRequestDTO(DEFAULT_NAME, DEFAULT_DESCRIPTION, null);
    }

    public static ProductRequestDTO createInvalidProductRequestDTOWithNegativePrice() {
        return new ProductRequestDTO(DEFAULT_NAME, DEFAULT_DESCRIPTION, BigDecimal.valueOf(-10.00));
    }

    public static ProductRequestDTO createInvalidProductRequestDTOWithZeroPrice() {
        return new ProductRequestDTO(DEFAULT_NAME, DEFAULT_DESCRIPTION, BigDecimal.ZERO);
    }

    public static ProductResponseDTO createProductResponseDTO() {
        return new ProductResponseDTO(DEFAULT_ID, DEFAULT_NAME, DEFAULT_DESCRIPTION, DEFAULT_PRICE);
    }

    public static ProductResponseDTO createCustomProductResponseDTO(String id, String name, String description, BigDecimal price) {
        return new ProductResponseDTO(id, name, description, price);
    }

    public static List<Product> createProductList() {
        return List.of(
                createProduct(),
                createCustomProduct("65f1a2b3c4d5e6f7a8b9c0d2", "Wireless Earbuds", "Noise-cancelling earbuds", BigDecimal.valueOf(199.99)),
                createCustomProduct("65f1a2b3c4d5e6f7a8b9c0d3", "Smart Watch", "Fitness tracking smartwatch", BigDecimal.valueOf(299.99))
        );
    }

    public static List<ProductResponseDTO> createProductResponseDTOList() {
        return List.of(
                createProductResponseDTO(),
                createCustomProductResponseDTO("65f1a2b3c4d5e6f7a8b9c0d2", "Wireless Earbuds", "Noise-cancelling earbuds", BigDecimal.valueOf(199.99)),
                createCustomProductResponseDTO("65f1a2b3c4d5e6f7a8b9c0d3", "Smart Watch", "Fitness tracking smartwatch", BigDecimal.valueOf(299.99))
        );
    }
}
