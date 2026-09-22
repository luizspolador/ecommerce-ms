package br.com.spolador.ecommerce.api_gateway;

import br.com.spolador.ecommerce.api_gateway.config.InstallOpenTelemetryAppender;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringBootTest
@DisplayName("Integration tests for API Gateway Security and Routing")
class SecurityAndRoutingIntegrationTest {

    @Autowired
    private ApplicationContext context;

    private WebTestClient webTestClient;

    @MockitoBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @MockitoBean
    private InstallOpenTelemetryAppender installOpenTelemetryAppender;

    @MockitoBean
    private OpenTelemetry openTelemetry;

    @MockitoBean
    private Tracer tracer;

    @BeforeEach
    void setUp() {
        Tracer noopTracer = OpenTelemetry.noop().getTracer("test");
        when(tracer.spanBuilder(anyString())).thenAnswer(inv -> noopTracer.spanBuilder(inv.getArgument(0)));
        when(openTelemetry.getTracer(anyString())).thenReturn(noopTracer);

        webTestClient = WebTestClient.bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    @Nested
    @DisplayName("Public endpoints (permitAll)")
    class PublicEndpointsTests {

        @Test
        @DisplayName("GET /eureka/** should be permitted without token")
        void getEureka_shouldBePermitted() {
            webTestClient.get()
                    .uri("/eureka/apps")
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
        }

        @Test
        @DisplayName("GET /api/v1/product/** should be permitted without token")
        void getProduct_shouldBePermitted() {
            webTestClient.get()
                    .uri("/api/v1/product/123")
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
        }

        @Test
        @DisplayName("GET /api/v1/inventory/** should be permitted without token")
        void getInventory_shouldBePermitted() {
            webTestClient.get()
                    .uri("/api/v1/inventory/SKU-1?quantity=1")
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
        }
    }

    @Nested
    @DisplayName("Protected endpoints without authentication")
    class UnauthenticatedProtectedEndpointsTests {

        @Test
        @DisplayName("POST /api/v1/product without token should return 401 Unauthorized")
        void postProductWithoutToken_shouldReturn401() {
            webTestClient.post()
                    .uri("/api/v1/product")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("POST /api/v1/inventory without token should return 401 Unauthorized")
        void postInventoryWithoutToken_shouldReturn401() {
            webTestClient.post()
                    .uri("/api/v1/inventory")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("POST /api/v1/order without token should return 401 Unauthorized")
        void postOrderWithoutToken_shouldReturn401() {
            webTestClient.post()
                    .uri("/api/v1/order")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("DELETE /api/v1/order/1 without token should return 401 Unauthorized")
        void deleteOrderWithoutToken_shouldReturn401() {
            webTestClient.delete()
                    .uri("/api/v1/order/1")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    @Nested
    @DisplayName("Role-based authorization")
    class RoleBasedAuthorizationTests {

        @Test
        @DisplayName("POST /api/v1/product with ROLE_USER should return 403 Forbidden")
        void postProductWithUserRole_shouldReturn403() {
            webTestClient.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                    .post()
                    .uri("/api/v1/product")
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("POST /api/v1/product with ROLE_ADMIN should pass security")
        void postProductWithAdminRole_shouldPassSecurity() {
            webTestClient.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .post()
                    .uri("/api/v1/product")
                    .exchange()
                    .expectStatus().value(status -> {
                        assertThat(status).isNotEqualTo(HttpStatus.UNAUTHORIZED.value());
                        assertThat(status).isNotEqualTo(HttpStatus.FORBIDDEN.value());
                    });
        }

        @Test
        @DisplayName("POST /api/v1/order with ROLE_USER should pass security")
        void postOrderWithUserRole_shouldPassSecurity() {
            webTestClient.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                    .post()
                    .uri("/api/v1/order")
                    .exchange()
                    .expectStatus().value(status -> {
                        assertThat(status).isNotEqualTo(HttpStatus.UNAUTHORIZED.value());
                        assertThat(status).isNotEqualTo(HttpStatus.FORBIDDEN.value());
                    });
        }

        @Test
        @DisplayName("POST /api/v1/order with only ROLE_ADMIN should return 403 Forbidden")
        void postOrderWithOnlyAdminRole_shouldReturn403() {
            webTestClient.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .post()
                    .uri("/api/v1/order")
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("DELETE /api/v1/order/1 with ROLE_USER should return 403 Forbidden")
        void deleteOrderWithUserRole_shouldReturn403() {
            webTestClient.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                    .delete()
                    .uri("/api/v1/order/1")
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("DELETE /api/v1/order/1 with ROLE_ADMIN should pass security")
        void deleteOrderWithAdminRole_shouldPassSecurity() {
            webTestClient.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .delete()
                    .uri("/api/v1/order/1")
                    .exchange()
                    .expectStatus().value(status -> {
                        assertThat(status).isNotEqualTo(HttpStatus.UNAUTHORIZED.value());
                        assertThat(status).isNotEqualTo(HttpStatus.FORBIDDEN.value());
                    });
        }

        @Test
        @DisplayName("GET /api/v1/order with ROLE_USER should pass security")
        void getOrderWithUserRole_shouldPassSecurity() {
            webTestClient.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                    .get()
                    .uri("/api/v1/order")
                    .exchange()
                    .expectStatus().value(status -> {
                        assertThat(status).isNotEqualTo(HttpStatus.UNAUTHORIZED.value());
                        assertThat(status).isNotEqualTo(HttpStatus.FORBIDDEN.value());
                    });
        }
    }
}
