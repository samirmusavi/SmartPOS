package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks supply quantity per branch — same concept as BranchInventory for products.
 * The supply catalog (Supply table) is shared across all branches.
 * Each branch has its own quantity of each supply in this table.
 * supply.quantity is kept as the global total (sum across all branches).
 */
@Entity
@Table(name = "branch_supply_inventory",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"branch_id", "supply_id"}))
public class BranchSupplyInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long branchId;

    @Column(nullable = false)
    private Long supplyId;

    @Column(nullable = false)
    private int quantity = 0;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public BranchSupplyInventory() {}

    public BranchSupplyInventory(Long branchId, Long supplyId, int quantity) {
        this.branchId = branchId;
        this.supplyId = supplyId;
        this.quantity = quantity;
    }

    public Long getId()                  { return id; }
    public Long getBranchId()            { return branchId; }
    public Long getSupplyId()            { return supplyId; }
    public int getQuantity()             { return quantity; }
    public LocalDateTime getUpdatedAt()  { return updatedAt; }

    public void setBranchId(Long v)      { this.branchId = v; }
    public void setSupplyId(Long v)      { this.supplyId = v; }
    public void setQuantity(int v)       { this.quantity = v; }
}