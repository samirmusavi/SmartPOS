package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_item")
public class PurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long purchaseId;

    /** Null when item is ad-hoc (not an existing product) */
    @Column
    private Long productId;

    @Column(nullable = false, length = 255)
    private String productName;

    @Column(length = 50)
    private String purity;

    /** PCS or GRAM */
    @Column(nullable = false, length = 10)
    private String unitType = "PCS";

    /** Number of pieces (always set; for GRAM items this equals weightGrams) */
    @Column(nullable = false)
    private double quantity = 0;

    /** Weight in grams — null for PCS items */
    @Column
    private Double weightGrams;

    /** OZ_RATE | PER_GRAM | FIXED_PRICE */
    @Column(nullable = false, length = 20)
    private String pricingMethod = "FIXED_PRICE";

    /** Gold/Silver oz rate in USD — null unless pricingMethod = OZ_RATE */
    @Column(precision = 10, scale = 2)
    private BigDecimal goldOzRate;

    /** Price per gram in USD — null unless pricingMethod = PER_GRAM */
    @Column(precision = 10, scale = 4)
    private BigDecimal pricePerGram;

    /** Unit price in AED (final, after conversion) */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /** totalPrice = unitPrice × quantity (or weight) in AED */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    /**
     * For scrap items: the tested purity entered at transaction time
     * (overrides product.purity for this purchase only).
     * e.g. "634.5", "521.0"
     */
    @Column(length = 20)
    private String scrapPurity;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public PurchaseItem() {}

    // ── Getters ───────────────────────────────────────────────
    public Long          getId()            { return id; }
    public Long          getPurchaseId()    { return purchaseId; }
    public Long          getProductId()     { return productId; }
    public String        getProductName()   { return productName; }
    public String        getPurity()        { return purity; }
    public String        getUnitType()      { return unitType; }
    public double        getQuantity()      { return quantity; }
    public Double        getWeightGrams()   { return weightGrams; }
    public String        getPricingMethod() { return pricingMethod; }
    public BigDecimal    getGoldOzRate()    { return goldOzRate; }
    public BigDecimal    getPricePerGram()  { return pricePerGram; }
    public BigDecimal    getUnitPrice()     { return unitPrice; }
    public BigDecimal    getTotalPrice()    { return totalPrice; }
    public String        getScrapPurity()   { return scrapPurity; }
    public LocalDateTime getCreatedAt()     { return createdAt; }

    // ── Setters ───────────────────────────────────────────────
    public void setPurchaseId(Long v)          { this.purchaseId    = v; }
    public void setProductId(Long v)           { this.productId     = v; }
    public void setProductName(String v)       { this.productName   = v; }
    public void setPurity(String v)            { this.purity        = v; }
    public void setUnitType(String v)          { this.unitType      = v != null ? v : "PCS"; }
    public void setQuantity(double v)          { this.quantity      = v; }
    public void setWeightGrams(Double v)       { this.weightGrams   = v; }
    public void setPricingMethod(String v)     { this.pricingMethod = v != null ? v : "FIXED_PRICE"; }
    public void setGoldOzRate(BigDecimal v)    { this.goldOzRate    = v; }
    public void setPricePerGram(BigDecimal v)  { this.pricePerGram  = v; }
    public void setUnitPrice(BigDecimal v)     { this.unitPrice     = v != null ? v : BigDecimal.ZERO; }
    public void setTotalPrice(BigDecimal v)    { this.totalPrice    = v != null ? v : BigDecimal.ZERO; }
    public void setScrapPurity(String v)       { this.scrapPurity   = v; }
}
