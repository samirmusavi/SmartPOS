package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private Long branchId;

    /** Optional FK to supplier table — null when free-text supplier used */
    @Column
    private Long supplierId;

    /** Free-text supplier name — always populated (copied from supplier or typed) */
    @Column(length = 255)
    private String supplierName;

    /** Format: PUR-{BRANCHCODE}-{YYYYMMDD}-{SEQ} */
    @Column(nullable = false, unique = true, length = 100)
    private String invoiceNumber;

    @Column(nullable = false)
    private LocalDateTime purchaseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private Status status = Status.FULLY_PAID;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal dueAmount = BigDecimal.ZERO;

    /** USD→AED exchange rate used for this purchase */
    @Column(precision = 10, scale = 4)
    private BigDecimal exchangeRate = new BigDecimal("3.6740");

    @Column(length = 1000)
    private String notes;

    // ── Gold Exchange payment details ──────────────────────────
    @Column(length = 50)
    private String goldPaymentPurity;

    @Column
    private Double goldPaymentWeightGrams;

    @Column(precision = 10, scale = 2)
    private BigDecimal goldPaymentOzRate;

    @Column(precision = 15, scale = 2)
    private BigDecimal goldPaymentValue;

    @Column(precision = 15, scale = 2)
    private BigDecimal cashAmountPaid;

    /** Total premium in AED for this purchase (sum of per-item premiums from the calculator) */
    @Column(name = "premium_amount", precision = 15, scale = 2)
    private BigDecimal premiumAmount = BigDecimal.ZERO;

    /** Total discount in AED for this purchase (sum of per-item discounts from the calculator) */
    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** Optional staff-applied rounding adjustment baked into the final totalAmount at purchase time */
    @Column(name = "round_off_amount", precision = 10, scale = 2)
    private BigDecimal roundOffAmount = BigDecimal.ZERO;

    // ── Unfixed pricing fields ─────────────────────────────────────────────────────
    // FIXED_AT_TRADE   = normal purchase — AED settled immediately (default for all existing rows)
    // UNFIXED_AT_TRADE = metal received now; AED price deferred until a later Fixing event
    @Column(name = "original_pricing_method", nullable = false, length = 20)
    private String originalPricingMethod = "FIXED_AT_TRADE";

    // NOT_APPLICABLE = FIXED_AT_TRADE (default)
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
    // Starts = pureWeightGrams; decremented by each Fixing event that consumes this purchase.
    // Null for FIXED_AT_TRADE transactions.
    @Column(name = "remaining_open_weight_grams")
    private Double remainingOpenWeightGrams;

    @Column(length = 255)
    private String createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── Enums ──────────────────────────────────────────────────
    public enum PaymentMethod {
        CASH,
        BANK_TRANSFER,
        CHEQUE,
        GOLD_EXCHANGE,
        CREDIT
    }

    public enum Status {
        FULLY_PAID,
        PARTIALLY_PAID,
        DUE,
        RETURNED
    }

    public Purchase() {}

    // ── Getters ───────────────────────────────────────────────
    public Long             getId()             { return id; }
    public Long             getBusinessId()     { return businessId; }
    public Long             getBranchId()       { return branchId; }
    public Long             getSupplierId()     { return supplierId; }
    public String           getSupplierName()   { return supplierName; }
    public String           getInvoiceNumber()  { return invoiceNumber; }
    public LocalDateTime    getPurchaseDate()   { return purchaseDate; }
    public PaymentMethod    getPaymentMethod()  { return paymentMethod; }
    public Status           getStatus()         { return status; }
    public BigDecimal       getTotalAmount()    { return totalAmount; }
    public BigDecimal       getAmountPaid()     { return amountPaid; }
    public BigDecimal       getDueAmount()      { return dueAmount; }
    public BigDecimal       getExchangeRate()   { return exchangeRate; }
    public String           getNotes()                  { return notes; }
    public String           getGoldPaymentPurity()      { return goldPaymentPurity; }
    public Double           getGoldPaymentWeightGrams() { return goldPaymentWeightGrams; }
    public BigDecimal       getGoldPaymentOzRate()      { return goldPaymentOzRate; }
    public BigDecimal       getGoldPaymentValue()       { return goldPaymentValue; }
    public BigDecimal       getCashAmountPaid()         { return cashAmountPaid; }
    public BigDecimal       getPremiumAmount()            { return premiumAmount; }
    public BigDecimal       getDiscountAmount()           { return discountAmount; }
    public BigDecimal       getRoundOffAmount()           { return roundOffAmount; }
    public String           getOriginalPricingMethod()    { return originalPricingMethod; }
    public String           getFixingCompletionStatus()   { return fixingCompletionStatus; }
    public Double           getGrossWeightGrams()         { return grossWeightGrams; }
    public Double           getPureWeightGrams()          { return pureWeightGrams; }
    public BigDecimal       getAgreedPremiumDiscount()    { return agreedPremiumDiscount; }
    public Double           getRemainingOpenWeightGrams() { return remainingOpenWeightGrams; }
    public String           getCreatedBy()                { return createdBy; }
    public LocalDateTime    getCreatedAt()                { return createdAt; }
    public LocalDateTime    getUpdatedAt()                { return updatedAt; }

    // ── Setters ───────────────────────────────────────────────
    public void setBusinessId(Long v)          { this.businessId    = v; }
    public void setBranchId(Long v)            { this.branchId      = v; }
    public void setSupplierId(Long v)          { this.supplierId    = v; }
    public void setSupplierName(String v)      { this.supplierName  = v; }
    public void setInvoiceNumber(String v)     { this.invoiceNumber = v; }
    public void setPurchaseDate(LocalDateTime v){ this.purchaseDate  = v; }
    public void setPaymentMethod(PaymentMethod v){ this.paymentMethod = v; }
    public void setStatus(Status v)            { this.status        = v; }
    public void setTotalAmount(BigDecimal v)   { this.totalAmount   = v != null ? v : BigDecimal.ZERO; }
    public void setAmountPaid(BigDecimal v)    { this.amountPaid    = v != null ? v : BigDecimal.ZERO; }
    public void setDueAmount(BigDecimal v)     { this.dueAmount     = v != null ? v : BigDecimal.ZERO; }
    public void setExchangeRate(BigDecimal v)  { this.exchangeRate  = v; }
    public void setNotes(String v)                         { this.notes                  = v; }
    public void setGoldPaymentPurity(String v)             { this.goldPaymentPurity       = v; }
    public void setGoldPaymentWeightGrams(Double v)        { this.goldPaymentWeightGrams  = v; }
    public void setGoldPaymentOzRate(BigDecimal v)         { this.goldPaymentOzRate       = v; }
    public void setGoldPaymentValue(BigDecimal v)          { this.goldPaymentValue        = v; }
    public void setCashAmountPaid(BigDecimal v)            { this.cashAmountPaid          = v; }
    public void setPremiumAmount(BigDecimal v)             { this.premiumAmount           = v != null ? v : BigDecimal.ZERO; }
    public void setDiscountAmount(BigDecimal v)            { this.discountAmount          = v != null ? v : BigDecimal.ZERO; }
    public void setRoundOffAmount(BigDecimal v)            { this.roundOffAmount          = v != null ? v : BigDecimal.ZERO; }
    public void setOriginalPricingMethod(String v)         { this.originalPricingMethod   = v != null ? v : "FIXED_AT_TRADE"; }
    public void setFixingCompletionStatus(String v)        { this.fixingCompletionStatus  = v != null ? v : "NOT_APPLICABLE"; }
    public void setGrossWeightGrams(Double v)              { this.grossWeightGrams        = v; }
    public void setPureWeightGrams(Double v)               { this.pureWeightGrams         = v; }
    public void setAgreedPremiumDiscount(BigDecimal v)     { this.agreedPremiumDiscount   = v; }
    public void setRemainingOpenWeightGrams(Double v)      { this.remainingOpenWeightGrams = v; }
    public void setCreatedBy(String v)                     { this.createdBy               = v; }
}
