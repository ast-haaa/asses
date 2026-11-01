package com.cynbit.remediation.dto.response;

import java.time.Instant;

public class ApplicationStatusResponse {

    private String applicationId;
    private String jobId;
    private String candidateName;
    private String email;
    private boolean duplicate;
    private String trackingUrl;
    private String status;
    private Instant submittedAt;

    public ApplicationStatusResponse() {}

    public ApplicationStatusResponse(String applicationId, String jobId, String candidateName, String email, boolean duplicate, String trackingUrl, String status, Instant submittedAt) {
        this.applicationId = applicationId;
        this.jobId = jobId;
        this.candidateName = candidateName;
        this.email = email;
        this.duplicate = duplicate;
        this.trackingUrl = trackingUrl;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public String getTrackingUrl() {
        return trackingUrl;
    }

    public void setTrackingUrl(String trackingUrl) {
        this.trackingUrl = trackingUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }
}

