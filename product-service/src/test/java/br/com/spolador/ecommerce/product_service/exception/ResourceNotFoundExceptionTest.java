package br.com.spolador.ecommerce.product_service.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for ResourceNotFoundException")
class ResourceNotFoundExceptionTest {

    @Test
    @DisplayName("Should correctly instantiate and expose fields and message")
    void shouldCorrectlyInstantiateAndExposeFields() {
        // Arrange & Act
        ResourceNotFoundException exception = new ResourceNotFoundException("Product", "id", "prod-999");

        // Assert
        assertThat(exception.getResourceName()).isEqualTo("Product");
        assertThat(exception.getFieldName()).isEqualTo("id");
        assertThat(exception.getFieldValue()).isEqualTo("prod-999");
        assertThat(exception.getMessage()).isEqualTo("Product not found with id: 'prod-999'");
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
