package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_return")
public class PurchaseReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private Long branchId;

    @Column(nullable = false)
    private Long purchaseId;

    @Column(length = 255)
    private String supplierName;

    /** Format: PRET-{BRANCHCODE}-{YYYYMMDD}-{SEQ:03d} */
    @Column(nullable = false, unique = true, length = 100)
    private String returnInvoiceNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalReturnAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Reason reason = Reason.OTHER;

    @Column(length = 1000)
    private String notes;

    @Column(length = 255)
    private String createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime returnedAt;

    public enum Reason {
        WRONG_ITEM,
        DAMAGED,
        SUPPLIER_ERROR,
        QUALITY_ISSUE,
        OTHER
    }

    public PurchaseReturn() {}

    // ── Getters ───────────────────────────────────────────────
    public Long           getId()                  { return id; }
    public Long           getBusinessId()          { return businessId; }
    public Long           getBranchId()            { return branchId; }
    public Long           getPurchaseId()          { return purchaseId; }
    public String         getSupplierName()        { return supplierName; }
    public String         getReturnInvoiceNumber() { return returnInvoiceNumber; }
    public BigDecimal     getTotalReturnAmount()   { return totalReturnAmount; }
    public Reason         getReason()              { return reason; }
    public String         getNotes()               { return notes; }
    public String         getCreatedBy()           { return createdBy; }
    public LocalDateTime  getReturnedAt()          { return returnedAt; }

    // ── Setters ───────────────────────────────────────────────
    public void setBusinessId(Long v)          { this.businessId          = v; }
    public void setBranchId(Long v)            { this.branchId            = v; }
    public void setPurchaseId(Long v)          { this.purchaseId          = v; }
    public void setSupplierName(String v)      { this.supplierName        = v; }
    public void setReturnInvoiceNumber(String v){ this.returnInvoiceNumber = v; }
    public void setTotalReturnAmount(BigDecimal v){ this.totalReturnAmount = v != null ? v : BigDecimal.ZERO; }
    public void setReason(Reason v)            { this.reason              = v != null ? v : Reason.OTHER; }
    public void setNotes(String v)             { this.notes               = v; }
    public void setCreatedBy(String v)         { this.createdBy           = v; }
}
