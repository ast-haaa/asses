package com.cynbit.remediation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class DocumentReferenceServiceTest {

    private final DocumentReferenceService service = new DocumentReferenceService();

    @Test
    @DisplayName("Should generate reference number matching canonical enterprise pattern")
    void shouldGenerateValidReferenceNumber() {
        int currentYear = Year.now().getValue();
        String offerRef = service.generateReferenceNumber("OFFER_LETTER", "Full-Time");
        String intRef = service.generateReferenceNumber("OFFER_LETTER", "Internship");

        Pattern pattern = Pattern.compile("^CB/HR/(INT|OFFER)/" + currentYear + "/[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{8}$");

        assertNotNull(offerRef);
        assertTrue(pattern.matcher(offerRef).matches(), "Offer ref does not match expected pattern: " + offerRef);
        assertTrue(offerRef.contains("/OFFER/"));

        assertNotNull(intRef);
        assertTrue(pattern.matcher(intRef).matches(), "Internship ref does not match expected pattern: " + intRef);
        assertTrue(intRef.contains("/INT/"));
    }

    @Test
    @DisplayName("Should demonstrate collision resistance across 10,000 generated references")
    void shouldDemonstrateCollisionResistance() {
        int iterations = 10000;
        Set<String> generatedRefs = new HashSet<>(iterations);

        for (int i = 0; i < iterations; i++) {
            String ref = service.generateReferenceNumber("OFFER_LETTER", "Internship");
            boolean isNew = generatedRefs.add(ref);
            assertTrue(isNew, "Collision detected for generated reference: " + ref);
        }

        assertEquals(iterations, generatedRefs.size());
    }

    @Test
    @DisplayName("Should compute consistent SHA-256 document checksums")
    void shouldComputeDeterministicChecksum() {
        String content = "OFFER_LETTER:Alex Rivera:2026-11-01:CB/HR/OFFER/2026/8K2N9P3X";
        String hash1 = service.computeDocumentChecksum(content);
        String hash2 = service.computeDocumentChecksum(content);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);

        String tamperedContent = content + " ";
        String hashTampered = service.computeDocumentChecksum(tamperedContent);
        assertNotEquals(hash1, hashTampered);
    }
}
