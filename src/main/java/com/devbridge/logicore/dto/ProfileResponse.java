package com.devbridge.logicore.dto;

import com.devbridge.logicore.enums.Role;

/**
 * What GET /api/auth/me returns: the logged-in user plus where they work.
 * Admin has no region or warehouse (sees everything), so both are null.
 */
public class ProfileResponse {

    private final Long id;
    private final String email;
    private final String fullName;
    private final Role role;
    private final String regionName;
    private final String warehouseName;

    public ProfileResponse(Long id, String email, String fullName, Role role,
                           String regionName, String warehouseName) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.regionName = regionName;
        this.warehouseName = warehouseName;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public String getRegionName() { return regionName; }
    public String getWarehouseName() { return warehouseName; }
}