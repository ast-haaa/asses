package com.cynbit.remediation.controller;

import com.cynbit.remediation.dto.request.DocumentLogRequest;
import com.cynbit.remediation.dto.request.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should authenticate valid HR admin, set HttpOnly cookie, and permit access to documents")
    void shouldAuthenticateAndAccessProtectedResource() throws Exception {
        LoginRequest loginRequest = new LoginRequest("hr.admin@cynbit.in", "HrAdmin#Cynbit2026");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/portal/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.role").value("ROLE_HR_ADMIN"))
                .andReturn();

        String setCookieHeader = loginResult.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("HttpOnly"));
        assertTrue(setCookieHeader.contains("SameSite=Strict"));

        String responseBody = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("data").get("token").asText();

        // Access protected resource using Bearer token
        DocumentLogRequest docRequest = new DocumentLogRequest();
        docRequest.setDocumentType("OFFER_LETTER");
        docRequest.setCandidateName("Rohit Verma");
        docRequest.setEmploymentType("Internship");
        docRequest.setJoiningDate("2026-12-01");
        docRequest.setSalary("Stipend");

        mockMvc.perform(post("/api/v1/portal/documents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.referenceNumber").exists())
                .andExpect(jsonPath("$.data.referenceNumber").value(org.hamcrest.Matchers.containsString("CB/HR/INT/")));
    }

    @Test
    @DisplayName("Should reject invalid login credentials with 401 Unauthorized")
    void shouldRejectInvalidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest("hr.admin@cynbit.in", "WrongPassword#123");

        mockMvc.perform(post("/api/v1/portal/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("Should block unauthenticated access to protected document endpoint with 401 Unauthorized")
    void shouldBlockUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/portal/interns"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }
}

