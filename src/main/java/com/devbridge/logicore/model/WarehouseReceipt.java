package com.devbridge.logicore.model;

import com.devbridge.logicore.enums.FreightCondition;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_receipts")
public class WarehouseReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FreightCondition condition;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal weightChecked;

    @Column(nullable = false, length = 80)
    private String receivedBy;

    public Long getId() {
        return id;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
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
