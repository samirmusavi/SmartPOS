package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks inter-branch supply transfers.
 * Same pattern as product StockTransfer, but for supplies.
 *
 * Flow:
 * 1. PENDING  — stock deducted from source branch_supply_inventory immediately
 * 2. COMPLETED — stock added to destination branch_supply_inventory on confirmation
 * 3. CANCELLED — stock returned to source branch_supply_inventory
 *
 * supply.quantity (global total) is re-synced after every state transition.
 */
@Entity
@Table(name = "supply_transfer")
public class SupplyTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private Long fromBranchId;

    @Column(nullable = false)
    private Long toBranchId;

    @Column(nullable = false)
    private Long supplyId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    private String notes;

    @Column(length = 100)
    private String createdBy;

    @Column(length = 100)
    private String completedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    public enum Status {
        PENDING, COMPLETED, CANCELLED
    }

    public SupplyTransfer() {}

    public SupplyTransfer(Long businessId, Long fromBranchId, Long toBranchId,
                          Long supplyId, int quantity,
                          String notes, String createdBy) {
        this.businessId   = businessId;
        this.fromBranchId = fromBranchId;
        this.toBranchId   = toBranchId;
        this.supplyId     = supplyId;
        this.quantity      = quantity;
        this.notes         = notes;
        this.createdBy     = createdBy;
    }

    // ── Getters ──
    public Long getId()                    { return id; }
    public Long getBusinessId()            { return businessId; }
    public Long getFromBranchId()          { return fromBranchId; }
    public Long getToBranchId()            { return toBranchId; }
    public Long getSupplyId()              { return supplyId; }
    public int getQuantity()               { return quantity; }
    public Status getStatus()              { return status; }
    public String getNotes()               { return notes; }
    public String getCreatedBy()           { return createdBy; }
    public String getCompletedBy()         { return completedBy; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public LocalDateTime getCompletedAt()  { return completedAt; }

    // ── Setters ──
    public void setStatus(Status v)        { this.status = v; }
    public void setCompletedBy(String v)   { this.completedBy = v; }
    public void setCompletedAt(LocalDateTime v) { this.completedAt = v; }
}
