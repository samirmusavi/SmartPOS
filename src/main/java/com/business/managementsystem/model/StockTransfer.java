package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transfer")
public class StockTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private Long fromBranchId;

    @Column(nullable = false)
    private String fromBranchName;

    @Column(nullable = false)
    private Long toBranchId;

    @Column(nullable = false)
    private String toBranchName;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private double quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private String createdBy;

    @Column
    private String completedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING,    // Transfer requested, not yet confirmed by receiving branch
        COMPLETED,  // Stock moved — inventories updated
        CANCELLED   // Transfer cancelled
    }

    public StockTransfer() {}

    // Getters
    public Long getId()              { return id; }
    public Long getBusinessId()      { return businessId; }
    public Long getFromBranchId()    { return fromBranchId; }
    public String getFromBranchName(){ return fromBranchName; }
    public Long getToBranchId()      { return toBranchId; }
    public String getToBranchName()  { return toBranchName; }
    public Long getProductId()       { return productId; }
    public String getProductName()   { return productName; }
    public double getQuantity()         { return quantity; }
    public Status getStatus()        { return status; }
    public String getNotes()         { return notes; }
    public String getCreatedBy()     { return createdBy; }
    public String getCompletedBy()   { return completedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setBusinessId(Long businessId)        { this.businessId = businessId; }
    public void setFromBranchId(Long fromBranchId)    { this.fromBranchId = fromBranchId; }
    public void setFromBranchName(String name)        { this.fromBranchName = name; }
    public void setToBranchId(Long toBranchId)        { this.toBranchId = toBranchId; }
    public void setToBranchName(String name)          { this.toBranchName = name; }
    public void setProductId(Long productId)          { this.productId = productId; }
    public void setProductName(String productName)    { this.productName = productName; }
    public void setQuantity(double quantity)             { this.quantity = quantity; }
    public void setStatus(Status status)              { this.status = status; }
    public void setNotes(String notes)                { this.notes = notes; }
    public void setCreatedBy(String createdBy)        { this.createdBy = createdBy; }
    public void setCompletedBy(String completedBy)    { this.completedBy = completedBy; }
}