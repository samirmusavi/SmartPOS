package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "business")
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedDate;

    private LocalDateTime expiryDate;

    // Customizer config — MEDIUMTEXT supports up to 16 MB (needed for base64-encoded logos)
    @Column(columnDefinition = "MEDIUMTEXT")
    private String receiptLogoBase64;

    private String receiptFooter;

    // Enums
    public enum Plan {
        BASIC, BUSINESS, ENTERPRISE
    }

    public enum Status {
        ACTIVE, INACTIVE, SUSPENDED
    }

    // Constructors
    public Business() {}

    public Business(String businessName, String ownerName, String email,
                    String phone, Plan plan) {
        this.businessName = businessName;
        this.ownerName    = ownerName;
        this.email        = email;
        this.phone        = phone;
        this.plan         = plan;
        this.status       = Status.ACTIVE;
        this.expiryDate   = null; // no expiry — subscription managed separately
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getJoinedDate() { return joinedDate; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public String getReceiptLogoBase64() { return receiptLogoBase64; }
    public void setReceiptLogoBase64(String receiptLogoBase64) { this.receiptLogoBase64 = receiptLogoBase64; }

    public String getReceiptFooter() { return receiptFooter; }
    public void setReceiptFooter(String receiptFooter) { this.receiptFooter = receiptFooter; }
}
