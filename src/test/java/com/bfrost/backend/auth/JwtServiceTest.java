package com.bfrost.backend.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-needs-32-bytes-minimum-length!!";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000);
    }

    @Test
    void generatesTokenThatValidatesAndRoundTripsUserId() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "alice");

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtService.validateAndParse(token).get("username", String.class)).isEqualTo("alice");
    }

    @Test
    void rejectsTokenSignedWithADifferentKey() {
        UUID userId = UUID.randomUUID();
        JwtService otherService = new JwtService("a-completely-different-secret-key-32-bytes!", 60_000);
        String token = otherService.generateAccessToken(userId, "alice");

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService shortLived = new JwtService(SECRET, -1_000);
        String token = shortLived.generateAccessToken(UUID.randomUUID(), "alice");

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void rejectsGarbageToken() {
        assertThat(jwtService.isValid("not-a-valid-jwt")).isFalse();
    }
}
