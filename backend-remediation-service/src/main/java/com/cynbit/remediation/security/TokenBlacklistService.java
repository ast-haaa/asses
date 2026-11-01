package com.cynbit.remediation.security;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklistToken(String token, long expirationEpochMs) {
        if (token != null && !token.isBlank()) {
            blacklistedTokens.put(token, expirationEpochMs);
        }
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Long expiration = blacklistedTokens.get(token);
        if (expiration == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiration) {
            blacklistedTokens.remove(token);
            return false;
        }
        return true;
    }
}

