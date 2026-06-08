package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Unified party ledger entry.
 * Every financial transaction (sale, purchase, receipt, payment, fix, settlement)
 * posts one or more entries here.
 *
 * Running balance convention (Σ credit − Σ debit):
 *   positive → CR  (party owes you, or has overpaid)
 *   negative → DR  (you owe the party)
 *
 * Entry directions:
 *   SAL / HPF  → aedCredit (party owes you AED), metalDebit (you gave metal)
 *   PUR        → aedDebit  (you owe party AED),  metalCredit (you received metal)
 *   REC        → aedDebit  (party paid you → reduces their outstanding)
 *   PAY        → aedCredit (you paid party → reduces your outstanding)
 *   SFX        → aedDebit  + metalCredit (closes the fix position)
 */
@Entity
@Table(name = "party_ledger_entry",
       indexes = {
           @Index(name = "idx_ple_party",  columnList = "business_id, party_id"),
           @Index(name = "idx_ple_date",   columnList = "business_id, party_id, voucher_date")
       })
public class PartyLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(name = "party_name")
    private String partyName;

    /** HPF, SAL, SFX, REC, PAY, PUR, ADJ */
    @Column(name = "voucher_type", nullable = false, length = 10)
    private String voucherType;

    /** e.g. SAL-65, HPF-40, REC-86 */
    @Column(name = "voucher_number", length = 100)
    private String voucherNumber;

    @Column(name = "voucher_date", nullable = false)
    private LocalDate voucherDate;

    @Column(name = "narration", length = 500)
    private String narration;

    @Column(name = "aed_debit",  nullable = false, precision = 15, scale = 2)
    private BigDecimal aedDebit  = BigDecimal.ZERO;

    @Column(name = "aed_credit", nullable = false, precision = 15, scale = 2)
    private BigDecimal aedCredit = BigDecimal.ZERO;

    /** Grams of metal debited (given out) */
    @Column(name = "metal_debit",  nullable = false)
    private Double metalDebit  = 0.0;

    /** Grams of metal credited (received in) */
    @Column(name = "metal_credit", nullable = false)
    private Double metalCredit = 0.0;

    /** GOLD / SILVER */
    @Column(name = "metal_type", length = 10)
    private String metalType;

    /** e.g. "995", "999.9", "18k" */
    @Column(name = "purity", length = 20)
    private String purity;

    /** FK to sale_transaction.id, purchase.id, metal_fix.id, etc. */
    @Column(name = "reference_id")
    private Long referenceId;

    /** SALE, PURCHASE, RECEIPT, PAYMENT, FIX, SETTLEMENT */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    public PartyLedgerEntry() {}

    // ── Getters ────────────────────────────────────────────────────
    public Long        getId()            { return id; }
    public Long        getBusinessId()    { return businessId; }
    public Long        getBranchId()      { return branchId; }
    public Long        getPartyId()       { return partyId; }
    public String      getPartyName()     { return partyName; }
    public String      getVoucherType()   { return voucherType; }
    public String      getVoucherNumber() { return voucherNumber; }
    public LocalDate   getVoucherDate()   { return voucherDate; }
    public String      getNarration()     { return narration; }
    public BigDecimal  getAedDebit()      { return aedDebit; }
    public BigDecimal  getAedCredit()     { return aedCredit; }
    public Double      getMetalDebit()    { return metalDebit; }
    public Double      getMetalCredit()   { return metalCredit; }
    public String      getMetalType()     { return metalType; }
    public String      getPurity()        { return purity; }
    public Long        getReferenceId()   { return referenceId; }
    public String      getReferenceType() { return referenceType; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public String      getCreatedBy()     { return createdBy; }

    // ── Setters ────────────────────────────────────────────────────
    public void setBusinessId(Long v)    { this.businessId    = v; }
    public void setBranchId(Long v)      { this.branchId      = v; }
    public void setPartyId(Long v)       { this.partyId       = v; }
    public void setPartyName(String v)   { this.partyName     = v; }
    public void setVoucherType(String v) { this.voucherType   = v; }
    public void setVoucherNumber(String v){ this.voucherNumber = v; }
    public void setVoucherDate(LocalDate v){ this.voucherDate  = v; }
    public void setNarration(String v)   { this.narration     = v; }
    public void setAedDebit(BigDecimal v) { this.aedDebit     = v != null ? v : BigDecimal.ZERO; }
    public void setAedCredit(BigDecimal v){ this.aedCredit    = v != null ? v : BigDecimal.ZERO; }
    public void setMetalDebit(Double v)  { this.metalDebit    = v != null ? v : 0.0; }
    public void setMetalCredit(Double v) { this.metalCredit   = v != null ? v : 0.0; }
    public void setMetalType(String v)   { this.metalType     = v; }
    public void setPurity(String v)      { this.purity        = v; }
    public void setReferenceId(Long v)   { this.referenceId   = v; }
    public void setReferenceType(String v){ this.referenceType = v; }
    public void setCreatedBy(String v)   { this.createdBy     = v; }
}
