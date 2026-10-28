package com.devbridge.logicore.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class RegionRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size.max}")
    private String code;

    @NotBlank(message = "{validation.required}")
    @Size(max = 80, message = "{validation.size.max}")
    private String name;

    // Optional. When not sent, the active flag is left as it is.
    private Boolean active;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}