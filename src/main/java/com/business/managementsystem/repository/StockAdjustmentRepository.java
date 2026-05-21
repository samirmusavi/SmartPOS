package com.business.managementsystem.repository;

import com.business.managementsystem.model.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAdjustmentRepository
        extends JpaRepository<StockAdjustment, Long> {

    // All adjustments for a business, newest first (global view)
    @Query("SELECT sa FROM StockAdjustment sa " +
            "JOIN FETCH sa.product " +
            "WHERE sa.businessId = :businessId " +
            "ORDER BY sa.adjustedAt DESC")
    List<StockAdjustment> findByBusinessIdOrderByAdjustedAtDesc(Long businessId);

    // All adjustments for a specific branch, newest first
    @Query("SELECT sa FROM StockAdjustment sa " +
            "JOIN FETCH sa.product " +
            "WHERE sa.businessId = :businessId " +
            "AND sa.branchId = :branchId " +
            "ORDER BY sa.adjustedAt DESC")
    List<StockAdjustment> findByBusinessIdAndBranchIdOrderByAdjustedAtDesc(
            Long businessId, Long branchId);

    // All adjustments for a specific product (global)
    @Query("SELECT sa FROM StockAdjustment sa " +
            "JOIN FETCH sa.product " +
            "WHERE sa.businessId = :businessId " +
            "AND sa.product.id = :productId " +
            "ORDER BY sa.adjustedAt DESC")
    List<StockAdjustment> findByBusinessIdAndProductId(
            Long businessId, Long productId);

    // Count adjustments for a business (global)
    long countByBusinessId(Long businessId);

    // Count adjustments for a specific branch
    long countByBusinessIdAndBranchId(Long businessId, Long branchId);

    // Total stock lost (negative adjustments) — global
    @Query("SELECT COALESCE(SUM(sa.adjustment), 0) FROM StockAdjustment sa " +
            "WHERE sa.businessId = :businessId AND sa.adjustment < 0")
    int getTotalStockLost(Long businessId);

    // Total stock gained (positive adjustments) — global
    @Query("SELECT COALESCE(SUM(sa.adjustment), 0) FROM StockAdjustment sa " +
            "WHERE sa.businessId = :businessId AND sa.adjustment > 0")
    int getTotalStockGained(Long businessId);

    // Total stock lost for a specific branch
    @Query("SELECT COALESCE(SUM(sa.adjustment), 0) FROM StockAdjustment sa " +
            "WHERE sa.businessId = :businessId " +
            "AND sa.branchId = :branchId AND sa.adjustment < 0")
    int getTotalStockLostByBranch(Long businessId, Long branchId);

    // Total stock gained for a specific branch
    @Query("SELECT COALESCE(SUM(sa.adjustment), 0) FROM StockAdjustment sa " +
            "WHERE sa.businessId = :businessId " +
            "AND sa.branchId = :branchId AND sa.adjustment > 0")
    int getTotalStockGainedByBranch(Long businessId, Long branchId);
}