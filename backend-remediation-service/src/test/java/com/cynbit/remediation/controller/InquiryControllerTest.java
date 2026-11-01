package com.cynbit.remediation.controller;

import com.cynbit.remediation.dto.request.InquiryRequest;
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
class InquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should successfully record valid inquiry with 201 Created")
    void shouldAcceptValidInquiry() throws Exception {
        InquiryRequest request = new InquiryRequest(
                "Alex Rivera",
                "alex.rivera@example.com",
                "Fintech Solutions",
                "Technology",
                "We would like to discuss enterprise AI automation services."
        );

        mockMvc.perform(post("/api/v1/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.inquiryId").exists());
    }

    @Test
    @DisplayName("Should reject invalid email and short message with 400 Bad Request ProblemDetail")
    void shouldRejectInvalidInquiryInputs() throws Exception {
        InquiryRequest request = new InquiryRequest(
                "A", // too short
                "invalid-email-address", // invalid format
                "Company",
                "Industry",
                "short" // less than 10 characters
        );

        mockMvc.perform(post("/api/v1/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.message").exists());
    }
}

