package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "branch_price_override",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"branch_id", "product_id"}))
public class BranchPriceOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long branchId;

    @Column(nullable = false)
    private Long productId;

    // Override price — used when branch is in a different country
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public BranchPriceOverride() {}

    public BranchPriceOverride(Long branchId, Long productId,
                               BigDecimal price) {
        this.branchId  = branchId;
        this.productId = productId;
        this.price     = price;
    }

    public Long getId()            { return id; }
    public Long getBranchId()      { return branchId; }
    public Long getProductId()     { return productId; }
    public BigDecimal getPrice()   { return price; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setBranchId(Long branchId)   { this.branchId = branchId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public void setPrice(BigDecimal price)   { this.price = price; }
}