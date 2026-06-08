package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
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

    // ── Party type flags ────────────────────────────────────────────
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean isSupplier = true;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isCustomer = false;

    // ── KYC fields ──────────────────────────────────────────────────
    @Column(length = 20, columnDefinition = "varchar(20) default 'NOT_VERIFIED'")
    private String kycStatus = "NOT_VERIFIED";

    @Column(length = 50)
    private String emiratesId;

    @Column(length = 50)
    private String passportNumber;

    @Column(length = 50)
    private String tradeLicenseNumber;

    @Column
    private LocalDate idExpiryDate;

    @Column(length = 500)
    private String kycNotes;

    /**
     * Short auto-generated code for fast lookup and display on statements.
     * Format: {INITIALS}{3-digit number}, e.g. AM001, SG003.
     * Unique per business (enforced in SupplierService — not a DB unique constraint
     * because uniqueness is scoped to businessId, not globally).
     */
    @Column(name = "party_code", length = 20)
    private String partyCode;

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

    public boolean isSupplier() { return isSupplier; }
    public void setSupplier(boolean supplier) { isSupplier = supplier; }

    public boolean isCustomer() { return isCustomer; }
    public void setCustomer(boolean customer) { isCustomer = customer; }

    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }

    public String getEmiratesId() { return emiratesId; }
    public void setEmiratesId(String emiratesId) { this.emiratesId = emiratesId; }

    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

    public String getTradeLicenseNumber() { return tradeLicenseNumber; }
    public void setTradeLicenseNumber(String tradeLicenseNumber) { this.tradeLicenseNumber = tradeLicenseNumber; }

    public LocalDate getIdExpiryDate() { return idExpiryDate; }
    public void setIdExpiryDate(LocalDate idExpiryDate) { this.idExpiryDate = idExpiryDate; }

    public String getKycNotes() { return kycNotes; }
    public void setKycNotes(String kycNotes) { this.kycNotes = kycNotes; }

    public String getPartyCode() { return partyCode; }
    public void setPartyCode(String partyCode) { this.partyCode = partyCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}