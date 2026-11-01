package com.cynbit.remediation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class InquiryRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email address is required")
    @Email(message = "Email must be a valid format")
    @Size(max = 120, message = "Email cannot exceed 120 characters")
    private String email;

    @Size(max = 120, message = "Company name cannot exceed 120 characters")
    private String company;

    @Size(max = 100, message = "Industry cannot exceed 100 characters")
    private String industry;

    @NotBlank(message = "Message content is required")
    @Size(min = 10, max = 3000, message = "Message must be between 10 and 3000 characters")
    private String message;

    public InquiryRequest() {}

    public InquiryRequest(String fullName, String email, String company, String industry, String message) {
        this.fullName = fullName;
        this.email = email;
        this.company = company;
        this.industry = industry;
        this.message = message;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
