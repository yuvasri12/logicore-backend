package com.devbridge.logicore.dto;

import com.devbridge.logicore.enums.FreightCondition;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WarehouseReceiptResponse {

    private Long id;
    private Long shipmentId;
    private FreightCondition condition;
    private BigDecimal weightChecked;
    private String receivedBy;
    private LocalDateTime receivedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public FreightCondition getCondition() {
        return condition;
    }

    public void setCondition(FreightCondition condition) {
        this.condition = condition;
    }

    public BigDecimal getWeightChecked() {
        return weightChecked;
    }

    public void setWeightChecked(BigDecimal weightChecked) {
        this.weightChecked = weightChecked;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
}
