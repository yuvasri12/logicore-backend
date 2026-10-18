package com.devbridge.logicore.dto;

import com.devbridge.logicore.enums.Role;

import javax.validation.constraints.NotNull;

public class UpdateUserRequest {

    @NotNull(message = "{validation.required}")
    private Role role;

    @NotNull(message = "{validation.required}")
    private Boolean enabled;

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}