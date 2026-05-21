package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_supply_template",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"product_id", "supply_id"}))
public class ProductSupplyTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_id", nullable = false)
    private Supply supply;

    // How many units of this supply are used per 1 unit sold of the product
    @Column(nullable = false)
    private double quantityPerUnit;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public ProductSupplyTemplate() {}

    public ProductSupplyTemplate(Long businessId, Product product,
                                 Supply supply, double quantityPerUnit) {
        this.businessId      = businessId;
        this.product         = product;
        this.supply          = supply;
        this.quantityPerUnit = quantityPerUnit;
    }

    public Long getId() { return id; }
    public Long getBusinessId() { return businessId; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Supply getSupply() { return supply; }
    public void setSupply(Supply supply) { this.supply = supply; }

    public double getQuantityPerUnit() { return quantityPerUnit; }
    public void setQuantityPerUnit(double quantityPerUnit) {
        this.quantityPerUnit = quantityPerUnit;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
}