package com.iris.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-at-least-32-bytes-long-for-hs256";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60, 30);
    }

    @Test
    void accessToken_carriesSubjectEmailAndTypeClaims() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(userId, "user@example.com");
        Claims claims = jwtService.parseAndValidate(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.getExpiration()).isAfter(Instant.now().plus(Duration.ofMinutes(59)));
    }

    @Test
    void refreshToken_carriesSubjectTypeAndUniqueId() {
        UUID userId = UUID.randomUUID();

        String tokenA = jwtService.generateRefreshToken(userId);
        String tokenB = jwtService.generateRefreshToken(userId);

        Claims claimsA = jwtService.parseAndValidate(tokenA);
        Claims claimsB = jwtService.parseAndValidate(tokenB);

        assertThat(claimsA.getSubject()).isEqualTo(userId.toString());
        assertThat(claimsA.get("type", String.class)).isEqualTo("refresh");
        assertThat(claimsA.getId()).isNotBlank();
        assertThat(claimsA.getId()).isNotEqualTo(claimsB.getId());
    }

    @Test
    void refreshToken_expiresAfterConfiguredTtl() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateRefreshToken(userId);
        Claims claims = jwtService.parseAndValidate(token);

        Duration untilExpiry = Duration.between(Instant.now(), claims.getExpiration().toInstant());
        assertThat(untilExpiry.toDays()).isBetween(29L, 30L);
    }

    @Test
    void parseAndValidate_rejectsTokenSignedWithADifferentKey() {
        String foreignToken = io.jsonwebtoken.Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .signWith(Keys.hmacShaKeyFor("a-completely-different-signing-key-32-bytes".getBytes()))
                .compact();

        assertThatThrownBy(() -> jwtService.parseAndValidate(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseAndValidate_rejectsMalformedToken() {
        assertThatThrownBy(() -> jwtService.parseAndValidate("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void getRefreshTokenTtl_reflectsConfiguredDays() {
        assertThat(jwtService.getRefreshTokenTtl()).isEqualTo(Duration.ofDays(30));
    }
}
