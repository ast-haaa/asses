package com.cynbit.remediation.controller;

import com.cynbit.remediation.dto.request.DocumentLogRequest;
import com.cynbit.remediation.dto.request.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
    @DisplayName("Should set HttpOnly, Secure, SameSite=Strict cookie on login and NOT expose JWT in JSON body")
    void shouldSetSecureHttpOnlyCookieAndNotExposeJwtInJsonOnLogin() throws Exception {
        LoginRequest loginRequest = new LoginRequest("hr.admin@cynbit.in", "HrAdmin#Cynbit2026");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/portal/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                // CRITICAL SECURITY ASSERTION: JWT must NEVER be returned in response JSON
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andExpect(jsonPath("$.data.userId").exists())
                .andExpect(jsonPath("$.data.username").value("hr.admin@cynbit.in"))
                .andExpect(jsonPath("$.data.role").value("ROLE_HR_ADMIN"))
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("cynbit_session");
        assertNotNull(sessionCookie, "cynbit_session cookie must be present");
        assertTrue(sessionCookie.isHttpOnly(), "Cookie must enforce HttpOnly to prevent XSS theft");
        assertTrue(sessionCookie.getSecure(), "Cookie must enforce Secure attribute");
        assertEquals("/", sessionCookie.getPath(), "Cookie must be scoped to Path=/");

        // Verify SameSite=Strict attribute in raw Set-Cookie header
        boolean hasSameSiteStrict = loginResult.getResponse().getHeaders("Set-Cookie").stream()
                .anyMatch(header -> header.contains("cynbit_session") && header.contains("SameSite=Strict"));
        assertTrue(hasSameSiteStrict, "cynbit_session Set-Cookie header must include SameSite=Strict");
    }

    @Test
    @DisplayName("Should permit access to /me with valid session cookie and reject unauthenticated requests")
    void shouldHandleMeEndpointAccess() throws Exception {
        // 1. Unauthenticated request to /me must be rejected with 401
        mockMvc.perform(get("/api/v1/portal/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));

        // 2. Authenticate to obtain session cookie
        LoginRequest loginRequest = new LoginRequest("hr.admin@cynbit.in", "HrAdmin#Cynbit2026");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/portal/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("cynbit_session");
        assertNotNull(sessionCookie);

        // 3. Authenticated request to /me with cookie must succeed
        mockMvc.perform(get("/api/v1/portal/auth/me").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("hr.admin@cynbit.in"))
                .andExpect(jsonPath("$.data.role").value("ROLE_HR_ADMIN"));
    }

    @Test
    @DisplayName("Should enforce CSRF protection on state-changing requests using cookie authentication")
    void shouldEnforceCsrfProtectionOnStateChangingRequests() throws Exception {
        // 1. Login to obtain session cookie
        LoginRequest loginRequest = new LoginRequest("hr.admin@cynbit.in", "HrAdmin#Cynbit2026");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/portal/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("cynbit_session");
        assertNotNull(sessionCookie);

        DocumentLogRequest docRequest = new DocumentLogRequest();
        docRequest.setDocumentType("OFFER_LETTER");
        docRequest.setCandidateName("Rohit Verma");
        docRequest.setEmploymentType("Internship");
        docRequest.setJoiningDate("2026-12-01");
        docRequest.setSalary("Stipend");

        // 2. Attempt state-changing mutation with session cookie BUT WITHOUT CSRF token -> 403 Forbidden
        mockMvc.perform(post("/api/v1/portal/documents")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docRequest)))
                .andExpect(status().isForbidden());

        // 3. Attempt state-changing mutation with session cookie AND valid CSRF token -> 201 Created
        mockMvc.perform(post("/api/v1/portal/documents")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.referenceNumber").exists());
    }

    @Test
    @DisplayName("Should clear session cookie and invalidate token in blacklist upon logout")
    void shouldClearCookieAndInvalidateTokenUponLogout() throws Exception {
        LoginRequest loginRequest = new LoginRequest("hr.admin@cynbit.in", "HrAdmin#Cynbit2026");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/portal/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("cynbit_session");
        assertNotNull(sessionCookie);

        // Perform logout with session cookie
        MvcResult logoutResult = mockMvc.perform(post("/api/v1/portal/auth/logout")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully logged out. Session terminated."))
                .andReturn();

        // Verify Set-Cookie header expires the cookie (Max-Age=0)
        boolean hasClearCookie = logoutResult.getResponse().getHeaders("Set-Cookie").stream()
                .anyMatch(h -> h.contains("cynbit_session") && (h.contains("Max-Age=0") || h.contains("max-age=0")));
        assertTrue(hasClearCookie, "Logout must emit Set-Cookie with Max-Age=0 for cynbit_session");

        // Verify token in sessionCookie is blacklisted: subsequent call to /me must fail with 401
        mockMvc.perform(get("/api/v1/portal/auth/me").cookie(sessionCookie))
                .andExpect(status().isUnauthorized());
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
}
