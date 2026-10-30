package com.devbridge.logicore.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/** Proof that an international shipment was cleared through customs. One per shipment. */
@Entity
@Table(
        name = "customs_clearances",
        uniqueConstraints = @UniqueConstraint(name = "uk_customs_shipment", columnNames = "shipment_id"))
public class CustomsClearance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(nullable = false, length = 60)
    private String customsReference;

    @Column(nullable = false, length = 80)
    private String clearedBy;

    @Column(nullable = false)
    private LocalDateTime clearedAt;

    @Column(length = 300)
    private String note;

    public Long getId() {
        return id;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public String getCustomsReference() {
        return customsReference;
    }

    public void setCustomsReference(String customsReference) {
        this.customsReference = customsReference;
    }

    public String getClearedBy() {
        return clearedBy;
    }

    public void setClearedBy(String clearedBy) {
        this.clearedBy = clearedBy;
    }

    public LocalDateTime getClearedAt() {
        return clearedAt;
    }

    public void setClearedAt(LocalDateTime clearedAt) {
        this.clearedAt = clearedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}