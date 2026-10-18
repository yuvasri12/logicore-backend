package com.devbridge.logicore.dto;

import com.devbridge.logicore.enums.Role;

import java.time.LocalDateTime;

public class UserAdminResponse {

    private final Long id;
    private final String email;
    private final String fullName;
    private final Role role;
    private final boolean enabled;
    private final LocalDateTime createdAt;

    public UserAdminResponse(Long id, String email, String fullName, Role role,
                             boolean enabled, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}