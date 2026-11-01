package com.cynbit.remediation.dto.response;

import java.time.Instant;
import java.util.Map;

public class DocumentResponse {

    private String referenceNumber;
    private String documentType;
    private String candidateName;
    private String employmentType;
    private String status;
    private Instant createdAt;
    private Map<String, Object> details;

    public DocumentResponse() {}

    public DocumentResponse(String referenceNumber, String documentType, String candidateName, String employmentType, String status, Instant createdAt, Map<String, Object> details) {
        this.referenceNumber = referenceNumber;
        this.documentType = documentType;
        this.candidateName = candidateName;
        this.employmentType = employmentType;
        this.status = status;
        this.createdAt = createdAt;
        this.details = details;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}

