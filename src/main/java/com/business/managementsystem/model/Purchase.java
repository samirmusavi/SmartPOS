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
    @Column(nullable = false, length = 20)
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
        DUE
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
    public String           getCreatedBy()              { return createdBy; }
    public LocalDateTime    getCreatedAt()      { return createdAt; }
    public LocalDateTime    getUpdatedAt()      { return updatedAt; }

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
    public void setNotes(String v)                       { this.notes                  = v; }
    public void setGoldPaymentPurity(String v)           { this.goldPaymentPurity       = v; }
    public void setGoldPaymentWeightGrams(Double v)      { this.goldPaymentWeightGrams  = v; }
    public void setGoldPaymentOzRate(BigDecimal v)       { this.goldPaymentOzRate       = v; }
    public void setGoldPaymentValue(BigDecimal v)        { this.goldPaymentValue        = v; }
    public void setCashAmountPaid(BigDecimal v)          { this.cashAmountPaid          = v; }
    public void setCreatedBy(String v)                   { this.createdBy               = v; }
}
