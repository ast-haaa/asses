package com.cynbit.remediation.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "test-secret-key-with-sufficient-entropy-for-hmac-sha256-testing-1234567890";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        jwtTokenProvider = new JwtTokenProvider(objectMapper, secret, expirationMs);
    }

    @Test
    @DisplayName("Should generate valid token and extract user claims")
    void shouldGenerateAndExtractClaims() {
        SecurityUser user = new SecurityUser("usr_100", "hr.admin@cynbit.in", "dummy", UserRole.ROLE_HR_ADMIN);

        String token = jwtTokenProvider.generateToken(user);
        assertNotNull(token);
        assertTrue(token.contains("."));

        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("hr.admin@cynbit.in", jwtTokenProvider.extractUsername(token));
        assertEquals("ROLE_HR_ADMIN", jwtTokenProvider.extractRole(token));
        assertEquals("usr_100", jwtTokenProvider.extractUserId(token));
    }

    @Test
    @DisplayName("Should reject tampered token signatures")
    void shouldRejectTamperedToken() {
        SecurityUser user = new SecurityUser("usr_200", "mentor@cynbit.in", "dummy", UserRole.ROLE_MENTOR);
        String token = jwtTokenProvider.generateToken(user);

        // Tamper with payload
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + "X." + parts[2];

        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("Should reject malformed tokens")
    void shouldRejectMalformedTokens() {
        assertFalse(jwtTokenProvider.validateToken("not-a-valid-token"));
        assertFalse(jwtTokenProvider.validateToken("a.b"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }
}
