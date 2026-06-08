package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Records the final settlement of a MetalFix.
 * Created when a FIXED or OPEN metal fix is settled at the confirmed rate.
 */
@Entity
@Table(name = "fix_settlement")
public class FixSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metal_fix_id", nullable = false)
    private Long metalFixId;

    /** Auto-generated: SFX-{SEQ} */
    @Column(name = "settlement_number", nullable = false, unique = true, length = 50)
    private String settlementNumber;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(name = "party_name")
    private String partyName;

    @Column(name = "weight_grams", nullable = false)
    private Double weightGrams;

    @Column(name = "fixed_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal fixedRate;

    @Column(name = "exchange_rate", precision = 10, scale = 4)
    private BigDecimal exchangeRate = new BigDecimal("3.6740");

    /** (weightGrams / 31.1035) × fixedRate × exchangeRate */
    @Column(name = "settlement_aed", nullable = false, precision = 15, scale = 2)
    private BigDecimal settlementAed;

    /** The original totalAed from the MetalFix */
    @Column(name = "original_aed", nullable = false, precision = 15, scale = 2)
    private BigDecimal originalAed;

    /**
     * originalAed − settlementAed.
     * Positive = CR for party (rate went down → they overpaid estimate).
     * Negative = DR for party (rate went up → they underpaid estimate).
     */
    @Column(name = "difference_cr_dr", precision = 15, scale = 2)
    private BigDecimal differenceCrDr;

    @Column(name = "settled_by")
    private String settledBy;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public FixSettlement() {}

    // ── Getters ────────────────────────────────────────────────────
    public Long        getId()               { return id; }
    public Long        getMetalFixId()       { return metalFixId; }
    public String      getSettlementNumber() { return settlementNumber; }
    public Long        getBusinessId()       { return businessId; }
    public Long        getBranchId()         { return branchId; }
    public Long        getPartyId()          { return partyId; }
    public String      getPartyName()        { return partyName; }
    public Double      getWeightGrams()      { return weightGrams; }
    public BigDecimal  getFixedRate()        { return fixedRate; }
    public BigDecimal  getExchangeRate()     { return exchangeRate; }
    public BigDecimal  getSettlementAed()    { return settlementAed; }
    public BigDecimal  getOriginalAed()      { return originalAed; }
    public BigDecimal  getDifferenceCrDr()   { return differenceCrDr; }
    public String      getSettledBy()        { return settledBy; }
    public LocalDate   getSettlementDate()   { return settlementDate; }
    public String      getNotes()            { return notes; }
    public LocalDateTime getCreatedAt()      { return createdAt; }

    // ── Setters ────────────────────────────────────────────────────
    public void setMetalFixId(Long v)        { this.metalFixId        = v; }
    public void setSettlementNumber(String v){ this.settlementNumber   = v; }
    public void setBusinessId(Long v)        { this.businessId        = v; }
    public void setBranchId(Long v)          { this.branchId          = v; }
    public void setPartyId(Long v)           { this.partyId           = v; }
    public void setPartyName(String v)       { this.partyName         = v; }
    public void setWeightGrams(Double v)     { this.weightGrams       = v; }
    public void setFixedRate(BigDecimal v)   { this.fixedRate         = v; }
    public void setExchangeRate(BigDecimal v){ this.exchangeRate       = v; }
    public void setSettlementAed(BigDecimal v){ this.settlementAed    = v; }
    public void setOriginalAed(BigDecimal v) { this.originalAed       = v; }
    public void setDifferenceCrDr(BigDecimal v){ this.differenceCrDr = v; }
    public void setSettledBy(String v)       { this.settledBy         = v; }
    public void setSettlementDate(LocalDate v){ this.settlementDate   = v; }
    public void setNotes(String v)           { this.notes             = v; }
}
