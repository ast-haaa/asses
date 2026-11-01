package com.cynbit.remediation.service;

import com.cynbit.remediation.dto.request.LoginRequest;
import com.cynbit.remediation.dto.response.AuthResponse;
import com.cynbit.remediation.exception.BusinessException;
import com.cynbit.remediation.security.JwtTokenProvider;
import com.cynbit.remediation.security.SecurityUser;
import com.cynbit.remediation.security.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final long expirationMs;
    private final Map<String, SecurityUser> userRegistry = new ConcurrentHashMap<>();

    public AuthService(
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.security.jwt.expiration-ms:28800000}") long expirationMs) {
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.expirationMs = expirationMs;

        // Initialize production-like administrative accounts with strong BCrypt hashes
        registerUser("usr_super_01", "super.admin@cynbit.in", "Admin@Cynbit#2026", UserRole.ROLE_SUPER_ADMIN);
        registerUser("usr_hr_01", "hr.admin@cynbit.in", "HrAdmin#Cynbit2026", UserRole.ROLE_HR_ADMIN);
        registerUser("usr_mentor_01", "mentor@cynbit.in", "Mentor#Cynbit2026", UserRole.ROLE_MENTOR);
    }

    private void registerUser(String id, String username, String rawPassword, UserRole role) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        userRegistry.put(username.toLowerCase(), new SecurityUser(id, username, encodedPassword, role));
    }

    public AuthResponse login(LoginRequest request) {
        SecurityUser user = userRegistry.get(request.getUsername().toLowerCase().trim());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or credentials");
        }

        String token = jwtTokenProvider.generateToken(user);
        return new AuthResponse(user.getUserId(), user.getUsername(), user.getRole().name(), token, expirationMs / 1000);
    }

    public SecurityUser getUserByUsername(String username) {
        SecurityUser user = userRegistry.get(username.toLowerCase().trim());
        if (user == null) {
            throw new BusinessException("User profile not found");
        }
        return user;
    }
}
