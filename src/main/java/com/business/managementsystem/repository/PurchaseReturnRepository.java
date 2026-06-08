package com.business.managementsystem.repository;

import com.business.managementsystem.model.PurchaseReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long> {

    // ── Fetch all for a branch / business ─────────────────────
    List<PurchaseReturn> findByBusinessIdAndBranchIdOrderByReturnedAtDesc(
            Long businessId, Long branchId);

    List<PurchaseReturn> findByBusinessIdOrderByReturnedAtDesc(Long businessId);

    List<PurchaseReturn> findByPurchaseId(Long purchaseId);

    // ── Counts ────────────────────────────────────────────────
    long countByBusinessIdAndBranchId(Long businessId, Long branchId);
    long countByBusinessId(Long businessId);

    // ── Total recovered ───────────────────────────────────────
    @Query("SELECT COALESCE(SUM(r.totalReturnAmount), 0) FROM PurchaseReturn r " +
           "WHERE r.businessId = :businessId AND r.branchId = :branchId")
    BigDecimal getTotalRecoveredByBranch(
            @Param("businessId") Long businessId,
            @Param("branchId")   Long branchId);

    @Query("SELECT COALESCE(SUM(r.totalReturnAmount), 0) FROM PurchaseReturn r " +
           "WHERE r.businessId = :businessId")
    BigDecimal getTotalRecoveredByBusiness(@Param("businessId") Long businessId);

    // ── Count for invoice sequence ────────────────────────────
    @Query("SELECT COUNT(r) FROM PurchaseReturn r " +
           "WHERE r.branchId = :branchId " +
           "AND r.returnedAt >= :from AND r.returnedAt < :to")
    long countByBranchAndDateRange(
            @Param("branchId") Long branchId,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to);
}
