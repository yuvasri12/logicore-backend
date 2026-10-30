package com.devbridge.logicore.dto;

import com.devbridge.logicore.enums.ShipmentMode;
import com.devbridge.logicore.enums.ShipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ShipmentResponse {

    private Long id;
    private String trackingNumber;
    private Long customerId;
    private String customerName;
    private String origin;
    private String destination;
    private Long originWarehouseId;
    private String originWarehouseName;
    private Long destinationWarehouseId;
    private String destinationWarehouseName;
    private ShipmentMode mode;
    // Air waybill, container or bill of lading number (optional).
    private String referenceNumber;
    private BigDecimal weight;
    private ShipmentStatus status;
    // True while freight received with a problem has not been cleared by a manager.
    private boolean onHold;
    // NOT_REQUIRED, PENDING or CLEARED: customs for shipments that cross a country border.
    private String customsStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<StatusHistoryResponse> timeline = new ArrayList<StatusHistoryResponse>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

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

    public String getOriginWarehouseName() {
        return originWarehouseName;
    }

    public void setOriginWarehouseName(String originWarehouseName) {
        this.originWarehouseName = originWarehouseName;
    }

    public Long getDestinationWarehouseId() {
        return destinationWarehouseId;
    }

    public void setDestinationWarehouseId(Long destinationWarehouseId) {
        this.destinationWarehouseId = destinationWarehouseId;
    }

    public String getDestinationWarehouseName() {
        return destinationWarehouseName;
    }

    public void setDestinationWarehouseName(String destinationWarehouseName) {
        this.destinationWarehouseName = destinationWarehouseName;
    }

    public ShipmentMode getMode() {
        return mode;
    }

    public void setMode(ShipmentMode mode) {
        this.mode = mode;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public boolean isOnHold() {
        return onHold;
    }

    public void setOnHold(boolean onHold) {
        this.onHold = onHold;
    }

    public String getCustomsStatus() {
        return customsStatus;
    }

    public void setCustomsStatus(String customsStatus) {
        this.customsStatus = customsStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<StatusHistoryResponse> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<StatusHistoryResponse> timeline) {
        this.timeline = timeline;
    }
}