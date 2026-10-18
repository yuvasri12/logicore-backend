package com.devbridge.logicore.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CustomerRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 120, message = "{validation.size.max}")
    private String name;

    @Size(max = 120, message = "{validation.size.max}")
    private String contactPerson;

    @Email(message = "{validation.email}")
    @Size(max = 160, message = "{validation.size.max}")
    private String email;

    @Size(max = 30, message = "{validation.size.max}")
    private String phone;

    @Size(max = 255, message = "{validation.size.max}")
    private String address;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}