package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A Metal Fix represents an agreement to buy or sell bullion at a price
 * that will be determined later (Hold Price Fix - HPF).
 *
 * Lifecycle:  OPEN → FIXED → SETTLED
 */
@Entity
@Table(name = "metal_fix")
public class MetalFix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "branch_id")
    private Long branchId;

    /** Auto-generated: HPF-{SEQ} */
    @Column(name = "fix_number", nullable = false, unique = true, length = 50)
    private String fixNumber;

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(name = "party_name")
    private String partyName;

    /** SALE_FIX / PURCHASE_FIX */
    @Column(name = "fix_type", nullable = false, length = 20)
    private String fixType;

    /** GOLD / SILVER */
    @Column(name = "metal_type", nullable = false, length = 10)
    private String metalType;

    /** e.g. "995", "999.9", "18k" */
    @Column(name = "purity", length = 20)
    private String purity;

    @Column(name = "weight_grams", nullable = false)
    private Double weightGrams;

    /** USD/troy oz rate at time of fix */
    @Column(name = "transaction_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal transactionRate;

    /** Negative = discount, positive = premium */
    @Column(name = "discount_premium", precision = 10, scale = 2)
    private BigDecimal discountPremium = BigDecimal.ZERO;

    /** PER_OZ (USD per oz) or PERCENTAGE */
    @Column(name = "discount_premium_type", length = 20)
    private String discountPremiumType = "PER_OZ";

    /** transactionRate + discountPremium (when PER_OZ) */
    @Column(name = "effective_rate", precision = 10, scale = 2)
    private BigDecimal effectiveRate;

    /** USD → AED conversion rate */
    @Column(name = "exchange_rate", precision = 10, scale = 4)
    private BigDecimal exchangeRate = new BigDecimal("3.6740");

    /** (weightGrams / 31.1035) × effectiveRate × exchangeRate */
    @Column(name = "total_aed", precision = 15, scale = 2)
    private BigDecimal totalAed;

    @Column(name = "margin_percent", precision = 5, scale = 2)
    private BigDecimal marginPercent;

    @Column(name = "margin_amount", precision = 15, scale = 2)
    private BigDecimal marginAmount;

    /** OPEN / FIXED / SETTLED */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "fixed_rate", precision = 10, scale = 2)
    private BigDecimal fixedRate;

    @Column(name = "fixed_date")
    private LocalDate fixedDate;

    @Column(name = "settlement_amount", precision = 15, scale = 2)
    private BigDecimal settlementAmount;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Column(name = "linked_sale_id")
    private Long linkedSaleId;

    @Column(name = "linked_purchase_id")
    private Long linkedPurchaseId;

    /**
     * NULL              = standalone HPF (existing behaviour, unchanged)
     * UNFIXED_SALE      = this fix prices an open weight from an UNFIXED_AT_TRADE SaleTransaction
     * UNFIXED_PURCHASE  = this fix prices an open weight from an UNFIXED_AT_TRADE Purchase
     *
     * When set, the fix is IMMUTABLE (posted directly as FIXED; no settle lifecycle).
     * The ledger entry posts AED only — metal was already posted at trade time.
     */
    @Column(name = "linked_transaction_type", length = 20)
    private String linkedTransactionType;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    public MetalFix() {}

    // ── Getters ─────────────────────────────────────────────────────
    public Long        getId()                  { return id; }
    public Long        getBusinessId()          { return businessId; }
    public Long        getBranchId()            { return branchId; }
    public String      getFixNumber()           { return fixNumber; }
    public Long        getPartyId()             { return partyId; }
    public String      getPartyName()           { return partyName; }
    public String      getFixType()             { return fixType; }
    public String      getMetalType()           { return metalType; }
    public String      getPurity()              { return purity; }
    public Double      getWeightGrams()         { return weightGrams; }
    public BigDecimal  getTransactionRate()     { return transactionRate; }
    public BigDecimal  getDiscountPremium()     { return discountPremium; }
    public String      getDiscountPremiumType() { return discountPremiumType; }
    public BigDecimal  getEffectiveRate()       { return effectiveRate; }
    public BigDecimal  getExchangeRate()        { return exchangeRate; }
    public BigDecimal  getTotalAed()            { return totalAed; }
    public BigDecimal  getMarginPercent()       { return marginPercent; }
    public BigDecimal  getMarginAmount()        { return marginAmount; }
    public String      getStatus()              { return status; }
    public BigDecimal  getFixedRate()           { return fixedRate; }
    public LocalDate   getFixedDate()           { return fixedDate; }
    public BigDecimal  getSettlementAmount()    { return settlementAmount; }
    public LocalDate   getSettlementDate()      { return settlementDate; }
    public Long        getLinkedSaleId()          { return linkedSaleId; }
    public Long        getLinkedPurchaseId()      { return linkedPurchaseId; }
    public String      getLinkedTransactionType() { return linkedTransactionType; }
    public String      getNotes()                 { return notes; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
    public LocalDateTime getUpdatedAt()         { return updatedAt; }
    public String      getCreatedBy()           { return createdBy; }

    // ── Setters ─────────────────────────────────────────────────────
    public void setBusinessId(Long v)          { this.businessId          = v; }
    public void setBranchId(Long v)            { this.branchId            = v; }
    public void setFixNumber(String v)         { this.fixNumber           = v; }
    public void setPartyId(Long v)             { this.partyId             = v; }
    public void setPartyName(String v)         { this.partyName           = v; }
    public void setFixType(String v)           { this.fixType             = v; }
    public void setMetalType(String v)         { this.metalType           = v; }
    public void setPurity(String v)            { this.purity              = v; }
    public void setWeightGrams(Double v)       { this.weightGrams         = v; }
    public void setTransactionRate(BigDecimal v){ this.transactionRate    = v; }
    public void setDiscountPremium(BigDecimal v){ this.discountPremium    = v; }
    public void setDiscountPremiumType(String v){ this.discountPremiumType = v; }
    public void setEffectiveRate(BigDecimal v) { this.effectiveRate       = v; }
    public void setExchangeRate(BigDecimal v)  { this.exchangeRate        = v; }
    public void setTotalAed(BigDecimal v)      { this.totalAed            = v; }
    public void setMarginPercent(BigDecimal v) { this.marginPercent       = v; }
    public void setMarginAmount(BigDecimal v)  { this.marginAmount        = v; }
    public void setStatus(String v)            { this.status              = v; }
    public void setFixedRate(BigDecimal v)     { this.fixedRate           = v; }
    public void setFixedDate(LocalDate v)      { this.fixedDate           = v; }
    public void setSettlementAmount(BigDecimal v){ this.settlementAmount  = v; }
    public void setSettlementDate(LocalDate v) { this.settlementDate      = v; }
    public void setLinkedSaleId(Long v)             { this.linkedSaleId           = v; }
    public void setLinkedPurchaseId(Long v)         { this.linkedPurchaseId       = v; }
    public void setLinkedTransactionType(String v)  { this.linkedTransactionType  = v; }
    public void setNotes(String v)                  { this.notes                  = v; }
    public void setCreatedBy(String v)         { this.createdBy           = v; }
}
