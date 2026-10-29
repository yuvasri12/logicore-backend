package com.devbridge.logicore.model;

import javax.persistence.*;

/**
 * A bin inside one warehouse. "Zone A / Rack 1 / Bin 1" can exist in every warehouse,
 * so a location is identified by warehouse + zone + rack + bin.
 * The warehouse can be empty only for old rows created before locations belonged to a warehouse.
 */
@Entity
@Table(
        name = "storage_locations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_storage_location",
                columnNames = {"warehouse_id", "zone", "rack", "bin"}))
public class StorageLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(nullable = false, length = 20)
    private String zone;

    @Column(nullable = false, length = 20)
    private String rack;

    @Column(nullable = false, length = 20)
    private String bin;

    public Long getId() {
        return id;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getRack() {
        return rack;
    }

    public void setRack(String rack) {
        this.rack = rack;
    }

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }
}