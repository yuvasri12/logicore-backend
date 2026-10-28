package com.devbridge.logicore.dto;

import com.devbridge.logicore.enums.Role;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 120, message = "{validation.size.max}")
    private String fullName;

    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.email}")
    @Size(max = 160, message = "{validation.size.max}")
    private String email;

    @NotNull(message = "{validation.required}")
    private Role role;

    @NotBlank(message = "{validation.required}")
    @Size(min = 8, max = 72, message = "{validation.size.min}")
    private String password;

    // Where the user works. Ops Manager: regionId. Warehouse Staff / Dispatcher: warehouseId.
    private Long regionId;

    private Long warehouseId;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Long getRegionId() { return regionId; }
    public void setRegionId(Long regionId) { this.regionId = regionId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
}