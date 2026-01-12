package com.webcrawler.backend.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.Set;

public class AuthResponse {

    private final String token;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant expiresAt;

    private final String username;

    private final Set<String> roles;

    public AuthResponse(String token, Instant expiresAt, String username, Set<String> roles) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.username = username;
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getUsername() {
        return username;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
