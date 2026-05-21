package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "supply_adjustment")
public class SupplyAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private Long supplyId;

    @Column(nullable = false)
    private String supplyName;

    @Column(nullable = false)
    private int oldQuantity;

    @Column(nullable = false)
    private int newQuantity;

    @Column(nullable = false)
    private int adjustment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private String adjustedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime adjustedAt;

    public enum Type {
        RECEIVED,  // New stock arrived from supplier
        USED,      // Stock consumed in operations
        MANUAL,    // Manual correction / count
        DAMAGED,   // Items found damaged/unusable
        OTHER
    }

    public SupplyAdjustment() {}

    public SupplyAdjustment(Long businessId, Long supplyId, String supplyName,
                            int oldQuantity, int newQuantity,
                            Type type, String notes, String adjustedBy) {
        this.businessId  = businessId;
        this.supplyId    = supplyId;
        this.supplyName  = supplyName;
        this.oldQuantity = oldQuantity;
        this.newQuantity = newQuantity;
        this.adjustment  = newQuantity - oldQuantity;
        this.type        = type;
        this.notes       = notes;
        this.adjustedBy  = adjustedBy;
    }

    public Long getId() { return id; }
    public Long getBusinessId() { return businessId; }
    public Long getSupplyId() { return supplyId; }
    public String getSupplyName() { return supplyName; }
    public int getOldQuantity() { return oldQuantity; }
    public int getNewQuantity() { return newQuantity; }
    public int getAdjustment() { return adjustment; }
    public Type getType() { return type; }
    public String getNotes() { return notes; }
    public String getAdjustedBy() { return adjustedBy; }
    public LocalDateTime getAdjustedAt() { return adjustedAt; }
}