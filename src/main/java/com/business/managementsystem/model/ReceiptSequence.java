package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "receipt_sequence",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"branch_id", "year"}))
public class ReceiptSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long branchId;

    @Column(nullable = false)
    private int year;

    // Current sequence number — incremented on every sale
    @Column(nullable = false)
    private long lastSequence = 0;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public ReceiptSequence() {}

    public ReceiptSequence(Long branchId, int year) {
        this.branchId     = branchId;
        this.year         = year;
        this.lastSequence = 0;
    }

    public Long getId()           { return id; }
    public Long getBranchId()     { return branchId; }
    public int getYear()          { return year; }
    public long getLastSequence() { return lastSequence; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setBranchId(Long branchId)       { this.branchId = branchId; }
    public void setYear(int year)                { this.year = year; }
    public void setLastSequence(long seq)        { this.lastSequence = seq; }
}