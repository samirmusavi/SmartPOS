package com.business.managementsystem.repository;

import com.business.managementsystem.model.UserBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserBranchRepository
        extends JpaRepository<UserBranch, Long> {

    // All branch assignments for a user
    List<UserBranch> findByUserId(Long userId);

    // All user assignments for a branch
    List<UserBranch> findByBranchId(Long branchId);

    // Check if a user is assigned to a specific branch
    boolean existsByUserIdAndBranchId(Long userId, Long branchId);

    // Remove a specific assignment
    @Modifying
    @Transactional
    @Query("DELETE FROM UserBranch ub " +
            "WHERE ub.userId = :userId AND ub.branchId = :branchId")
    void deleteByUserIdAndBranchId(Long userId, Long branchId);

    // Remove all branch assignments for a user (used when deleting staff)
    @Modifying
    @Transactional
    @Query("DELETE FROM UserBranch ub WHERE ub.userId = :userId")
    void deleteByUserId(Long userId);
}