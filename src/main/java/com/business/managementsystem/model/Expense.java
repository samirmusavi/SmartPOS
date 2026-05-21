package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expense")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    // Which branch this expense belongs to.
    // Null for legacy expenses created before multi-branch, or owner-global expenses.
    @Column
    private Long branchId;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String category;

    // The date this expense started / occurred
    @Column(nullable = false)
    private LocalDate expenseDate;

    // Is this a recurring expense?
    @Column(nullable = false)
    private boolean recurring = false;

    // MONTHLY, WEEKLY, YEARLY — only relevant if recurring = true
    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    // If recurring, when does it stop? null = forever
    private LocalDate recurringEndDate;

    public enum Frequency { MONTHLY, WEEKLY, YEARLY }

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Expense() {}

    public Expense(Long businessId, String title, String description,
                   BigDecimal amount, String category, LocalDate expenseDate,
                   boolean recurring, Frequency frequency,
                   LocalDate recurringEndDate) {
        this.businessId       = businessId;
        this.title            = title;
        this.description      = description;
        this.amount           = amount;
        this.category         = category;
        this.expenseDate      = expenseDate;
        this.recurring        = recurring;
        this.frequency        = frequency;
        this.recurringEndDate = recurringEndDate;
    }

    public Long getId()             { return id; }
    public void setId(Long id)      { this.id = id; }

    public Long getBusinessId()                      { return businessId; }
    public void setBusinessId(Long businessId)       { this.businessId = businessId; }

    public Long getBranchId()                        { return branchId; }
    public void setBranchId(Long branchId)           { this.branchId = branchId; }

    public String getTitle()                         { return title; }
    public void setTitle(String title)               { this.title = title; }

    public String getDescription()                   { return description; }
    public void setDescription(String description)   { this.description = description; }

    public BigDecimal getAmount()                    { return amount; }
    public void setAmount(BigDecimal amount)         { this.amount = amount; }

    public String getCategory()                      { return category; }
    public void setCategory(String category)         { this.category = category; }

    public LocalDate getExpenseDate()                { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate){ this.expenseDate = expenseDate; }

    public boolean isRecurring()                     { return recurring; }
    public void setRecurring(boolean recurring)      { this.recurring = recurring; }

    public Frequency getFrequency()                  { return frequency; }
    public void setFrequency(Frequency frequency)    { this.frequency = frequency; }

    public LocalDate getRecurringEndDate()           { return recurringEndDate; }
    public void setRecurringEndDate(LocalDate recurringEndDate) {
        this.recurringEndDate = recurringEndDate;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}