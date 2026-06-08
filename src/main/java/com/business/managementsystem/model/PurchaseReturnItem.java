package com.business.managementsystem.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_return_item")
public class PurchaseReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long purchaseReturnId;

    /** FK to purchase_item.id — null only for ad-hoc items */
    @Column
    private Long purchaseItemId;

    @Column
    private Long productId;

    @Column(length = 255)
    private String productName;

    @Column(length = 50)
    private String purity;

    /** PCS or GRAM */
    @Column(nullable = false, length = 10)
    private String unitType = "PCS";

    /** Pieces returned (for PCS) or grams returned (for GRAM) */
    @Column(nullable = false)
    private double returnQuantity = 0;

    /** Weight in grams removed from inventory (PCS items only) */
    @Column
    private Double returnWeightGrams;

    @Column(precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalReturnAmount;

    public PurchaseReturnItem() {}

    // ── Getters ───────────────────────────────────────────────
    public Long       getId()                { return id; }
    public Long       getPurchaseReturnId()  { return purchaseReturnId; }
    public Long       getPurchaseItemId()    { return purchaseItemId; }
    public Long       getProductId()         { return productId; }
    public String     getProductName()       { return productName; }
    public String     getPurity()            { return purity; }
    public String     getUnitType()          { return unitType; }
    public double     getReturnQuantity()    { return returnQuantity; }
    public Double     getReturnWeightGrams() { return returnWeightGrams; }
    public BigDecimal getUnitPrice()         { return unitPrice; }
    public BigDecimal getTotalReturnAmount() { return totalReturnAmount; }

    // ── Setters ───────────────────────────────────────────────
    public void setPurchaseReturnId(Long v)     { this.purchaseReturnId  = v; }
    public void setPurchaseItemId(Long v)       { this.purchaseItemId    = v; }
    public void setProductId(Long v)            { this.productId         = v; }
    public void setProductName(String v)        { this.productName       = v; }
    public void setPurity(String v)             { this.purity            = v; }
    public void setUnitType(String v)           { this.unitType          = v != null ? v : "PCS"; }
    public void setReturnQuantity(double v)     { this.returnQuantity    = v; }
    public void setReturnWeightGrams(Double v)  { this.returnWeightGrams = v; }
    public void setUnitPrice(BigDecimal v)      { this.unitPrice         = v; }
    public void setTotalReturnAmount(BigDecimal v){ this.totalReturnAmount = v; }
}
