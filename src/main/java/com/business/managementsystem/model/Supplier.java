package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "supplier")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private String name;

    private String contactPerson;
    private String phone;
    private String email;
    private String address;

    @Column(length = 500)
    private String notes;

    // Optional link to a customer record — set when this supplier is also a customer
    @Column
    private Long linkedCustomerId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Supplier() {}

    public Supplier(Long businessId, String name, String contactPerson,
                    String phone, String email, String address, String notes) {
        this.businessId    = businessId;
        this.name          = name;
        this.contactPerson = contactPerson;
        this.phone         = phone;
        this.email         = email;
        this.address       = address;
        this.notes         = notes;
    }

    public Long getId() { return id; }
    public Long getBusinessId() { return businessId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getLinkedCustomerId() { return linkedCustomerId; }
    public void setLinkedCustomerId(Long linkedCustomerId) { this.linkedCustomerId = linkedCustomerId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}