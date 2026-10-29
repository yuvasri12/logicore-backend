package com.devbridge.logicore.dto;

import com.devbridge.logicore.enums.Role;

import javax.validation.constraints.NotNull;

public class UpdateUserRequest {

    @NotNull(message = "{validation.required}")
    private Role role;

    @NotNull(message = "{validation.required}")
    private Boolean enabled;

    // Optional. Send them to change where the user works. Leave them out to keep the current assignment.
    private Long regionId;

    private Long warehouseId;

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Long getRegionId() { return regionId; }
    public void setRegionId(Long regionId) { this.regionId = regionId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
}