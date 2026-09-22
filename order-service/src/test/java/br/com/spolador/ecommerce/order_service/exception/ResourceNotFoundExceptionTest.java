package br.com.spolador.ecommerce.order_service.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for ResourceNotFoundException in order-service")
class ResourceNotFoundExceptionTest {

    @Test
    @DisplayName("Should correctly instantiate and retain resourceName, fieldName, fieldValue")
    void testResourceNotFoundExceptionProperties() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Order", "id", 100L);

        assertThat(exception.getResourceName()).isEqualTo("Order");
        assertThat(exception.getFieldName()).isEqualTo("id");
        assertThat(exception.getFieldValue()).isEqualTo(100L);
        assertThat(exception.getMessage()).isEqualTo("Order not found with id: '100'");
    }
}
