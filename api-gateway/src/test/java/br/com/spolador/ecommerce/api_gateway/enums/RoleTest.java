package br.com.spolador.ecommerce.api_gateway.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for Role enum")
class RoleTest {

    @Test
    @DisplayName("Should contain all expected enum values")
    void testRoleValues() {
        assertThat(Role.values()).containsExactly(
                Role.ADMIN,
                Role.USER
        );
        assertThat(Role.valueOf("ADMIN")).isEqualTo(Role.ADMIN);
        assertThat(Role.valueOf("USER")).isEqualTo(Role.USER);
    }
}
