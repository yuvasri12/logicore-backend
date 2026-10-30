package com.devbridge.logicore.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** The customs reference (entry or declaration number) and an optional note. */
public class ClearCustomsRequest {

    @NotBlank
    @Size(max = 60, message = "{validation.size.max}")
    private String customsReference;

    @Size(max = 300, message = "{validation.size.max}")
    private String note;

    public String getCustomsReference() {
        return customsReference;
    }

    public void setCustomsReference(String customsReference) {
        this.customsReference = customsReference;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}