package com.devbridge.logicore.dto;

import com.devbridge.logicore.enums.ShipmentStatus;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class UpdateStatusRequest {

    @NotNull
    private ShipmentStatus status;

    @NotBlank
    private String updatedBy;

    private String remarks;

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
