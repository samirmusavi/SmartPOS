package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "supply")
public class Supply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    // Unit of measurement — e.g. "pieces", "rolls", "boxes", "kg", "litres"
    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    private int quantity;

    // Alert threshold — when quantity drops to or below this, show as low stock
    @Column(nullable = false)
    private int minimumThreshold;

    // Cost per unit — for expense tracking awareness
    @Column(precision = 10, scale = 2)
    private BigDecimal costPerUnit;

    // Optional link to a supplier
    @Column
    private Long supplierId;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Supply() {}

    public Supply(Long businessId, String name, String category,
                  String unit, int quantity, int minimumThreshold,
                  BigDecimal costPerUnit, Long supplierId, String notes) {
        this.businessId       = businessId;
        this.name             = name;
        this.category         = category;
        this.unit             = unit;
        this.quantity         = quantity;
        this.minimumThreshold = minimumThreshold;
        this.costPerUnit      = costPerUnit;
        this.supplierId       = supplierId;
        this.notes            = notes;
    }

    public Long getId() { return id; }
    public Long getBusinessId() { return businessId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getMinimumThreshold() { return minimumThreshold; }
    public void setMinimumThreshold(int minimumThreshold) {
        this.minimumThreshold = minimumThreshold;
    }

    public BigDecimal getCostPerUnit() { return costPerUnit; }
    public void setCostPerUnit(BigDecimal costPerUnit) {
        this.costPerUnit = costPerUnit;
    }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Convenience — is this supply running low?
    public boolean isLow() { return quantity <= minimumThreshold; }
    public boolean isOut() { return quantity == 0; }
}