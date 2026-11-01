package com.cynbit.remediation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EducationEntry {

    @NotBlank(message = "Qualification level is required")
    private String qualificationLevel;

    @NotBlank(message = "Degree name is required")
    private String degree;

    @Size(max = 120, message = "Specialization cannot exceed 120 characters")
    private String specialization;

    @NotBlank(message = "University or institution name is required")
    @Size(max = 150, message = "University cannot exceed 150 characters")
    private String university;

    @Min(value = 1990, message = "Graduation year must be valid")
    @Max(value = 2035, message = "Graduation year must be valid")
    private Integer graduationYear;

    @Size(max = 20, message = "Score type cannot exceed 20 characters")
    private String scoreType;

    @Size(max = 20, message = "Score value cannot exceed 20 characters")
    private String scoreValue;

    @Size(max = 50, message = "Status cannot exceed 50 characters")
    private String status;

    public EducationEntry() {}

    public String getQualificationLevel() {
        return qualificationLevel;
    }

    public void setQualificationLevel(String qualificationLevel) {
        this.qualificationLevel = qualificationLevel;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    public String getScoreType() {
        return scoreType;
    }

    public void setScoreType(String scoreType) {
        this.scoreType = scoreType;
    }

    public String getScoreValue() {
        return scoreValue;
    }

    public void setScoreValue(String scoreValue) {
        this.scoreValue = scoreValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

