package com.cynbit.remediation.controller;

import com.cynbit.remediation.dto.request.LoginRequest;
import com.cynbit.remediation.dto.response.ApiResponse;
import com.cynbit.remediation.dto.response.AuthResponse;
import com.cynbit.remediation.security.SecurityUser;
import com.cynbit.remediation.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/portal/auth")
public class AuthController {

    private final AuthService authService;
    private final String cookieName;
    private final long expirationMs;

    public AuthController(
            AuthService authService,
            @Value("${app.security.jwt.cookie-name:cynbit_session}") String cookieName,
            @Value("${app.security.jwt.expiration-ms:28800000}") long expirationMs) {
        this.authService = authService;
        this.cookieName = cookieName;
        this.expirationMs = expirationMs;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.login(request);

        // Issue HttpOnly, Secure, SameSite=Strict cookie
        ResponseCookie cookie = ResponseCookie.from(cookieName, authResponse.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(expirationMs / 1000)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ApiResponse.success("Authentication successful.", authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(ApiResponse.success("Successfully logged out.", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(
            @AuthenticationPrincipal SecurityUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }

        return ResponseEntity.ok(ApiResponse.success("User session active", Map.of(
                "userId", principal.getUserId(),
                "username", principal.getUsername(),
                "role", principal.getRole().name()
        )));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.success("Authentication service healthy.", Map.of("status", "UP")));
    }
}
