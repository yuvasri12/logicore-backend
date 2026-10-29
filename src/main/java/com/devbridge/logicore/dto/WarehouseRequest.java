package com.devbridge.logicore.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class WarehouseRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size.max}")
    private String code;

    @NotBlank(message = "{validation.required}")
    @Size(max = 120, message = "{validation.size.max}")
    private String name;

    @Size(max = 80, message = "{validation.size.max}")
    private String city;

    @Size(max = 80, message = "{validation.size.max}")
    private String country;

    @NotNull(message = "{validation.required}")
    private Long regionId;

    // Optional. When not sent, the active flag is left as it is.
    private Boolean active;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Long getRegionId() { return regionId; }
    public void setRegionId(Long regionId) { this.regionId = regionId; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}