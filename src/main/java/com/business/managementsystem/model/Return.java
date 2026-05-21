package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sale_return")
public class Return {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    // Branch where the return was processed
    // Null for legacy returns created before multi-branch
    @Column
    private Long branchId;

    // The original sale this return is against
    @Column(nullable = false)
    private Long saleId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private double quantityReturned;

    // Return invoice number — format: RET-{BRANCH}-{YEAR}-{SEQ}
    @Column(unique = true)
    private String invoiceNumber;

    // Link to the original transaction
    @Column
    private Long transactionId;

    // Refund amount = quantityReturned × priceAtSale
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Reason reason;

    @Column(length = 500)
    private String notes;

    // Who processed the return
    @Column(nullable = false)
    private String processedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime returnedAt;

    public enum Reason {
        DAMAGED,         // Item was damaged / defective
        DEFECTIVE,       // Legacy alias for DAMAGED (kept for DB compatibility)
        WRONG_ITEM,      // Wrong product given
        CUSTOMER_CHANGE, // Customer changed their mind
        EXPIRED,         // Product was expired
        OTHER
    }

    public Return() {}

    public Return(Long businessId, Long branchId, Long saleId,
                  Long productId, String productName,
                  double quantityReturned, BigDecimal refundAmount,
                  Reason reason, String notes, String processedBy) {
        this.businessId       = businessId;
        this.branchId         = branchId;
        this.saleId           = saleId;
        this.productId        = productId;
        this.productName      = productName;
        this.quantityReturned = quantityReturned;
        this.refundAmount     = refundAmount;
        this.reason           = reason;
        this.notes            = notes;
        this.processedBy      = processedBy;
    }

    public Long getId()               { return id; }
    public Long getBusinessId()       { return businessId; }
    public Long getBranchId()         { return branchId; }
    public Long getSaleId()           { return saleId; }
    public Long getProductId()        { return productId; }
    public String getProductName()    { return productName; }
    public double getQuantityReturned()  { return quantityReturned; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public Reason getReason()         { return reason; }
    public String getNotes()          { return notes; }
    public String getProcessedBy()    { return processedBy; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public String getInvoiceNumber()  { return invoiceNumber; }
    public Long getTransactionId()    { return transactionId; }

    public void setBranchId(Long branchId)        { this.branchId = branchId; }
    public void setInvoiceNumber(String v)        { this.invoiceNumber = v; }
    public void setTransactionId(Long v)          { this.transactionId = v; }
}