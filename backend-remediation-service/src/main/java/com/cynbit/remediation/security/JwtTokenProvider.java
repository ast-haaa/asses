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

    private static final String HMAC_SHA256 = "HmacSHA256";
    private final ObjectMapper objectMapper;
    private final byte[] secretKeyBytes;
    private final long expirationMs;

    public JwtTokenProvider(
            ObjectMapper objectMapper,
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-ms:28800000}") long expirationMs) {
        this.objectMapper = objectMapper;
        this.secretKeyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMs = expirationMs;
    }

    public String generateToken(SecurityUser user) {
        long now = System.currentTimeMillis();
        long exp = now + expirationMs;

        try {
            // 1. Header
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsBytes(header));

            // 2. Payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", user.getUsername());
            payload.put("userId", user.getUserId());
            payload.put("role", user.getRole().name());
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
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            String contentToSign = parts[0] + "." + parts[1];
            String expectedSignature = sign(contentToSign);
            if (!MessageDigestEquals(expectedSignature, parts[2])) {
                return false;
            }

            Map<String, Object> claims = parseClaims(parts[1]);
            Number expNumber = (Number) claims.get("exp");
            if (expNumber == null) {
                return false;
            }
            long expTime = expNumber.longValue() * 1000;
            if (System.currentTimeMillis() >= expTime) {
                return false;
            }

            String sub = (String) claims.get("sub");
            String role = (String) claims.get("role");
            if (sub == null || sub.isBlank() || role == null || role.isBlank()) {
                return false;
            }
            try {
                UserRole.valueOf(role);
            } catch (IllegalArgumentException ex) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
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

    public Map<String, Object> extractAllClaims(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        return parseClaims(parts[1]);
    }

    private Map<String, Object> parseClaims(String encodedPayload) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(encodedPayload);
            return objectMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JWT payload claims", e);
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

