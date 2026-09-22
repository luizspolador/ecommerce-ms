package br.com.spolador.ecommerce.api_gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Unit tests for SecurityConfig in api-gateway")
class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Nested
    @DisplayName("reactiveJwtAuthenticationConverterAdapter()")
    class JwtConverterTests {

        @Test
        @DisplayName("Should extract and prefix roles with ROLE_ when realm_access has roles")
        void whenRolesPresent_shouldConvertWithRolePrefix() {
            ReactiveJwtAuthenticationConverterAdapter adapter =
                    ReflectionTestUtils.invokeMethod(securityConfig, "reactiveJwtAuthenticationConverterAdapter");
            assertThat(adapter).isNotNull();

            Jwt jwt = createJwt(Map.of("realm_access", Map.of("roles", List.of("ADMIN", "USER"))));

            AbstractAuthenticationToken token = adapter.convert(jwt).block();

            assertThat(token).isNotNull();
            List<String> roles = token.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .toList();
            assertThat(roles).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        }

        @Test
        @DisplayName("Should return empty ROLE_ authorities when realm_access is null")
        void whenRealmAccessNull_shouldReturnEmptyAuthorities() {
            ReactiveJwtAuthenticationConverterAdapter adapter =
                    ReflectionTestUtils.invokeMethod(securityConfig, "reactiveJwtAuthenticationConverterAdapter");
            assertThat(adapter).isNotNull();

            Jwt jwt = createJwt(Map.of("sub", "user-123"));

            AbstractAuthenticationToken token = adapter.convert(jwt).block();

            assertThat(token).isNotNull();
            List<String> roles = token.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .toList();
            assertThat(roles).isEmpty();
        }

        @Test
        @DisplayName("Should return empty ROLE_ authorities when realm_access is empty")
        void whenRealmAccessEmpty_shouldReturnEmptyAuthorities() {
            ReactiveJwtAuthenticationConverterAdapter adapter =
                    ReflectionTestUtils.invokeMethod(securityConfig, "reactiveJwtAuthenticationConverterAdapter");
            assertThat(adapter).isNotNull();

            Jwt jwt = createJwt(Map.of("realm_access", Map.of()));

            AbstractAuthenticationToken token = adapter.convert(jwt).block();

            assertThat(token).isNotNull();
            List<String> roles = token.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .toList();
            assertThat(roles).isEmpty();
        }
    }

    @Nested
    @DisplayName("securityWebFilterChain()")
    class SecurityFilterChainTests {

        @Test
        @DisplayName("Should build SecurityWebFilterChain with ServerHttpSecurity")
        void shouldBuildSecurityWebFilterChain() {
            ServerHttpSecurity serverHttpSecurity = ServerHttpSecurity.http();
            ReactiveJwtDecoder jwtDecoder = mock(ReactiveJwtDecoder.class);
            serverHttpSecurity.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder)));

            SecurityWebFilterChain filterChain = securityConfig.securityWebFilterChain(serverHttpSecurity);

            assertThat(filterChain).isNotNull();
        }
    }

    private Jwt createJwt(Map<String, Object> claims) {
        Map<String, Object> finalClaims = new HashMap<>(claims);
        finalClaims.putIfAbsent("sub", "user-test");
        return new Jwt(
                "dummy-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                finalClaims
        );
    }
}
