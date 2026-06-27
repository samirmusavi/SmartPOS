package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String barcode;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal costPrice;

    @Column(nullable = false)
    private double quantity;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private boolean active = true;

    // Unit type: PCS (pieces) or GRAM (weight in grams)
    @Column(nullable = false)
    private String unitType = "PCS";

    // Purity e.g. "750", "995", "999.9" — nullable
    @Column
    private String purity;

    // Optional link to a supplier — nullable
    @Column
    private Long supplierId;

    // Relative URL path to the product image, e.g. /uploads/products/1/42_1712345678.jpg
    @Column
    private String imagePath;

    /** Product class: "BULLION" (investment-grade bars/coins) or "JEWELLERY" (fabricated pieces).
     *  Used for product organization and filtering only — does NOT drive VAT logic. */
    @Column(name = "product_class", nullable = false)
    private String productClass = "JEWELLERY";

    /** True for scrap gold/silver items — purity entered per-transaction */
    @Column(nullable = false)
    private boolean isScrap = false;

    /** Global total weight of all pieces in grams — synced from branch_inventory sums.
     *  Null for GRAM-unit products (weight = quantity there). */
    @Column
    private Double totalWeightGrams;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Product() {}

    public Product(Long businessId, String name, String barcode,
                   BigDecimal price, BigDecimal costPrice,
                   double quantity, String category) {
        this.businessId = businessId;
        this.name       = name;
        this.barcode    = barcode;
        this.price      = price;
        this.costPrice  = costPrice;
        this.quantity   = quantity;
        this.category   = category;
        this.active     = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType != null ? unitType : "PCS"; }

    public String getPurity() { return purity; }
    public void setPurity(String purity) { this.purity = purity; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getProductClass() { return productClass; }
    public void setProductClass(String productClass) { this.productClass = productClass != null ? productClass : "JEWELLERY"; }

    public boolean isScrap() { return isScrap; }
    public void setScrap(boolean scrap) { this.isScrap = scrap; }

    public Double getTotalWeightGrams() { return totalWeightGrams; }
    public void setTotalWeightGrams(Double totalWeightGrams) { this.totalWeightGrams = totalWeightGrams; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
