package com.cynbit.remediation.service;

import com.cynbit.remediation.security.JwtTokenProvider;
import com.cynbit.remediation.security.TokenBlacklistService;
import com.cynbit.remediation.security.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
            new ObjectMapper(),
            new TokenBlacklistService(),
            "test-secret-key-with-sufficient-entropy-for-hmac-sha256-testing-enterprise-level-1234567890",
            3600000L,
            "cynbit.in"
    );

    @Test
    @DisplayName("Should abort application startup if development seed is enabled under production profile")
    void shouldFailStartupIfDevSeedEnabledInProduction() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("production");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                new AuthService(
                        passwordEncoder,
                        jwtTokenProvider,
                        prodEnv,
                        3600000L,
                        true, // devSeedEnabled = true
                        "admin@example.local",
                        "SecretPassword123!",
                        "ROLE_HR_ADMIN"
                )
        );

        assertTrue(ex.getMessage().contains("strictly forbidden under the production profile"),
                "Startup exception message must clearly state the production violation");
    }

    @Test
    @DisplayName("Should initialize empty user registry when dev seed is disabled")
    void shouldInitializeEmptyRegistryWhenDevSeedDisabled() {
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");

        AuthService service = new AuthService(
                passwordEncoder,
                jwtTokenProvider,
                devEnv,
                3600000L,
                false, // devSeedEnabled = false
                null,
                null,
                "ROLE_HR_ADMIN"
        );

        // Verification: Any lookup should fail since registry is empty
        assertThrows(RuntimeException.class, () -> service.getUserByUsername("any.user@example.local"));
    }

    @Test
    @DisplayName("Should permit dynamic user registration via test fixtures")
    void shouldPermitDynamicUserRegistration() {
        MockEnvironment testEnv = new MockEnvironment();
        testEnv.setActiveProfiles("test");

        AuthService service = new AuthService(
                passwordEncoder,
                jwtTokenProvider,
                testEnv,
                3600000L,
                false,
                null,
                null,
                "ROLE_HR_ADMIN"
        );

        service.registerUser("usr_fixture_01", "fixture.user@example.local", "ValidPassword#123", UserRole.ROLE_MENTOR);
        assertNotNull(service.getUserByUsername("fixture.user@example.local"));
    }
}

