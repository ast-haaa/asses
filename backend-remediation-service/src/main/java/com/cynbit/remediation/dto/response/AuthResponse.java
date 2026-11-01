package com.cynbit.remediation.dto.response;

public class AuthResponse {

    private String userId;
    private String username;
    private String role;
    private String token;
    private long expiresIn;

    public AuthResponse() {}

    public AuthResponse(String userId, String username, String role, String token, long expiresIn) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.token = token;
        this.expiresIn = expiresIn;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
