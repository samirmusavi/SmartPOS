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

    // Total premium (or discount if negative) in AED for the whole transaction
    // Stored directly from the calculator so ledger can use the exact figure.
    @Column(name = "premium_amount", precision = 15, scale = 2)
    private BigDecimal premiumAmount = BigDecimal.ZERO;

    // Optional staff-applied rounding adjustment (positive = round up, negative = round down).
    // Baked into the final totalAmount at the time of sale.
    @Column(name = "round_off_amount", precision = 10, scale = 2)
    private BigDecimal roundOffAmount = BigDecimal.ZERO;

    // When true the printed tax invoice includes the UAE reverse-charge VAT declaration
    // (Cabinet Decision No. 127/2024). Always a MANUAL toggle — never auto-detected.
    @Column(name = "include_reverse_charge_declaration", columnDefinition = "boolean default false")
    private Boolean includeReverseChargeDeclaration = false;

    // ── Unfixed pricing fields ─────────────────────────────────────────────────────
    // Immutable record of how this transaction was priced at the counter.
    // FIXED_AT_TRADE   = normal sale — AED settled immediately (default for all existing rows)
    // UNFIXED_AT_TRADE = metal moves now; AED price deferred until a later Fixing event
    @Column(name = "original_pricing_method", nullable = false, length = 20)
    private String originalPricingMethod = "FIXED_AT_TRADE";

    // Tracks resolution progress of an UNFIXED_AT_TRADE transaction.
    // NOT_APPLICABLE = FIXED_AT_TRADE transaction (default)
    // OPEN           = unfixed; remaining open weight > 0
    // FULLY_FIXED    = all pure weight consumed by Fixing events
    @Column(name = "fixing_completion_status", nullable = false, length = 20)
    private String fixingCompletionStatus = "NOT_APPLICABLE";

    // Total gross weight in grams across all line items.
    @Column(name = "gross_weight_grams")
    private Double grossWeightGrams;

    // Pure gold weight = grossWeightGrams × (purity / 1000).
    // ALWAYS calculated server-side; never accepted from the frontend.
    @Column(name = "pure_weight_grams")
    private Double pureWeightGrams;

    // Per-oz USD premium/discount agreed at deal time.
    // For UNFIXED_AT_TRADE: locked permanently; reused at each subsequent Fixing event.
    @Column(name = "agreed_premium_discount", precision = 10, scale = 4)
    private BigDecimal agreedPremiumDiscount;

    // For UNFIXED_AT_TRADE: grams remaining to be priced.
    // Starts = pureWeightGrams; decremented by each Fixing event that consumes this sale.
    // Null for FIXED_AT_TRADE transactions.
    @Column(name = "remaining_open_weight_grams")
    private Double remainingOpenWeightGrams;

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
    public BigDecimal    getPremiumAmount()                  { return premiumAmount; }
    public BigDecimal    getRoundOffAmount()                 { return roundOffAmount; }
    public Boolean       getIncludeReverseChargeDeclaration() { return includeReverseChargeDeclaration; }
    public String        getOriginalPricingMethod()          { return originalPricingMethod; }
    public String        getFixingCompletionStatus()         { return fixingCompletionStatus; }
    public Double        getGrossWeightGrams()               { return grossWeightGrams; }
    public Double        getPureWeightGrams()                { return pureWeightGrams; }
    public BigDecimal    getAgreedPremiumDiscount()          { return agreedPremiumDiscount; }
    public Double        getRemainingOpenWeightGrams()       { return remainingOpenWeightGrams; }
    public LocalDateTime getCreatedAt()                      { return createdAt; }

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
    public void setPremiumAmount(BigDecimal v)                   { this.premiumAmount = v != null ? v : BigDecimal.ZERO; }
    public void setRoundOffAmount(BigDecimal v)                  { this.roundOffAmount = v != null ? v : BigDecimal.ZERO; }
    public void setIncludeReverseChargeDeclaration(Boolean v)   { this.includeReverseChargeDeclaration = v != null ? v : false; }
    public void setOriginalPricingMethod(String v)               { this.originalPricingMethod = v != null ? v : "FIXED_AT_TRADE"; }
    public void setFixingCompletionStatus(String v)              { this.fixingCompletionStatus = v != null ? v : "NOT_APPLICABLE"; }
    public void setGrossWeightGrams(Double v)                    { this.grossWeightGrams = v; }
    public void setPureWeightGrams(Double v)                     { this.pureWeightGrams = v; }
    public void setAgreedPremiumDiscount(BigDecimal v)           { this.agreedPremiumDiscount = v; }
    public void setRemainingOpenWeightGrams(Double v)            { this.remainingOpenWeightGrams = v; }
}