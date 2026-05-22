package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_entry")
public class CashEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @Column
    private Long branchId;

    @Column(nullable = false)
    private LocalDate sheetDate;

    @Column(length = 255)
    private String partyName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountIn = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountOut = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EntryType entryType;

    // Links to the originating record (sale_transaction.id, expense.id, sale_return.id)
    @Column
    private Long referenceId;

    @Column(nullable = false)
    private int sortOrder = 0;

    @Column(length = 255)
    private String createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum EntryType {
        SALE,
        EXPENSE,
        RETURN,
        MANUAL_IN,
        MANUAL_OUT,
        HAND_LOAN_IN,
        HAND_LOAN_OUT,
        OPENING
    }

    public CashEntry() {}

    public CashEntry(Long businessId, Long branchId, LocalDate sheetDate,
                     String partyName, String description,
                     BigDecimal amountIn, BigDecimal amountOut,
                     EntryType entryType, Long referenceId,
                     int sortOrder, String createdBy) {
        this.businessId  = businessId;
        this.branchId    = branchId;
        this.sheetDate   = sheetDate;
        this.partyName   = partyName;
        this.description = description;
        this.amountIn    = amountIn  != null ? amountIn  : BigDecimal.ZERO;
        this.amountOut   = amountOut != null ? amountOut : BigDecimal.ZERO;
        this.entryType   = entryType;
        this.referenceId = referenceId;
        this.sortOrder   = sortOrder;
        this.createdBy   = createdBy;
    }

    // ── Getters ───────────────────────────────────────────────────
    public Long        getId()          { return id; }
    public Long        getBusinessId()  { return businessId; }
    public Long        getBranchId()    { return branchId; }
    public LocalDate   getSheetDate()   { return sheetDate; }
    public String      getPartyName()   { return partyName; }
    public String      getDescription() { return description; }
    public BigDecimal  getAmountIn()    { return amountIn; }
    public BigDecimal  getAmountOut()   { return amountOut; }
    public EntryType   getEntryType()   { return entryType; }
    public Long        getReferenceId() { return referenceId; }
    public int         getSortOrder()   { return sortOrder; }
    public String      getCreatedBy()   { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ── Setters (only mutable fields) ────────────────────────────
    public void setPartyName(String v)    { this.partyName   = v; }
    public void setDescription(String v)  { this.description = v; }
    public void setAmountIn(BigDecimal v)  { this.amountIn  = v != null ? v : BigDecimal.ZERO; }
    public void setAmountOut(BigDecimal v) { this.amountOut = v != null ? v : BigDecimal.ZERO; }
    public void setSortOrder(int v)        { this.sortOrder  = v; }
}
