package com.puzzlezen.auth.service;

import com.puzzlezen.auth.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtils — tests unitaires")
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        // Clé de 256 bits pour les tests
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret",
                "test-secret-key-that-is-at-least-256-bits-long-for-hmac");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpiration", 86400000L);
    }

    @Test
    @DisplayName("generateToken produit un token non vide")
    void shouldGenerateNonEmptyToken() {
        String token = jwtUtils.generateToken("morgan", "PLAYER");
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("getUsernameFromToken extrait le bon username")
    void shouldExtractCorrectUsername() {
        String token = jwtUtils.generateToken("morgan", "PLAYER");
        assertThat(jwtUtils.getUsernameFromToken(token)).isEqualTo("morgan");
    }

    @Test
    @DisplayName("validateToken retourne true pour un token valide")
    void shouldValidateValidToken() {
        String token = jwtUtils.generateToken("morgan", "PLAYER");
        assertThat(jwtUtils.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken retourne false pour un token invalide")
    void shouldRejectInvalidToken() {
        assertThat(jwtUtils.validateToken("this.is.not.a.valid.jwt")).isFalse();
    }

    @Test
    @DisplayName("validateToken retourne false pour un token vide")
    void shouldRejectEmptyToken() {
        assertThat(jwtUtils.validateToken("")).isFalse();
    }
}
