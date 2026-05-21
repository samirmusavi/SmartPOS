package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Records which products are archived at which branch.
 * A product in this table is hidden from that branch's product list and POS.
 * The product remains active (product.active = true) and visible at other branches.
 *
 * This is separate from the global soft-delete (product.active = false)
 * which is used when an owner removes a product that has sales history.
 */
@Entity
@Table(name = "branch_product_archive",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"branch_id", "product_id"}))
public class BranchProductArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long branchId;

    @Column(nullable = false)
    private Long productId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime archivedAt;

    public BranchProductArchive() {}

    public BranchProductArchive(Long branchId, Long productId) {
        this.branchId  = branchId;
        this.productId = productId;
    }

    public Long getId()                     { return id; }
    public Long getBranchId()               { return branchId; }
    public Long getProductId()              { return productId; }
    public LocalDateTime getArchivedAt()    { return archivedAt; }

    public void setBranchId(Long v)         { this.branchId = v; }
    public void setProductId(Long v)        { this.productId = v; }
}