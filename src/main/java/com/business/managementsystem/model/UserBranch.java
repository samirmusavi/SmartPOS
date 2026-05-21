package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_branch",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "branch_id"}))
public class UserBranch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long branchId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime assignedAt;

    public UserBranch() {}

    public UserBranch(Long userId, Long branchId) {
        this.userId   = userId;
        this.branchId = branchId;
    }

    public Long getId()          { return id; }
    public Long getUserId()      { return userId; }
    public Long getBranchId()    { return branchId; }
    public LocalDateTime getAssignedAt() { return assignedAt; }

    public void setUserId(Long userId)    { this.userId = userId; }
    public void setBranchId(Long branchId){ this.branchId = branchId; }
}