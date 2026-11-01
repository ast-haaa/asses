package com.cynbit.remediation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DocumentLogRequest {

    @NotBlank(message = "Document type is required (e.g. OFFER_LETTER, BITELEVATE_OFFER, JOINING_KIT)")
    private String documentType;

    @NotBlank(message = "Candidate name is required")
    @Size(min = 2, max = 120, message = "Candidate name must be between 2 and 120 characters")
    private String candidateName;

    @NotBlank(message = "Employment type is required (e.g. Full-Time, Internship)")
    private String employmentType;

    @NotBlank(message = "Joining date is required")
    private String joiningDate;

    @NotBlank(message = "Salary or stipend detail is required")
    private String salary;

    private String duration;
    private String reportingTo;
    private String validUntil;

    public DocumentLogRequest() {}

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

    public String getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(String joiningDate) {
        this.joiningDate = joiningDate;
    }

    public String getSalary() {
        return salary;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getReportingTo() {
        return reportingTo;
    }

    public void setReportingTo(String reportingTo) {
        this.reportingTo = reportingTo;
    }

    public String getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(String validUntil) {
        this.validUntil = validUntil;
    }
}

