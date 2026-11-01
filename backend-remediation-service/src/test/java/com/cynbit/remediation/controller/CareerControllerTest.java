package com.cynbit.remediation.controller;

import com.cynbit.remediation.dto.request.CareerApplicationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CareerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should submit application and reject duplicate submissions with 409 Conflict")
    void shouldHandleApplicationAndRejectDuplicates() throws Exception {
        CareerApplicationRequest request = new CareerApplicationRequest();
        request.setFullName("Priya Sharma");
        request.setEmail("priya.sharma.test@cynbit-candidate.com");
        request.setPhone("+919876543210");
        request.setWhatsapp("+919876543210");
        request.setDob("2002-04-12");
        request.setGender("FEMALE");
        request.setNoticePeriod("Immediate");
        request.setSkills("Java, Spring Boot, React, Microservices");
        request.setCurrentCity("Jaipur");
        request.setPermanentAddress("32/115 Pratap Nagar, Jaipur");
        request.setLinkedinUrl("https://linkedin.com/in/priyasharma");
        request.setPortfolioUrl("https://priyasharma.dev");
        request.setMessage("Excited to apply for the Backend Developer Intern role at Cynbit.");
        request.setAgreedToTerms(true);

        // First attempt: 201 Created
        mockMvc.perform(post("/api/v1/careers/backend-developer-intern/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.applicationId").exists())
                .andExpect(jsonPath("$.data.trackingUrl").exists());

        // Duplicate attempt: 409 Conflict with trackingUrl property
        mockMvc.perform(post("/api/v1/careers/backend-developer-intern/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Application"))
                .andExpect(jsonPath("$.trackingUrl").exists());
    }
}

