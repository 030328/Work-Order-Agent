package com.wo.common.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    @Test
    void generateTokenShouldExposeExpectedClaims() {
        String token = JwtUtil.generateToken(1001L, "admin", "ADMIN");

        Claims claims = JwtUtil.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("1001");
        assertThat(JwtUtil.getUserId(token)).isEqualTo(1001L);
        assertThat(JwtUtil.getUsername(token)).isEqualTo("admin");
        assertThat(JwtUtil.getRole(token)).isEqualTo("ADMIN");
        assertThat(JwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void expiredTokenShouldBeInvalid() {
        String token = JwtUtil.generateToken(1001L, "admin", "ADMIN", -1000L);

        assertThat(JwtUtil.isTokenValid(token)).isFalse();
    }

    @Test
    void malformedTokenShouldBeInvalid() {
        assertThat(JwtUtil.isTokenValid("not-a-jwt-token")).isFalse();
    }
}
