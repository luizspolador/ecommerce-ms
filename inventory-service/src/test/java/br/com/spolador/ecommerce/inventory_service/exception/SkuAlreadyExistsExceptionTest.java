package br.com.spolador.ecommerce.inventory_service.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for SkuAlreadyExistsException")
class SkuAlreadyExistsExceptionTest {

    @Test
    @DisplayName("Should construct exception with formatted message and getters")
    void shouldConstructExceptionCorrectly() {
        SkuAlreadyExistsException exception = new SkuAlreadyExistsException("IPHONE_15_BLACK");

        assertThat(exception.getSku()).isEqualTo("IPHONE_15_BLACK");
        assertThat(exception.getMessage()).isEqualTo("The inventory for SKU 'IPHONE_15_BLACK' already exists");
    }
}
