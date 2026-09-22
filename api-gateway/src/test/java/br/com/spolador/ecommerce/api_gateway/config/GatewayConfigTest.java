package br.com.spolador.ecommerce.api_gateway.config;

import br.com.spolador.ecommerce.api_gateway.config.InstallOpenTelemetryAppender;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Unit and integration tests for GatewayConfig")
class GatewayConfigTest {

    @Autowired
    private RouteLocatorBuilder routeLocatorBuilder;

    @MockitoBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @MockitoBean
    private InstallOpenTelemetryAppender installOpenTelemetryAppender;

    @MockitoBean
    private OpenTelemetry openTelemetry;

    @MockitoBean
    private Tracer tracer;

    @Test
    @DisplayName("Should configure routes for product-service, order-service, and inventory-service")
    void testRouteLocator() {
        GatewayConfig gatewayConfig = new GatewayConfig();
        RouteLocator routeLocator = gatewayConfig.routeLocator(routeLocatorBuilder);

        List<Route> routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes).isNotNull().hasSize(3);

        List<String> routeIds = routes.stream().map(Route::getId).toList();
        assertThat(routeIds).containsExactlyInAnyOrder("product-service", "order-service", "inventory-service");

        Route productRoute = routes.stream().filter(r -> r.getId().equals("product-service")).findFirst().orElseThrow();
        assertThat(productRoute.getUri().toString()).isEqualTo("lb://PRODUCT-SERVICE");

        Route orderRoute = routes.stream().filter(r -> r.getId().equals("order-service")).findFirst().orElseThrow();
        assertThat(orderRoute.getUri().toString()).isEqualTo("lb://ORDER-SERVICE");

        Route inventoryRoute = routes.stream().filter(r -> r.getId().equals("inventory-service")).findFirst().orElseThrow();
        assertThat(inventoryRoute.getUri().toString()).isEqualTo("lb://INVENTORY-SERVICE");
    }
}
