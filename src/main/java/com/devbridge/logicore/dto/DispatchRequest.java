package com.devbridge.logicore.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class DispatchRequest {

    @NotNull
    private Long shipmentId;

    @NotBlank
    private String vehicleNumber;

    @NotBlank
    private String destination;

    @NotBlank
    private String dispatchedBy;

    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDispatchedBy() {
        return dispatchedBy;
    }

    public void setDispatchedBy(String dispatchedBy) {
        this.dispatchedBy = dispatchedBy;
    }
}
