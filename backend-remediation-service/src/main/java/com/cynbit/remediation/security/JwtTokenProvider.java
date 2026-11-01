package com.cynbit.remediation.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    public static final String EXPECTED_ALGORITHM = "HS256";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int MIN_SECRET_LENGTH_BYTES = 32; // 256 bits minimum for HS256

    private final ObjectMapper objectMapper;
    private final TokenBlacklistService tokenBlacklistService;
    private final byte[] secretKeyBytes;
    private final long expirationMs;
    private final String expectedIssuer;

    public JwtTokenProvider(
            ObjectMapper objectMapper,
            TokenBlacklistService tokenBlacklistService,
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-ms:28800000}") long expirationMs,
            @Value("${app.security.jwt.issuer:cynbit.in}") String expectedIssuer) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT_SECRET environment variable is required and must not be empty");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "JWT_SECRET must have at least 256 bits (32 bytes) of entropy for HS256 security. Current length: " + keyBytes.length + " bytes.");
        }

        this.objectMapper = objectMapper;
        this.tokenBlacklistService = tokenBlacklistService;
        this.secretKeyBytes = keyBytes;
        this.expirationMs = expirationMs;
        this.expectedIssuer = (expectedIssuer != null && !expectedIssuer.isBlank()) ? expectedIssuer : "cynbit.in";
    }

    public String generateToken(SecurityUser user) {
        long now = System.currentTimeMillis();
        long exp = now + expirationMs;

        try {
            // 1. Header with explicit HS256 algorithm
            Map<String, Object> header = new HashMap<>();
            header.put("alg", EXPECTED_ALGORITHM);
            header.put("typ", "JWT");
            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsBytes(header));

            // 2. Payload with mandatory claims: sub, userId, role, iss, iat, exp, jti
            Map<String, Object> payload = new HashMap<>();
            payload.put("jti", java.util.UUID.randomUUID().toString());
            payload.put("sub", user.getUsername());
            payload.put("userId", user.getUserId());
            payload.put("role", user.getRole().name());
            payload.put("iss", expectedIssuer);
            payload.put("iat", now / 1000);
            payload.put("exp", exp / 1000);
            String encodedPayload = base64UrlEncode(objectMapper.writeValueAsBytes(payload));

            // 3. Signature
            String contentToSign = encodedHeader + "." + encodedPayload;
            String signature = sign(contentToSign);

            return contentToSign + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate JWT token", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                return false;
            }
            if (tokenBlacklistService.isBlacklisted(token)) {
                return false;
            }
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            // 1. Explicitly validate Header & Algorithm (strictly HS256)
            Map<String, Object> header = parseJsonMap(parts[0]);
            String alg = (String) header.get("alg");
            if (!EXPECTED_ALGORITHM.equals(alg)) {
                return false;
            }

            // 2. Validate HMAC-SHA256 Signature using constant-time comparison
            String contentToSign = parts[0] + "." + parts[1];
            String expectedSignature = sign(contentToSign);
            if (!MessageDigestEquals(expectedSignature, parts[2])) {
                return false;
            }

            // 3. Validate Payload and Mandatory Claims
            Map<String, Object> claims = parseJsonMap(parts[1]);

            // Expiration validation
            Number expNumber = (Number) claims.get("exp");
            if (expNumber == null) {
                return false;
            }
            long expTime = expNumber.longValue() * 1000;
            if (System.currentTimeMillis() >= expTime) {
                return false;
            }

            // Issuer validation
            String iss = (String) claims.get("iss");
            if (iss == null || !expectedIssuer.equals(iss)) {
                return false;
            }

            // Subject (Username) validation
            String sub = (String) claims.get("sub");
            if (sub == null || sub.isBlank()) {
                return false;
            }

            // UserId validation
            String userId = (String) claims.get("userId");
            if (userId == null || userId.isBlank()) {
                return false;
            }

            // Role validation against UserRole enum
            String role = (String) claims.get("role");
            if (role == null || role.isBlank()) {
                return false;
            }
            try {
                UserRole.valueOf(role);
            } catch (IllegalArgumentException ex) {
                return false;
            }

            return true;
        } catch (Exception e) {
            // Any parsing failure, base64 decode failure, JSON error, or null reference must return false
            return false;
        }
    }

    public long getExpirationEpochMs(String token) {
        Map<String, Object> claims = extractAllClaims(token);
        Number expNumber = (Number) claims.get("exp");
        return expNumber != null ? expNumber.longValue() * 1000 : System.currentTimeMillis();
    }

    public String extractUsername(String token) {
        return (String) extractAllClaims(token).get("sub");
    }

    public String extractRole(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    public String extractUserId(String token) {
        return (String) extractAllClaims(token).get("userId");
    }

    public String extractIssuer(String token) {
        return (String) extractAllClaims(token).get("iss");
    }

    public Map<String, Object> extractAllClaims(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        return parseJsonMap(parts[1]);
    }

    private Map<String, Object> parseJsonMap(String encodedString) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(encodedString);
            return objectMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode or parse JWT segment", e);
        }
    }

    private String sign(String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, HMAC_SHA256);
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(hmacBytes);
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean MessageDigestEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(aBytes, bBytes);
    }
}
