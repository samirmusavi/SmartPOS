package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sale")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    // Links this sale item back to its parent transaction
    // All items sold in one checkout share the same transactionId
    @Column
    private Long transactionId;

    // Branch this sale was made at
    // Null for legacy data created before multi-branch
    @Column
    private Long branchId;

    // Unique receipt number — format: {BRANCH_CODE}-{YEAR}-{SEQ}
    // e.g. DXB-2026-00001
    // Null for legacy sales before receipt system was added
    // Receipt number — shared across all items in the same transaction.
    // NOT unique here — uniqueness is enforced on sale_transaction only.
    @Column
    private String receiptNumber;

    // Who processed this sale
    @Column
    private String cashierName;

    // Payment method: Cash, Card, Bank Transfer
    @Column
    private String paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private double quantitySold;

    // Unit type at time of sale (PCS or GRAM)
    @Column
    private String unitType;

    // Purity at time of sale (e.g. "750", "999.9")
    @Column
    private String purity;

    // Scrap purity entered per-transaction for scrap items (e.g. "750", "916")
    @Column
    private String scrapPurity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtSale;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costAtSale;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime saleDate;

    public Sale() {}

    public Sale(Long businessId, Product product,
                double quantitySold, BigDecimal priceAtSale) {
        this.businessId   = businessId;
        this.product      = product;
        this.quantitySold = quantitySold;
        this.priceAtSale  = priceAtSale;
        this.costAtSale   = product.getCostPrice() != null
                ? product.getCostPrice()
                : BigDecimal.ZERO;
        this.totalAmount  = priceAtSale.multiply(
                BigDecimal.valueOf(quantitySold));
        this.unitType     = product.getUnitType();
        this.purity       = product.getPurity();
    }

    // Getters
    public Long getId()              { return id; }
    public Long getBusinessId()      { return businessId; }
    public Long getTransactionId()   { return transactionId; }
    public Long getBranchId()        { return branchId; }
    public String getReceiptNumber(){ return receiptNumber; }
    public String getCashierName()  { return cashierName; }
    public String getPaymentMethod(){ return paymentMethod; }
    public Product getProduct()     { return product; }
    public double getQuantitySold()    { return quantitySold; }
    public String getUnitType()        { return unitType; }
    public String getPurity()          { return purity; }
    public String getScrapPurity()     { return scrapPurity; }
    public BigDecimal getPriceAtSale()  { return priceAtSale; }
    public BigDecimal getCostAtSale()   { return costAtSale; }
    public BigDecimal getTotalAmount()  { return totalAmount; }
    public LocalDateTime getSaleDate()  { return saleDate; }

    // Setters
    public void setId(Long id)                      { this.id = id; }
    public void setBusinessId(Long businessId)      { this.businessId = businessId; }
    public void setTransactionId(Long v)            { this.transactionId = v; }
    public void setBranchId(Long branchId)          { this.branchId = branchId; }
    public void setReceiptNumber(String r)          { this.receiptNumber = r; }
    public void setCashierName(String cashierName)  { this.cashierName = cashierName; }
    public void setPaymentMethod(String method)     { this.paymentMethod = method; }
    public void setProduct(Product product)         { this.product = product; }
    public void setQuantitySold(double q)            { this.quantitySold = q; }
    public void setUnitType(String unitType)         { this.unitType = unitType; }
    public void setPurity(String purity)             { this.purity = purity; }
    public void setScrapPurity(String scrapPurity)   { this.scrapPurity = scrapPurity; }
    public void setPriceAtSale(BigDecimal p)        { this.priceAtSale = p; }
    public void setCostAtSale(BigDecimal c)         { this.costAtSale = c; }
    public void setTotalAmount(BigDecimal t)        { this.totalAmount = t; }
}