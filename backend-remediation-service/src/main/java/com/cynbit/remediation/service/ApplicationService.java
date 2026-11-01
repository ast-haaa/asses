package com.cynbit.remediation.service;

import com.cynbit.remediation.dto.request.CareerApplicationRequest;
import com.cynbit.remediation.dto.request.DocumentLogRequest;
import com.cynbit.remediation.dto.request.InquiryRequest;
import com.cynbit.remediation.dto.response.ApplicationStatusResponse;
import com.cynbit.remediation.dto.response.DocumentResponse;
import com.cynbit.remediation.exception.DuplicateApplicationException;
import com.cynbit.remediation.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ApplicationService {

    private final DocumentReferenceService documentReferenceService;

    private final Map<String, InquiryRecord> inquiryStore = new ConcurrentHashMap<>();
    private final Map<String, ApplicationStatusResponse> applicationStore = new ConcurrentHashMap<>();
    private final Map<String, String> emailJobIndex = new ConcurrentHashMap<>();
    private final Map<String, DocumentResponse> documentStore = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1000);

    public ApplicationService(DocumentReferenceService documentReferenceService) {
        this.documentReferenceService = documentReferenceService;
    }

    public String recordInquiry(InquiryRequest request) {
        String inquiryId = "inq_" + System.currentTimeMillis() + "_" + idCounter.incrementAndGet();
        inquiryStore.put(inquiryId, new InquiryRecord(inquiryId, request.getFullName(), request.getEmail(), request.getCompany(), request.getIndustry(), request.getMessage(), Instant.now()));
        return inquiryId;
    }

    public ApplicationStatusResponse submitCareerApplication(String jobId, CareerApplicationRequest request) {
        String duplicateKey = request.getEmail().toLowerCase().trim() + ":" + jobId.toLowerCase().trim();
        String existingAppId = emailJobIndex.get(duplicateKey);

        if (existingAppId != null) {
            String trackingUrl = "https://portal.cynbit.in/status/" + existingAppId;
            throw new DuplicateApplicationException(
                    "You have already submitted an active application for this position. Your profile is currently under review.",
                    trackingUrl
            );
        }

        String applicationId = "app_" + System.currentTimeMillis() + "_" + idCounter.incrementAndGet();
        String trackingUrl = "https://portal.cynbit.in/status/" + applicationId;

        ApplicationStatusResponse response = new ApplicationStatusResponse(
                applicationId,
                jobId,
                request.getFullName(),
                request.getEmail(),
                false,
                trackingUrl,
                "SUBMITTED",
                Instant.now()
        );

        applicationStore.put(applicationId, response);
        emailJobIndex.put(duplicateKey, applicationId);

        return response;
    }

    public ApplicationStatusResponse getApplicationStatus(String jobId, String applicationId) {
        ApplicationStatusResponse response = applicationStore.get(applicationId);
        if (response == null || !response.getJobId().equalsIgnoreCase(jobId)) {
            throw new ResourceNotFoundException("Application record not found for ID: " + applicationId);
        }
        return response;
    }

    public DocumentResponse logDocument(DocumentLogRequest request) {
        String refNumber = documentReferenceService.generateReferenceNumber(
                request.getDocumentType(),
                request.getEmploymentType()
        );

        Map<String, Object> details = new HashMap<>();
        details.put("joiningDate", request.getJoiningDate());
        details.put("salary", request.getSalary());
        details.put("duration", request.getDuration());
        details.put("reportingTo", request.getReportingTo());
        details.put("validUntil", request.getValidUntil());

        DocumentResponse doc = new DocumentResponse(
                refNumber,
                request.getDocumentType(),
                request.getCandidateName(),
                request.getEmploymentType(),
                "ACTIVE",
                Instant.now(),
                details
        );

        documentStore.put(refNumber, doc);
        return doc;
    }

    public DocumentResponse getDocumentByRef(String referenceNumber) {
        DocumentResponse doc = documentStore.get(referenceNumber);
        if (doc == null) {
            throw new ResourceNotFoundException("Document not found with reference: " + referenceNumber);
        }
        return doc;
    }

    public List<Map<String, Object>> getRegisteredInterns() {
        return List.of(
                Map.of("id", "int_01", "name", "Aarav Patel", "track", "Backend Engineering", "status", "ACTIVE", "eodCount", 18),
                Map.of("id", "int_02", "name", "Sneha Rao", "track", "AI / Autonomous Agents", "status", "ACTIVE", "eodCount", 22)
        );
    }

    public List<Map<String, Object>> getDailySubmissions() {
        return List.of(
                Map.of("submissionId", "sub_101", "internName", "Aarav Patel", "date", "2026-10-31", "deliverable", "Spring Boot JWT Filter Chain", "status", "APPROVED"),
                Map.of("submissionId", "sub_102", "internName", "Sneha Rao", "date", "2026-10-31", "deliverable", "LangChain Tool Calling Evaluation", "status", "PENDING_REVIEW")
        );
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
