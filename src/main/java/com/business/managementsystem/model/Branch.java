package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "branch")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private String name;

    @Column
    private String address;

    @Column
    private String phone;

    @Column
    private String city;

    // Country matters — different country = price overrides may apply
    @Column
    private String country = "UAE";

    @Column(nullable = false)
    private boolean isMainBranch = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Status {
        ACTIVE,
        INACTIVE
    }

    public Branch() {}

    public Branch(Long businessId, String name, String city,
                  String country, boolean isMainBranch) {
        this.businessId   = businessId;
        this.name         = name;
        this.city         = city;
        this.country      = country;
        this.isMainBranch = isMainBranch;
        this.status       = Status.ACTIVE;
    }

    // Getters
    public Long getId()            { return id; }
    public Long getBusinessId()    { return businessId; }
    public String getName()        { return name; }
    public String getAddress()     { return address; }
    public String getPhone()       { return phone; }
    public String getCity()        { return city; }
    public String getCountry()     { return country; }
    public boolean isMainBranch()  { return isMainBranch; }
    public Status getStatus()      { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setBusinessId(Long businessId)   { this.businessId = businessId; }
    public void setName(String name)             { this.name = name; }
    public void setAddress(String address)       { this.address = address; }
    public void setPhone(String phone)           { this.phone = phone; }
    public void setCity(String city)             { this.city = city; }
    public void setCountry(String country)       { this.country = country; }
    public void setMainBranch(boolean main)      { this.isMainBranch = main; }
    public void setStatus(Status status)         { this.status = status; }
}