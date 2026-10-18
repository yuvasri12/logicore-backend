package com.devbridge.logicore.dto;

import com.devbridge.logicore.enums.FreightCondition;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class WarehouseReceiveRequest {

    @NotNull
    private Long shipmentId;

    @NotNull
    private FreightCondition condition;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal weightChecked;

    @NotBlank
    private String receivedBy;

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
}
