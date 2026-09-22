package br.com.spolador.ecommerce.product_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for OpenApiConfig in product-service")
class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    @DisplayName("Should create OpenAPI bean with title, version and contact info")
    void testProductOpenAPI() {
        OpenAPI openAPI = openApiConfig.productOpenAPI();

        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Product Service API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1.0.0");
        assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("Luiz Spolador");
        assertThat(openAPI.getInfo().getLicense().getName()).isEqualTo("Apache 2.0");
    }
}
