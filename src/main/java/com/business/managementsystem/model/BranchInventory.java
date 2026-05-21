package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "branch_inventory",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"branch_id", "product_id"}))
public class BranchInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long branchId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private double quantity = 0;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public BranchInventory() {}

    public BranchInventory(Long branchId, Long productId, double quantity) {
        this.branchId  = branchId;
        this.productId = productId;
        this.quantity  = quantity;
    }

    public Long getId()            { return id; }
    public Long getBranchId()      { return branchId; }
    public Long getProductId()     { return productId; }
    public double getQuantity()       { return quantity; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setBranchId(Long branchId)   { this.branchId = branchId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public void setQuantity(double quantity)    { this.quantity = quantity; }
}