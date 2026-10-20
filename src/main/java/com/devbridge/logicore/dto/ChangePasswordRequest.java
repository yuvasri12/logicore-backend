package com.devbridge.logicore.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ChangePasswordRequest {

    @NotBlank(message = "{validation.required}")
    private String currentPassword;

    // 72 is the longest password BCrypt can use.
    @NotBlank(message = "{validation.required}")
    @Size(min = 8, max = 72, message = "{validation.size.min}")
    private String newPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
