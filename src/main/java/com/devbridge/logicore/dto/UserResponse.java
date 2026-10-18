package com.devbridge.logicore.dto;

import com.devbridge.logicore.enums.Role;

public class UserResponse {

    private final Long id;
    private final String email;
    private final String fullName;
    private final Role role;

    public UserResponse(Long id, String email, String fullName, Role role) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
}