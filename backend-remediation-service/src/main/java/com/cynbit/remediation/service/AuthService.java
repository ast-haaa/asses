package com.cynbit.remediation.service;

import com.cynbit.remediation.dto.request.LoginRequest;
import com.cynbit.remediation.dto.response.AuthResponse;
import com.cynbit.remediation.exception.BusinessException;
import com.cynbit.remediation.security.JwtTokenProvider;
import com.cynbit.remediation.security.SecurityUser;
import com.cynbit.remediation.security.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final long expirationMs;
    private final Map<String, SecurityUser> userRegistry = new ConcurrentHashMap<>();

    public AuthService(
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.security.jwt.expiration-ms:28800000}") long expirationMs,
            @Value("${app.security.dev-seed.enabled:false}") boolean devSeedEnabled,
            @Value("${app.security.dev-seed.username:}") String devUsername,
            @Value("${app.security.dev-seed.password:}") String devPassword,
            @Value("${app.security.dev-seed.role:ROLE_HR_ADMIN}") String devRole) {
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.expirationMs = expirationMs;

        if (devSeedEnabled) {
            if (StringUtils.hasText(devUsername) && StringUtils.hasText(devPassword)) {
                log.warn("DEVELOPMENT NOTICE: Explicit development seed enabled for user [{}]. Never enable in production.", devUsername);
                try {
                    UserRole role = UserRole.valueOf(devRole.trim());
                    registerUser("usr_dev_seed", devUsername.trim(), devPassword.trim(), role);
                } catch (IllegalArgumentException e) {
                    log.error("Invalid dev-seed role configured [{}], default to ROLE_HR_ADMIN", devRole);
                    registerUser("usr_dev_seed", devUsername.trim(), devPassword.trim(), UserRole.ROLE_HR_ADMIN);
                }
            } else {
                log.info("Development seed flag is active but dev-seed credentials are not configured. In-memory registry initialized empty.");
            }
        } else {
            log.info("Standard mode: In-memory user registry initialized empty. Administrative accounts must be configured via authoritative identity provider or test fixtures.");
        }
    }

    /**
     * Registers a security user into the in-memory registry.
     * Used for test fixtures, dynamic onboarding, or development seeding.
     */
    public void registerUser(String id, String username, String rawPassword, UserRole role) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        userRegistry.put(username.toLowerCase().trim(), new SecurityUser(id, username.trim(), encodedPassword, role));
    }

    public void clearUsers() {
        userRegistry.clear();
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

