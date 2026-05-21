package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_adjustment")
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    // Which branch this adjustment belongs to.
    // Null for legacy adjustments recorded before multi-branch.
    @Column
    private Long branchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private double oldQuantity;

    @Column(nullable = false)
    private double newQuantity;

    // The difference — positive = stock added, negative = stock removed
    @Column(nullable = false)
    private double adjustment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Reason reason;

    @Column(length = 500)
    private String notes;

    // Who made the adjustment (user full name)
    @Column(nullable = false)
    private String adjustedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime adjustedAt;

    public enum Reason {
        MANUAL_COUNT,    // Physical count correction
        DAMAGED,         // Items found damaged
        STOLEN,          // Theft / shrinkage
        SUPPLIER_RETURN, // Returned to supplier
        RECEIVED_STOCK,  // New stock received
        OTHER
    }

    public StockAdjustment() {}

    public StockAdjustment(Long businessId, Long branchId, Product product,
                           double oldQuantity, double newQuantity,
                           Reason reason, String notes,
                           String adjustedBy) {
        this.businessId  = businessId;
        this.branchId    = branchId;
        this.product     = product;
        this.oldQuantity = oldQuantity;
        this.newQuantity = newQuantity;
        this.adjustment  = newQuantity - oldQuantity;
        this.reason      = reason;
        this.notes       = notes;
        this.adjustedBy  = adjustedBy;
    }

    // Legacy constructor — no branchId
    public StockAdjustment(Long businessId, Product product,
                           double oldQuantity, double newQuantity,
                           Reason reason, String notes,
                           String adjustedBy) {
        this(businessId, null, product, oldQuantity, newQuantity,
                reason, notes, adjustedBy);
    }

    public Long getId()                  { return id; }
    public Long getBusinessId()          { return businessId; }
    public Long getBranchId()            { return branchId; }
    public Product getProduct()          { return product; }
    public double getOldQuantity()          { return oldQuantity; }
    public double getNewQuantity()          { return newQuantity; }
    public double getAdjustment()           { return adjustment; }
    public Reason getReason()            { return reason; }
    public String getNotes()             { return notes; }
    public String getAdjustedBy()        { return adjustedBy; }
    public LocalDateTime getAdjustedAt() { return adjustedAt; }
}