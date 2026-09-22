package br.com.spolador.ecommerce.order_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for OpenApiConfig in order-service")
class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    @DisplayName("Should create OpenAPI bean with title, version, contact and security scheme")
    void testOrderOpenAPI() {
        OpenAPI openAPI = openApiConfig.orderOpenAPI();

        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Order Service API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1.0.0");
        assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("Luiz Spolador");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey(OpenApiConfig.SECURITY_SCHEME_NAME);
    }
}
