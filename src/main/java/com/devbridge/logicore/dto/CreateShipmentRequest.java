package com.devbridge.logicore.dto;

import com.devbridge.logicore.enums.ShipmentMode;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class CreateShipmentRequest {

    // Send customerId to use a saved customer. A typed customerName still works if no id is sent.
    private Long customerId;

    @Size(max = 120, message = "{validation.size.max}")
    private String customerName;

    @NotBlank
    private String origin;

    @NotBlank
    private String destination;

    // Where the shipment starts and ends, as warehouses. They decide who may see it.
    private Long originWarehouseId;

    private Long destinationWarehouseId;

    @NotNull
    private ShipmentMode mode;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal weight;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Long getOriginWarehouseId() {
        return originWarehouseId;
    }

    public void setOriginWarehouseId(Long originWarehouseId) {
        this.originWarehouseId = originWarehouseId;
    }

    public Long getDestinationWarehouseId() {
        return destinationWarehouseId;
    }

    public void setDestinationWarehouseId(Long destinationWarehouseId) {
        this.destinationWarehouseId = destinationWarehouseId;
    }

    public ShipmentMode getMode() {
        return mode;
    }

    public void setMode(ShipmentMode mode) {
        this.mode = mode;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
}