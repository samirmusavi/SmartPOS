package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_day_sheet")
public class CashDaySheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @Column
    private Long branchId;

    @Column(nullable = false)
    private LocalDate sheetDate;

    // Opening cash balance for this day — auto-filled from yesterday's closing
    // unless openingOverridden = true, in which case the manually set value wins
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean openingOverridden = false;

    // ── Denomination bundles ──────────────────────────────────────
    @Column(nullable = false) private int bundles1000 = 0;
    @Column(nullable = false) private int bundles500  = 0;
    @Column(nullable = false) private int bundles200  = 0;
    @Column(nullable = false) private int bundles100  = 0;
    @Column(nullable = false) private int bundles50   = 0;
    @Column(nullable = false) private int bundles10   = 0;
    @Column(nullable = false) private int bundles5    = 0;

    // Loose / mixed notes not sorted into bundles
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal mixNotes = BigDecimal.ZERO;

    // Coins total
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal coins = BigDecimal.ZERO;

    // Hand loans given out today (affects excess/short calculation)
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal handLoanTotal = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public CashDaySheet() {}

    public CashDaySheet(Long businessId, Long branchId,
                        LocalDate sheetDate, BigDecimal openingBalance) {
        this.businessId     = businessId;
        this.branchId       = branchId;
        this.sheetDate      = sheetDate;
        this.openingBalance = openingBalance != null ? openingBalance : BigDecimal.ZERO;
    }

    // ── Getters ───────────────────────────────────────────────────
    public Long       getId()                  { return id; }
    public Long       getBusinessId()          { return businessId; }
    public Long       getBranchId()            { return branchId; }
    public LocalDate  getSheetDate()           { return sheetDate; }
    public BigDecimal getOpeningBalance()      { return openingBalance; }
    public boolean    isOpeningOverridden()    { return openingOverridden; }
    public int        getBundles1000()         { return bundles1000; }
    public int        getBundles500()          { return bundles500; }
    public int        getBundles200()          { return bundles200; }
    public int        getBundles100()          { return bundles100; }
    public int        getBundles50()           { return bundles50; }
    public int        getBundles10()           { return bundles10; }
    public int        getBundles5()            { return bundles5; }
    public BigDecimal getMixNotes()            { return mixNotes; }
    public BigDecimal getCoins()               { return coins; }
    public BigDecimal getHandLoanTotal()       { return handLoanTotal; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
    public LocalDateTime getUpdatedAt()        { return updatedAt; }

    // ── Setters ───────────────────────────────────────────────────
    public void setOpeningBalance(BigDecimal v)  { this.openingBalance   = v != null ? v : BigDecimal.ZERO; }
    public void setOpeningOverridden(boolean v)  { this.openingOverridden = v; }
    public void setBundles1000(int v)            { this.bundles1000 = v; }
    public void setBundles500(int v)             { this.bundles500  = v; }
    public void setBundles200(int v)             { this.bundles200  = v; }
    public void setBundles100(int v)             { this.bundles100  = v; }
    public void setBundles50(int v)              { this.bundles50   = v; }
    public void setBundles10(int v)              { this.bundles10   = v; }
    public void setBundles5(int v)               { this.bundles5    = v; }
    public void setMixNotes(BigDecimal v)        { this.mixNotes    = v != null ? v : BigDecimal.ZERO; }
    public void setCoins(BigDecimal v)           { this.coins       = v != null ? v : BigDecimal.ZERO; }
    public void setHandLoanTotal(BigDecimal v)   { this.handLoanTotal = v != null ? v : BigDecimal.ZERO; }
}
