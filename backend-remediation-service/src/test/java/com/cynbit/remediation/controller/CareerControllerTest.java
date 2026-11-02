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
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @DisplayName("Should submit application, retrieve status with minimal data, and reject duplicate submissions with 409 Conflict")
    void shouldHandleApplicationAndRejectDuplicates() throws Exception {
        CareerApplicationRequest request = new CareerApplicationRequest();
        request.setFullName("Priya Sharma");
        request.setEmail("candidate.test@example.local");
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
        MvcResult result = mockMvc.perform(post("/api/v1/careers/backend-developer-intern/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.applicationId").exists())
                .andExpect(jsonPath("$.data.trackingToken").exists())
                .andExpect(jsonPath("$.data.trackingUrl").exists())
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.candidateName").doesNotExist())
                .andReturn();

        String appId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("applicationId").asText();

        // High-entropy tracking token check (must start with trk_)
        assertTrue(appId.startsWith("trk_"), "Application ID must be a high-entropy tracking token starting with trk_");

        // Verify status retrieval via GET /api/v1/careers/{jobId}/applications/{applicationId}
        // CRITICAL PRIVACY ASSERTION: Only minimum lifecycle status is returned; email and personal info are omitted.
        mockMvc.perform(get("/api/v1/careers/backend-developer-intern/applications/" + appId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationId").value(appId))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.candidateName").doesNotExist());

        // Duplicate attempt: 409 Conflict without leaking existing tracking token or URL
        mockMvc.perform(post("/api/v1/careers/backend-developer-intern/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Application"))
                // REGRESSION TEST: Must NEVER disclose existing trackingUrl or trackingToken
                .andExpect(jsonPath("$.trackingUrl").doesNotExist())
                .andExpect(jsonPath("$.trackingToken").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("Should restrict unauthorized HTTP methods on career endpoints with 401 Unauthorized")
    void shouldBlockUnauthorizedHttpMethodsOnCareerEndpoints() throws Exception {
        // DELETE on career route is not publicly permitted in SecurityConfig and requires authentication
        mockMvc.perform(delete("/api/v1/careers/backend-developer-intern/applications").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }
}

