package com.devbridge.logicore.dto;

import java.time.LocalDateTime;

public class CustomerResponse {

    private final Long id;
    private final String name;
    private final String contactPerson;
    private final String email;
    private final String phone;
    private final String address;
    private final boolean active;
    private final LocalDateTime createdAt;

    public CustomerResponse(Long id, String name, String contactPerson, String email,
                            String phone, String address, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.contactPerson = contactPerson;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getContactPerson() { return contactPerson; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}