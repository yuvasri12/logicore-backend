package com.devbridge.logicore.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Why a receiving problem (damage, missing items...) is accepted. */
public class ClearIssueRequest {

    @NotBlank
    @Size(max = 300)
    private String note;

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}