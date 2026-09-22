package br.com.spolador.ecommerce.order_service.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for ServiceUnavailableException")
class ServiceUnavailableExceptionTest {

    @Test
    @DisplayName("Should construct exception with message")
    void shouldConstructWithMessage() {
        ServiceUnavailableException ex = new ServiceUnavailableException("Service is down");

        assertThat(ex.getMessage()).isEqualTo("Service is down");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("Should construct exception with message and cause")
    void shouldConstructWithMessageAndCause() {
        Throwable cause = new IllegalStateException("Timeout");
        ServiceUnavailableException ex = new ServiceUnavailableException("Service is down", cause);

        assertThat(ex.getMessage()).isEqualTo("Service is down");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
