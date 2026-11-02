package com.cynbit.remediation.dto.response;

import java.time.Instant;

/**
 * Public response DTO returning strictly minimum application status data.
 * CRITICAL PRIVACY CONTROL: Does NOT expose candidate email, full name, phone number,
 * or other personal identifiable information (PII) over public status queries.
 */
public class ApplicationStatusResponse {

    private String trackingToken;
    private String applicationId;
    private String jobId;
    private String trackingUrl;
    private String status;
    private Instant submittedAt;

    public ApplicationStatusResponse() {}

    public ApplicationStatusResponse(String trackingToken, String jobId, String trackingUrl, String status, Instant submittedAt) {
        this.trackingToken = trackingToken;
        this.applicationId = trackingToken; // Aliased for client backwards compatibility
        this.jobId = jobId;
        this.trackingUrl = trackingUrl;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public String getTrackingToken() {
        return trackingToken;
    }

    public void setTrackingToken(String trackingToken) {
        this.trackingToken = trackingToken;
        this.applicationId = trackingToken;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
        this.trackingToken = applicationId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
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
