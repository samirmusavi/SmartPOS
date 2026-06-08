package com.business.managementsystem.model;

import jakarta.persistence.*;

@Entity
@Table(name = "voucher_sequence",
       uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "voucher_type"}))
public class VoucherSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "voucher_type", nullable = false, length = 10)
    private String voucherType;

    @Column(name = "last_number", nullable = false)
    private int lastNumber = 0;

    public VoucherSequence() {}

    public VoucherSequence(Long businessId, String voucherType) {
        this.businessId  = businessId;
        this.voucherType = voucherType;
        this.lastNumber  = 0;
    }

    public Long   getId()          { return id; }
    public Long   getBusinessId()  { return businessId; }
    public String getVoucherType() { return voucherType; }
    public int    getLastNumber()  { return lastNumber; }

    public void setBusinessId(Long v)  { this.businessId  = v; }
    public void setVoucherType(String v){ this.voucherType = v; }
    public void setLastNumber(int v)   { this.lastNumber  = v; }
}
