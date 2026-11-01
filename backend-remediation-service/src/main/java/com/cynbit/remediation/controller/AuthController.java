package com.cynbit.remediation.controller;

import com.cynbit.remediation.dto.request.LoginRequest;
import com.cynbit.remediation.dto.response.ApiResponse;
import com.cynbit.remediation.dto.response.AuthResponse;
import com.cynbit.remediation.security.JwtTokenProvider;
import com.cynbit.remediation.security.SecurityUser;
import com.cynbit.remediation.security.TokenBlacklistService;
import com.cynbit.remediation.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/portal/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;
    private final String cookieName;
    private final long expirationMs;

    public AuthController(
            AuthService authService,
            TokenBlacklistService tokenBlacklistService,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.security.jwt.cookie-name:cynbit_session}") String cookieName,
            @Value("${app.security.jwt.expiration-ms:28800000}") long expirationMs) {
        this.authService = authService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieName = cookieName;
        this.expirationMs = expirationMs;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.login(request);

        // Store JWT strictly in an HttpOnly, Secure, SameSite=Strict cookie
        ResponseCookie cookie = ResponseCookie.from(cookieName, authResponse.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(expirationMs / 1000)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // Return ONLY safe user metadata. Never expose the raw JWT in JSON body!
        Map<String, Object> safeUserInfo = Map.of(
                "userId", authResponse.getUserId(),
                "username", authResponse.getUsername(),
                "role", authResponse.getRole()
        );

        return ResponseEntity.ok(ApiResponse.success("Authentication successful.", safeUserInfo));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        String token = resolveToken(request);
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            long expirationEpochMs = jwtTokenProvider.getExpirationEpochMs(token);
            tokenBlacklistService.blacklistToken(token, expirationEpochMs);
        }

        // Expire session cookie
        ResponseCookie clearCookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
        return ResponseEntity.ok(ApiResponse.success("Successfully logged out. Session terminated.", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(
            @AuthenticationPrincipal SecurityUser principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
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

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
