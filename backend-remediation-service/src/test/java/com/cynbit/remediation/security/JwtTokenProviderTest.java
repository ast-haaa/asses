package com.cynbit.remediation.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private TokenBlacklistService tokenBlacklistService;
    private ObjectMapper objectMapper;
    private final String secret = "test-secret-key-with-sufficient-entropy-for-hmac-sha256-testing-enterprise-1234567890";
    private final long expirationMs = 3600000; // 1 hour
    private final String issuer = "cynbit.in";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        tokenBlacklistService = new TokenBlacklistService();
        jwtTokenProvider = new JwtTokenProvider(objectMapper, tokenBlacklistService, secret, expirationMs, issuer);
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
        assertEquals("cynbit.in", jwtTokenProvider.extractIssuer(token));
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
    @DisplayName("Should reject expired tokens")
    void shouldRejectExpiredToken() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(objectMapper, tokenBlacklistService, secret, -1000L, issuer);
        SecurityUser user = new SecurityUser("usr_300", "expired@cynbit.in", "dummy", UserRole.ROLE_HR_ADMIN);

        String expiredToken = expiredProvider.generateToken(user);
        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }

    @Test
    @DisplayName("Should reject tokens with incorrect algorithm (alg != HS256)")
    void shouldRejectIncorrectAlgorithm() throws Exception {
        // 1. None algorithm attack
        Map<String, Object> noneHeader = Map.of("alg", "none", "typ", "JWT");
        Map<String, Object> payload = createValidPayloadMap();
        String tokenWithNoneAlg = buildCustomToken(noneHeader, payload, secret);
        assertFalse(jwtTokenProvider.validateToken(tokenWithNoneAlg), "Tokens with alg=none must be rejected");

        // 2. RS256 algorithm substitution
        Map<String, Object> rsaHeader = Map.of("alg", "RS256", "typ", "JWT");
        String tokenWithRs256 = buildCustomToken(rsaHeader, payload, secret);
        assertFalse(jwtTokenProvider.validateToken(tokenWithRs256), "Tokens with alg=RS256 must be rejected");
    }

    @Test
    @DisplayName("Should reject tokens missing required claims: sub, userId, role, or exp")
    void shouldRejectMissingClaims() throws Exception {
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");

        // Missing subject (sub)
        Map<String, Object> noSubPayload = createValidPayloadMap();
        noSubPayload.remove("sub");
        assertFalse(jwtTokenProvider.validateToken(buildCustomToken(header, noSubPayload, secret)));

        // Missing userId
        Map<String, Object> noUserIdPayload = createValidPayloadMap();
        noUserIdPayload.remove("userId");
        assertFalse(jwtTokenProvider.validateToken(buildCustomToken(header, noUserIdPayload, secret)));

        // Missing role
        Map<String, Object> noRolePayload = createValidPayloadMap();
        noRolePayload.remove("role");
        assertFalse(jwtTokenProvider.validateToken(buildCustomToken(header, noRolePayload, secret)));

        // Invalid role enum
        Map<String, Object> invalidRolePayload = createValidPayloadMap();
        invalidRolePayload.put("role", "ROLE_UNAUTHORIZED_HACKER");
        assertFalse(jwtTokenProvider.validateToken(buildCustomToken(header, invalidRolePayload, secret)));

        // Missing exp
        Map<String, Object> noExpPayload = createValidPayloadMap();
        noExpPayload.remove("exp");
        assertFalse(jwtTokenProvider.validateToken(buildCustomToken(header, noExpPayload, secret)));
    }

    @Test
    @DisplayName("Should reject tokens with missing or incorrect issuer")
    void shouldRejectInvalidIssuer() throws Exception {
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");

        // Missing issuer (iss)
        Map<String, Object> noIssPayload = createValidPayloadMap();
        noIssPayload.remove("iss");
        assertFalse(jwtTokenProvider.validateToken(buildCustomToken(header, noIssPayload, secret)));

        // Fraudulent issuer
        Map<String, Object> fakeIssPayload = createValidPayloadMap();
        fakeIssPayload.put("iss", "evil-corporation.com");
        assertFalse(jwtTokenProvider.validateToken(buildCustomToken(header, fakeIssPayload, secret)));
    }

    @Test
    @DisplayName("Should reject blacklisted tokens")
    void shouldRejectBlacklistedToken() {
        SecurityUser user = new SecurityUser("usr_400", "user@cynbit.in", "dummy", UserRole.ROLE_SUPER_ADMIN);
        String token = jwtTokenProvider.generateToken(user);

        assertTrue(jwtTokenProvider.validateToken(token));

        tokenBlacklistService.blacklistToken(token, System.currentTimeMillis() + 60000);
        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should enforce minimum 256 bits (32 bytes) entropy on JWT secret")
    void shouldEnforceSecretLengthValidation() {
        // Less than 32 bytes (256 bits)
        String shortSecret = "too-short-secret-123";
        assertThrows(IllegalArgumentException.class, () ->
                new JwtTokenProvider(objectMapper, tokenBlacklistService, shortSecret, expirationMs, issuer)
        );

        // Null or blank secret
        assertThrows(IllegalArgumentException.class, () ->
                new JwtTokenProvider(objectMapper, tokenBlacklistService, "", expirationMs, issuer)
        );
        assertThrows(IllegalArgumentException.class, () ->
                new JwtTokenProvider(objectMapper, tokenBlacklistService, null, expirationMs, issuer)
        );
    }

    @Test
    @DisplayName("Should reject malformed tokens")
    void shouldRejectMalformedTokens() {
        assertFalse(jwtTokenProvider.validateToken("not-a-valid-token"));
        assertFalse(jwtTokenProvider.validateToken("a.b"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
        assertFalse(jwtTokenProvider.validateToken("header.payload.signature.extra"));
    }

    private Map<String, Object> createValidPayloadMap() {
        Map<String, Object> payload = new HashMap<>();
        long now = System.currentTimeMillis();
        payload.put("sub", "valid.user@cynbit.in");
        payload.put("userId", "usr_999");
        payload.put("role", "ROLE_HR_ADMIN");
        payload.put("iss", "cynbit.in");
        payload.put("iat", now / 1000);
        payload.put("exp", (now + 3600000) / 1000);
        return payload;
    }

    private String buildCustomToken(Map<String, Object> header, Map<String, Object> payload, String secretKey) throws Exception {
        String encHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(header));
        String encPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(payload));
        String contentToSign = encHeader + "." + encPayload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] sigBytes = mac.doFinal(contentToSign.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(sigBytes);

        return contentToSign + "." + signature;
    }
}
