package com.cynbit.remediation.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Year;
import java.util.HexFormat;

@Service
public class DocumentReferenceService {

    private static final String CROCKFORD_BASE32 = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateReferenceNumber(String documentType, String employmentType) {
        String category = "Internship".equalsIgnoreCase(employmentType) ? "INT" : "OFFER";
        int currentYear = Year.now().getValue();

        StringBuilder randomSuffix = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            int index = SECURE_RANDOM.nextInt(CROCKFORD_BASE32.length());
            randomSuffix.append(CROCKFORD_BASE32.charAt(index));
        }

        return String.format("CB/HR/%s/%d/%s", category, currentYear, randomSuffix.toString());
    }

    public String computeDocumentChecksum(String canonicalPayload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
