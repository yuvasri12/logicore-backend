package com.devbridge.logicore.dto;

import javax.validation.constraints.NotBlank;

public class CancelShipmentRequest {

    @NotBlank
    private String reason;

    @NotBlank
    private String cancelledBy;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }
}
