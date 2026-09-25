package br.com.spolador.ecommerce.order_service.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for ProductNotRegisteredException")
class ProductNotRegisteredExceptionTest {

    @Test
    @DisplayName("Should instantiate exception with correct sku and message")
    void testExceptionCreation() {
        ProductNotRegisteredException exception = new ProductNotRegisteredException("SKU-123");

        assertThat(exception.getSku()).isEqualTo("SKU-123");
        assertThat(exception.getMessage()).contains("SKU 'SKU-123'", "not registered in catalog");
    }
}
