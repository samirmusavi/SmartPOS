package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sale_transaction")
public class SaleTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @Column
    private Long branchId;

    @Column
    private Long customerId;

    @Column
    private String customerName;

    // Unique receipt number — format: {BRANCH_CODE}-{YEAR}-{SEQUENCE}
    // e.g. MAI-2026-00001
    @Column(unique = true)
    private String receiptNumber;

    // Who processed the sale
    @Column
    private String cashierName;

    // Cash / Card / Bank Transfer
    @Column
    private String paymentMethod;

    // Pre-VAT subtotal
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    // VAT amount (5%)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    // Total charged to customer (subtotal + VAT)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // How much cash was received (for cash payments)
    @Column(precision = 10, scale = 2)
    private BigDecimal amountReceived;

    // Change given back
    @Column(precision = 10, scale = 2)
    private BigDecimal changeGiven;

    // Discount amount applied to this transaction
    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // Amount still owed by customer (total - amountReceived)
    @Column(precision = 10, scale = 2)
    private BigDecimal dueAmount = BigDecimal.ZERO;

    // VAT percentage used for this transaction (default 5%)
    @Column(nullable = false)
    private double vatPercent = 5.0;

    // Payment status: FULLY_PAID, PARTIALLY_PAID
    @Column(nullable = false)
    private String status = "FULLY_PAID";

    // USD→AED exchange rate at time of sale
    @Column(precision = 10, scale = 4)
    private BigDecimal exchangeRate;

    // Pricing method used: OZ_RATE or PER_GRAM or FIXED (standard product price)
    @Column
    private String pricingMethod;

    // Gold Oz Rate (USD) at time of sale
    @Column(precision = 10, scale = 2)
    private BigDecimal goldOzRate;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public SaleTransaction() {}

    public Long          getId()             { return id; }
    public Long          getBusinessId()     { return businessId; }
    public Long          getBranchId()       { return branchId; }
    public Long          getCustomerId()     { return customerId; }
    public String        getCustomerName()   { return customerName; }
    public String        getReceiptNumber()  { return receiptNumber; }
    public String        getCashierName()    { return cashierName; }
    public String        getPaymentMethod()  { return paymentMethod; }
    public BigDecimal    getSubtotal()       { return subtotal; }
    public BigDecimal    getVatAmount()      { return vatAmount; }
    public BigDecimal    getTotalAmount()    { return totalAmount; }
    public BigDecimal    getAmountReceived() { return amountReceived; }
    public BigDecimal    getChangeGiven()    { return changeGiven; }
    public BigDecimal    getDiscountAmount() { return discountAmount; }
    public BigDecimal    getDueAmount()      { return dueAmount; }
    public double        getVatPercent()     { return vatPercent; }
    public String        getStatus()         { return status; }
    public BigDecimal    getExchangeRate()   { return exchangeRate; }
    public String        getPricingMethod()  { return pricingMethod; }
    public BigDecimal    getGoldOzRate()     { return goldOzRate; }
    public LocalDateTime getCreatedAt()      { return createdAt; }

    // Setters
    public void setBusinessId(Long v)         { this.businessId = v; }
    public void setBranchId(Long v)           { this.branchId = v; }
    public void setCustomerId(Long v)         { this.customerId = v; }
    public void setCustomerName(String v)     { this.customerName = v; }
    public void setReceiptNumber(String v)    { this.receiptNumber = v; }
    public void setCashierName(String v)      { this.cashierName = v; }
    public void setPaymentMethod(String v)    { this.paymentMethod = v; }
    public void setSubtotal(BigDecimal v)     { this.subtotal = v; }
    public void setVatAmount(BigDecimal v)    { this.vatAmount = v; }
    public void setTotalAmount(BigDecimal v)  { this.totalAmount = v; }
    public void setAmountReceived(BigDecimal v){ this.amountReceived = v; }
    public void setChangeGiven(BigDecimal v)  { this.changeGiven = v; }
    public void setDiscountAmount(BigDecimal v){ this.discountAmount = v; }
    public void setDueAmount(BigDecimal v)     { this.dueAmount = v; }
    public void setVatPercent(double v)        { this.vatPercent = v; }
    public void setStatus(String v)            { this.status = v; }
    public void setExchangeRate(BigDecimal v)  { this.exchangeRate = v; }
    public void setPricingMethod(String v)     { this.pricingMethod = v; }
    public void setGoldOzRate(BigDecimal v)    { this.goldOzRate = v; }
}