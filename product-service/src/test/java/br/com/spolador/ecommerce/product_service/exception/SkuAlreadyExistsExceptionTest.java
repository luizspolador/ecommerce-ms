package br.com.spolador.ecommerce.product_service.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for SkuAlreadyExistsException")
class SkuAlreadyExistsExceptionTest {

    @Test
    @DisplayName("Should construct exception with formatted message and getters")
    void shouldConstructExceptionCorrectly() {
        SkuAlreadyExistsException exception = new SkuAlreadyExistsException("PROD-XYZ-001");

        assertThat(exception.getSku()).isEqualTo("PROD-XYZ-001");
        assertThat(exception.getMessage()).isEqualTo("The product with SKU 'PROD-XYZ-001' already exists");
    }
}
