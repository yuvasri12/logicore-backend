package com.devbridge.logicore.dto;

import javax.validation.constraints.NotBlank;

public class ShipmentActionRequest {

    @NotBlank
    private String updatedBy;

    private String remarks;

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