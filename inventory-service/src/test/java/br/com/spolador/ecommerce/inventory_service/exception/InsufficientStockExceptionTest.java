package br.com.spolador.ecommerce.inventory_service.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for InsufficientStockException")
class InsufficientStockExceptionTest {

    @Test
    @DisplayName("Should construct exception with formatted message and getters")
    void shouldConstructExceptionCorrectly() {
        InsufficientStockException exception = new InsufficientStockException("IPHONE_15_BLACK", 10, 3);

        assertThat(exception.getSku()).isEqualTo("IPHONE_15_BLACK");
        assertThat(exception.getRequestedQuantity()).isEqualTo(10);
        assertThat(exception.getAvailableQuantity()).isEqualTo(3);
        assertThat(exception.getMessage())
                .isEqualTo("Insufficient stock for SKU 'IPHONE_15_BLACK'. Requested: 10, Available: 3");
    }
}
