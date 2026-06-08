package com.business.managementsystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SaleDTO {

    private Long          id;
    private Long          productId;
    private String        productName;
    private double        quantitySold;
    private BigDecimal    priceAtSale;
    private BigDecimal    totalAmount;
    private LocalDateTime saleDate;

    // Receipt & branch fields
    private String receiptNumber;
    private Long   branchId;
    private String cashierName;
    private String paymentMethod;

    // Gold / Silver metadata
    private String unitType;
    private String purity;

    // Scrap purity — set per-transaction for scrap items
    private String scrapPurity;

    // Full constructor
    public SaleDTO(Long id, Long productId, String productName,
                   double quantitySold, BigDecimal priceAtSale,
                   BigDecimal totalAmount, LocalDateTime saleDate,
                   String receiptNumber, Long branchId,
                   String cashierName, String paymentMethod) {
        this.id            = id;
        this.productId     = productId;
        this.productName   = productName;
        this.quantitySold  = quantitySold;
        this.priceAtSale   = priceAtSale;
        this.totalAmount   = totalAmount;
        this.saleDate      = saleDate;
        this.receiptNumber = receiptNumber;
        this.branchId      = branchId;
        this.cashierName   = cashierName;
        this.paymentMethod = paymentMethod;
    }

    // Legacy constructor
    public SaleDTO(Long id, Long productId, String productName,
                   double quantitySold, BigDecimal priceAtSale,
                   BigDecimal totalAmount, LocalDateTime saleDate) {
        this(id, productId, productName, quantitySold,
                priceAtSale, totalAmount, saleDate,
                null, null, null, null);
    }

    // Getters
    public Long          getId()            { return id; }
    public Long          getProductId()     { return productId; }
    public String        getProductName()   { return productName; }
    public double        getQuantitySold()  { return quantitySold; }
    public BigDecimal    getPriceAtSale()   { return priceAtSale; }
    public BigDecimal    getTotalAmount()   { return totalAmount; }
    public LocalDateTime getSaleDate()      { return saleDate; }
    public String        getReceiptNumber() { return receiptNumber; }
    public Long          getBranchId()      { return branchId; }
    public String        getCashierName()   { return cashierName; }
    public String        getPaymentMethod() { return paymentMethod; }
    public String        getUnitType()      { return unitType; }
    public String        getPurity()        { return purity; }
    public String        getScrapPurity()   { return scrapPurity; }

    // Setters
    public void setId(Long id)                      { this.id = id; }
    public void setProductId(Long productId)        { this.productId = productId; }
    public void setProductName(String productName)  { this.productName = productName; }
    public void setQuantitySold(double quantitySold)   { this.quantitySold = quantitySold; }
    public void setPriceAtSale(BigDecimal p)        { this.priceAtSale = p; }
    public void setTotalAmount(BigDecimal t)        { this.totalAmount = t; }
    public void setSaleDate(LocalDateTime saleDate) { this.saleDate = saleDate; }
    public void setReceiptNumber(String r)          { this.receiptNumber = r; }
    public void setBranchId(Long branchId)          { this.branchId = branchId; }
    public void setCashierName(String cashierName)  { this.cashierName = cashierName; }
    public void setPaymentMethod(String m)          { this.paymentMethod = m; }
    public void setUnitType(String unitType)         { this.unitType = unitType; }
    public void setPurity(String purity)             { this.purity = purity; }
    public void setScrapPurity(String scrapPurity)   { this.scrapPurity = scrapPurity; }
}