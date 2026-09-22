package br.com.spolador.ecommerce.product_service.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for Product Model entity")
class ProductModelTest {

    @Test
    @DisplayName("Should construct Product using Builder, AllArgsConstructor and NoArgsConstructor")
    void testProductConstructorsAndBuilder() {
        // Builder
        Product productFromBuilder = Product.builder()
                .id("p1")
                .sku("SKU-LAPTOP-01")
                .name("Laptop")
                .description("Gaming laptop")
                .price(BigDecimal.valueOf(2500.00))
                .build();

        assertThat(productFromBuilder.getId()).isEqualTo("p1");
        assertThat(productFromBuilder.getSku()).isEqualTo("SKU-LAPTOP-01");
        assertThat(productFromBuilder.getName()).isEqualTo("Laptop");
        assertThat(productFromBuilder.getDescription()).isEqualTo("Gaming laptop");
        assertThat(productFromBuilder.getPrice()).isEqualTo(BigDecimal.valueOf(2500.00));

        // AllArgsConstructor
        Product productFromAllArgs = new Product("p1", "SKU-LAPTOP-01", "Laptop", "Gaming laptop", BigDecimal.valueOf(2500.00));
        assertThat(productFromAllArgs).isEqualTo(productFromBuilder);

        // NoArgsConstructor & Setters
        Product productFromNoArgs = new Product();
        productFromNoArgs.setId("p1");
        productFromNoArgs.setSku("SKU-LAPTOP-01");
        productFromNoArgs.setName("Laptop");
        productFromNoArgs.setDescription("Gaming laptop");
        productFromNoArgs.setPrice(BigDecimal.valueOf(2500.00));

        assertThat(productFromNoArgs).isEqualTo(productFromBuilder);
        assertThat(productFromNoArgs.hashCode()).isEqualTo(productFromBuilder.hashCode());
        assertThat(productFromNoArgs.toString()).contains("p1", "SKU-LAPTOP-01", "Laptop");

        // Builder toString
        assertThat(Product.builder().toString()).isNotEmpty();
    }
}
