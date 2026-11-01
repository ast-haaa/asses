package com.cynbit.remediation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CareerApplicationRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email address is required")
    @Email(message = "Email must be a valid format")
    @Size(max = 120, message = "Email cannot exceed 120 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be between 10 and 15 digits, optionally prefixed with +")
    private String phone;

    @Pattern(regexp = "^(\\+?[0-9]{10,15})?$", message = "WhatsApp number must be valid if provided")
    private String whatsapp;

    @NotBlank(message = "Date of birth is required")
    private String dob;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotBlank(message = "Notice period is required")
    @Size(max = 60, message = "Notice period cannot exceed 60 characters")
    private String noticePeriod;

    @NotBlank(message = "Skills summary is required")
    @Size(max = 500, message = "Skills cannot exceed 500 characters")
    private String skills;

    @NotBlank(message = "Current city is required")
    @Size(max = 100, message = "Current city cannot exceed 100 characters")
    private String currentCity;

    @NotBlank(message = "Residential address is required")
    @Size(max = 500, message = "Permanent address cannot exceed 500 characters")
    private String permanentAddress;

    @Pattern(regexp = "^(https?://.*)?$", message = "Portfolio URL must be a valid HTTP/HTTPS URL")
    private String portfolioUrl;

    @NotBlank(message = "LinkedIn URL is required")
    @Pattern(regexp = "^https?://(www\\.)?linkedin\\.com/.*$", message = "Must be a valid LinkedIn profile URL")
    private String linkedinUrl;

    @Pattern(regexp = "^(https?://.*)?$", message = "Profile URL must be valid if provided")
    private String professionalProfileUrl;

    @NotBlank(message = "Cover letter / motivation message is required")
    @Size(min = 10, max = 5000, message = "Message must be between 10 and 5000 characters")
    private String message;

    @AssertTrue(message = "Candidate must agree to the terms and privacy conditions")
    private Boolean agreedToTerms;

    @Valid
    private List<EducationEntry> educationHistory;

    public CareerApplicationRequest() {}

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getNoticePeriod() {
        return noticePeriod;
    }

    public void setNoticePeriod(String noticePeriod) {
        this.noticePeriod = noticePeriod;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getCurrentCity() {
        return currentCity;
    }

    public void setCurrentCity(String currentCity) {
        this.currentCity = currentCity;
    }

    public String getPermanentAddress() {
        return permanentAddress;
    }

    public void setPermanentAddress(String permanentAddress) {
        this.permanentAddress = permanentAddress;
    }

    public String getPortfolioUrl() {
        return portfolioUrl;
    }

    public void setPortfolioUrl(String portfolioUrl) {
        this.portfolioUrl = portfolioUrl;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public String getProfessionalProfileUrl() {
        return professionalProfileUrl;
    }

    public void setProfessionalProfileUrl(String professionalProfileUrl) {
        this.professionalProfileUrl = professionalProfileUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getAgreedToTerms() {
        return agreedToTerms;
    }

    public void setAgreedToTerms(Boolean agreedToTerms) {
        this.agreedToTerms = agreedToTerms;
    }

    public List<EducationEntry> getEducationHistory() {
        return educationHistory;
    }

    public void setEducationHistory(List<EducationEntry> educationHistory) {
        this.educationHistory = educationHistory;
    }
}

