package com.business.managementsystem.repository;

import com.business.managementsystem.model.BranchPriceOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchPriceOverrideRepository
        extends JpaRepository<BranchPriceOverride, Long> {

    Optional<BranchPriceOverride> findByBranchIdAndProductId(
            Long branchId, Long productId);

    List<BranchPriceOverride> findByBranchId(Long branchId);

    @Modifying
    @Transactional
    @Query("DELETE FROM BranchPriceOverride b " +
            "WHERE b.branchId = :branchId AND b.productId = :productId")
    void deleteByBranchIdAndProductId(Long branchId, Long productId);
}