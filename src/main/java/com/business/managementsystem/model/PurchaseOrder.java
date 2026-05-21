package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    // Which branch placed this order — stock goes here on receipt
    // Null for legacy orders created before multi-branch
    @Column
    private Long branchId;

    // Which supplier this order is from
    @Column(nullable = false)
    private Long supplierId;

    @Column(nullable = false)
    private String supplierName;

    // Which product is being restocked
    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private double quantityOrdered;

    // Filled in when received — may differ from ordered
    @Column
    private Double quantityReceived;

    // Cost per unit agreed with supplier
    @Column(precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column
    private LocalDate expectedDelivery;

    @Column
    private LocalDate receivedDate;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Status {
        DRAFT,     // Auto-generated draft — not yet sent to supplier
        PENDING,   // Order placed, waiting for delivery
        RECEIVED,  // Stock arrived, inventory updated
        CANCELLED  // Order cancelled
    }

    public PurchaseOrder() {}

    // Getters
    public Long getId() { return id; }
    public Long getBusinessId() { return businessId; }
    public Long getBranchId() { return branchId; }
    public Long getSupplierId() { return supplierId; }
    public String getSupplierName() { return supplierName; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public double getQuantityOrdered() { return quantityOrdered; }
    public Double getQuantityReceived() { return quantityReceived; }
    public BigDecimal getUnitCost() { return unitCost; }
    public BigDecimal getTotalCost() { return totalCost; }
    public Status getStatus() { return status; }
    public LocalDate getExpectedDelivery() { return expectedDelivery; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public String getNotes() { return notes; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public void setProductId(Long productId) { this.productId = productId; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setQuantityOrdered(double quantityOrdered) { this.quantityOrdered = quantityOrdered; }
    public void setQuantityReceived(Double quantityReceived) { this.quantityReceived = quantityReceived; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public void setStatus(Status status) { this.status = status; }
    public void setExpectedDelivery(LocalDate expectedDelivery) { this.expectedDelivery = expectedDelivery; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}