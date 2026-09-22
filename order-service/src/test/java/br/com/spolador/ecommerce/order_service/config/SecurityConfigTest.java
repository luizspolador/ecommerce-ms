package br.com.spolador.ecommerce.order_service.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for SecurityConfig")
class SecurityConfigTest {

    @Test
    @DisplayName("Should configure security filter chain")
    void testSecurityFilterChain() throws Exception {
        SecurityConfig config = new SecurityConfig();
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        DefaultSecurityFilterChain chain = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(chain);

        SecurityFilterChain result = config.securityFilterChain(http);
        assertThat(result).isEqualTo(chain);
        verify(http).build();
    }

    @Test
    @DisplayName("Should extract ROLE_ authorities from realm_access roles claim")
    void testJwtAuthenticationConverter_withRoles() {
        SecurityConfig config = new SecurityConfig();
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

        org.springframework.security.oauth2.jwt.Jwt jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("realm_access", java.util.Map.of("roles", java.util.List.of("ADMIN", "USER")))
                .build();

        org.springframework.security.authentication.AbstractAuthenticationToken token = converter.convert(jwt);
        assertThat(token).isNotNull();
        assertThat(token.getAuthorities()).extracting(org.springframework.security.core.GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("Should not extract ROLE_ authorities when realm_access has no roles or is null")
    void testJwtAuthenticationConverter_withoutRoles() {
        SecurityConfig config = new SecurityConfig();
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

        org.springframework.security.oauth2.jwt.Jwt jwtNoRealm = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("sub", "user1")
                .build();

        org.springframework.security.authentication.AbstractAuthenticationToken token = converter.convert(jwtNoRealm);
        assertThat(token).isNotNull();
        assertThat(token.getAuthorities()).extracting(org.springframework.security.core.GrantedAuthority::getAuthority)
                .doesNotContain("ROLE_ADMIN", "ROLE_USER");

        org.springframework.security.oauth2.jwt.Jwt jwtEmptyRoles = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("realm_access", java.util.Collections.emptyMap())
                .build();

        org.springframework.security.authentication.AbstractAuthenticationToken token2 = converter.convert(jwtEmptyRoles);
        assertThat(token2).isNotNull();
        assertThat(token2.getAuthorities()).extracting(org.springframework.security.core.GrantedAuthority::getAuthority)
                .doesNotContain("ROLE_ADMIN", "ROLE_USER");
    }
}
