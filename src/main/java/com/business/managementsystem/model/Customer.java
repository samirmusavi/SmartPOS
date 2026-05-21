package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private String fullName;

    @Column
    private String phone;

    @Column
    private String email;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private int loyaltyPoints = 0;

    // Total amount spent across all sales linked to this customer
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;

    // Number of times this customer has made a purchase
    @Column(nullable = false)
    private int visitCount = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Last time they made a purchase
    private LocalDateTime lastVisitAt;

    // Optional link to a supplier record — set when this customer is also a supplier
    @Column
    private Long linkedSupplierId;

    public Customer() {}

    public Customer(Long businessId, String fullName,
                    String phone, String email, String notes) {
        this.businessId = businessId;
        this.fullName   = fullName;
        this.phone      = phone;
        this.email      = email;
        this.notes      = notes;
        this.totalSpent = BigDecimal.ZERO;
        this.visitCount = 0;
        this.loyaltyPoints = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }

    public int getVisitCount() { return visitCount; }
    public void setVisitCount(int visitCount) { this.visitCount = visitCount; }

    public int getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(int loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public LocalDateTime getLastVisitAt() { return lastVisitAt; }
    public void setLastVisitAt(LocalDateTime lastVisitAt) { this.lastVisitAt = lastVisitAt; }

    public Long getLinkedSupplierId() { return linkedSupplierId; }
    public void setLinkedSupplierId(Long linkedSupplierId) { this.linkedSupplierId = linkedSupplierId; }

    // Called when a sale is linked to this customer
    public void recordPurchase(BigDecimal amount, int pointsEarned) {
        this.totalSpent  = this.totalSpent.add(amount);
        this.visitCount  = this.visitCount + 1;
        this.loyaltyPoints = this.loyaltyPoints + pointsEarned;
        this.lastVisitAt = LocalDateTime.now();
    }
}