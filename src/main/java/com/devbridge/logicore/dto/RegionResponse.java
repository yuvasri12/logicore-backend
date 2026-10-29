package com.devbridge.logicore.dto;

public class RegionResponse {

    private final Long id;
    private final String code;
    private final String name;
    private final boolean active;

    public RegionResponse(Long id, String code, String name, boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}