package com.devbridge.logicore.dto;

import com.devbridge.logicore.enums.ShipmentStatus;

import java.time.LocalDateTime;

public class TrackingTimelineResponse {

    private ShipmentStatus status;
    private String publicRemark;
    private LocalDateTime updatedAt;

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public String getPublicRemark() {
        return publicRemark;
    }

    public void setPublicRemark(String publicRemark) {
        this.publicRemark = publicRemark;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
