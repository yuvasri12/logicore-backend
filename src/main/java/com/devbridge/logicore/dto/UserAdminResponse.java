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
    private final Long regionId;
    private final String regionName;
    private final Long warehouseId;
    private final String warehouseName;

    public UserAdminResponse(Long id, String email, String fullName, Role role,
                             boolean enabled, LocalDateTime createdAt,
                             Long regionId, String regionName,
                             Long warehouseId, String warehouseName) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.regionId = regionId;
        this.regionName = regionName;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getRegionId() { return regionId; }
    public String getRegionName() { return regionName; }
    public Long getWarehouseId() { return warehouseId; }
    public String getWarehouseName() { return warehouseName; }
}