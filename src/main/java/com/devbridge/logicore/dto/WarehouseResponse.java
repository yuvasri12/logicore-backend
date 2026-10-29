package com.devbridge.logicore.dto;

public class WarehouseResponse {

    private final Long id;
    private final String code;
    private final String name;
    private final String city;
    private final String country;
    private final Long regionId;
    private final String regionName;
    private final boolean active;

    public WarehouseResponse(Long id, String code, String name, String city, String country,
                             Long regionId, String regionName, boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.city = city;
        this.country = country;
        this.regionId = regionId;
        this.regionName = regionName;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public Long getRegionId() { return regionId; }
    public String getRegionName() { return regionName; }
    public boolean isActive() { return active; }
}