package com.webcrawler.backend.web.dto;

import java.time.LocalDateTime;
import java.util.Set;

public class UserResponse {

    private final Long id;
    private final String username;
    private final Set<String> roles;
    private final LocalDateTime createdAt;

    public UserResponse(Long id, String username, Set<String> roles, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.roles = roles;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
