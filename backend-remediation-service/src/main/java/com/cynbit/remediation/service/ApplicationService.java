package com.cynbit.remediation.service;

import com.cynbit.remediation.dto.request.CareerApplicationRequest;
import com.cynbit.remediation.dto.request.DocumentLogRequest;
import com.cynbit.remediation.dto.request.InquiryRequest;
import com.cynbit.remediation.dto.response.ApplicationStatusResponse;
import com.cynbit.remediation.dto.response.DocumentResponse;
import com.cynbit.remediation.exception.DuplicateApplicationException;
import com.cynbit.remediation.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ApplicationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final DocumentReferenceService documentReferenceService;

    private final Map<String, InquiryRecord> inquiryStore = new ConcurrentHashMap<>();
    private final Map<String, CandidateApplicationRecord> applicationStore = new ConcurrentHashMap<>();
    private final Map<String, String> emailJobIndex = new ConcurrentHashMap<>();
    private final Map<String, DocumentResponse> documentStore = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1000);

    public ApplicationService(DocumentReferenceService documentReferenceService) {
        this.documentReferenceService = documentReferenceService;
    }

    private String generateSecureTrackingToken() {
        byte[] entropy = new byte[18]; // 144 bits of cryptographic entropy
        SECURE_RANDOM.nextBytes(entropy);
        return "trk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
    }

    public String recordInquiry(InquiryRequest request) {
        String inquiryId = "inq_" + System.currentTimeMillis() + "_" + idCounter.incrementAndGet();
        inquiryStore.put(inquiryId, new InquiryRecord(inquiryId, request.getFullName(), request.getEmail(), request.getCompany(), request.getIndustry(), request.getMessage(), Instant.now()));
        return inquiryId;
    }

    /**
     * Submits a candidate job application with atomic deduplication.
     *
     * NOTE: ConcurrentHashMap.putIfAbsent provides atomic thread-safe deduplication for this
     * single-node in-memory prototype. In distributed production environments, database-level
     * unique constraints (e.g. UNIQUE KEY uq_candidate_job (email, job_id)) and ACID transactions
     * are strictly required to enforce uniqueness across multiple application instances.
     *
     * Returns strictly minimum status data; candidate email and personal information are NOT exposed.
     */
    public ApplicationStatusResponse submitCareerApplication(String jobId, CareerApplicationRequest request) {
        String duplicateKey = request.getEmail().toLowerCase().trim() + ":" + jobId.toLowerCase().trim();

        String trackingToken = generateSecureTrackingToken();
        String trackingUrl = "https://portal.cynbit.in/status/" + trackingToken;

        CandidateApplicationRecord newRecord = new CandidateApplicationRecord(
                trackingToken,
                jobId,
                request.getFullName(),
                request.getEmail(),
                "SUBMITTED",
                Instant.now(),
                trackingUrl
        );

        // Atomic deduplication using putIfAbsent
        String existingToken = emailJobIndex.putIfAbsent(duplicateKey, trackingToken);
        if (existingToken != null) {
            String existingTrackingUrl = "https://portal.cynbit.in/status/" + existingToken;
            throw new DuplicateApplicationException(
                    "You have already submitted an active application for this position. Your profile is currently under review.",
                    existingTrackingUrl
            );
        }

        // Successfully claimed unique key; save application record
        applicationStore.put(trackingToken, newRecord);

        // Return strictly minimum status data (no PII exposure)
        return new ApplicationStatusResponse(
                trackingToken,
                jobId,
                trackingUrl,
                newRecord.getStatus(),
                newRecord.getSubmittedAt()
        );
    }

    /**
     * Retrieves application status using the high-entropy tracking token.
     * PRIVACY SAFEGUARD: Exposes only minimum lifecycle status data (status, jobId, timestamp).
     * Candidate name, email, phone number, and address are strictly withheld.
     */
    public ApplicationStatusResponse getApplicationStatus(String jobId, String trackingToken) {
        CandidateApplicationRecord record = applicationStore.get(trackingToken);
        if (record == null || !record.getJobId().equalsIgnoreCase(jobId)) {
            throw new ResourceNotFoundException("Application record not found for tracking token: " + trackingToken);
        }
        return new ApplicationStatusResponse(
                record.getTrackingToken(),
                record.getJobId(),
                record.getTrackingUrl(),
                record.getStatus(),
                record.getSubmittedAt()
        );
    }

    /**
     * Logs an administrative HR document with atomic collision-resistant reservation.
     *
     * NOTE: ConcurrentHashMap.putIfAbsent provides atomic collision handling for this in-memory prototype.
     * In distributed production environments, a database unique constraint (UNIQUE KEY (reference_number))
     * backed by an ACID transaction ensures multi-node uniqueness.
     */
    public DocumentResponse logDocument(DocumentLogRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("joiningDate", request.getJoiningDate());
        details.put("salary", request.getSalary());
        details.put("duration", request.getDuration());
        details.put("reportingTo", request.getReportingTo());
        details.put("validUntil", request.getValidUntil());

        int attempts = 0;
        int maxRetries = 10;
        while (attempts < maxRetries) {
            String candidateRef = documentReferenceService.generateReferenceNumber(
                    request.getDocumentType(),
                    request.getEmploymentType()
            );

            DocumentResponse doc = new DocumentResponse(
                    candidateRef,
                    request.getDocumentType(),
                    request.getCandidateName(),
                    request.getEmploymentType(),
                    "ACTIVE",
                    Instant.now(),
                    details
            );

            // Atomic reservation: insert only if candidateRef is not already present
            DocumentResponse existing = documentStore.putIfAbsent(candidateRef, doc);
            if (existing == null) {
                return doc;
            }
            // Collision occurred; retry with newly generated reference
            attempts++;
        }

        throw new IllegalStateException("Failed to generate a unique document reference after " + maxRetries + " collision retries. Please retry.");
    }

    public DocumentResponse getDocumentByRef(String referenceNumber) {
        DocumentResponse doc = documentStore.get(referenceNumber);
        if (doc == null) {
            throw new ResourceNotFoundException("Document not found with reference: " + referenceNumber);
        }
        return doc;
    }

    /**
     * Returns synthetic demonstration records for intern roster.
     * PROTOTYPE NOTICE: Purely synthetic demonstration data for API shape evaluation;
     * contains no actual person or employee information.
     */
    public List<Map<String, Object>> getRegisteredInterns() {
        return List.of(
                Map.of(
                        "id", "demo_int_01",
                        "name", "Synthetic Intern Alpha",
                        "track", "Backend Engineering",
                        "status", "ACTIVE",
                        "eodCount", 18,
                        "isSyntheticDemonstration", true
                ),
                Map.of(
                        "id", "demo_int_02",
                        "name", "Synthetic Intern Beta",
                        "track", "Autonomous Systems",
                        "status", "ACTIVE",
                        "eodCount", 22,
                        "isSyntheticDemonstration", true
                )
        );
    }

    /**
     * Returns synthetic demonstration records for daily deliverables.
     * PROTOTYPE NOTICE: Purely synthetic demonstration data for API shape evaluation;
     * contains no actual person or employee information.
     */
    public List<Map<String, Object>> getDailySubmissions() {
        return List.of(
                Map.of(
                        "submissionId", "demo_sub_101",
                        "internName", "Synthetic Intern Alpha",
                        "date", "2026-10-31",
                        "deliverable", "Spring Boot Filter Chain Implementation",
                        "status", "APPROVED",
                        "isSyntheticDemonstration", true
                ),
                Map.of(
                        "submissionId", "demo_sub_102",
                        "internName", "Synthetic Intern Beta",
                        "date", "2026-10-31",
                        "deliverable", "API Rate Limiting Prototype",
                        "status", "PENDING_REVIEW",
                        "isSyntheticDemonstration", true
                )
        );
    }

    public static class CandidateApplicationRecord {
        private final String trackingToken;
        private final String jobId;
        private final String fullName;
        private final String email;
        private final String status;
        private final Instant submittedAt;
        private final String trackingUrl;

        public CandidateApplicationRecord(String trackingToken, String jobId, String fullName, String email, String status, Instant submittedAt, String trackingUrl) {
            this.trackingToken = trackingToken;
            this.jobId = jobId;
            this.fullName = fullName;
            this.email = email;
            this.status = status;
            this.submittedAt = submittedAt;
            this.trackingUrl = trackingUrl;
        }

        public String getTrackingToken() { return trackingToken; }
        public String getJobId() { return jobId; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getStatus() { return status; }
        public Instant getSubmittedAt() { return submittedAt; }
        public String getTrackingUrl() { return trackingUrl; }
    }

    public static class InquiryRecord {
        private final String id;
        private final String fullName;
        private final String email;
        private final String company;
        private final String industry;
        private final String message;
        private final Instant timestamp;

        public InquiryRecord(String id, String fullName, String email, String company, String industry, String message, Instant timestamp) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.company = company;
            this.industry = industry;
            this.message = message;
            this.timestamp = timestamp;
        }

        public String getId() { return id; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getCompany() { return company; }
        public String getIndustry() { return industry; }
        public String getMessage() { return message; }
        public Instant getTimestamp() { return timestamp; }
    }
}
