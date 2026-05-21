package com.business.managementsystem.repository;

import com.business.managementsystem.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, Long> {

    // ── Global (owner view — no branch filter) ────────────────
    List<PurchaseOrder> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    Optional<PurchaseOrder> findByIdAndBusinessId(Long id, Long businessId);

    List<PurchaseOrder> findByBusinessIdAndStatus(
            Long businessId, PurchaseOrder.Status status);

    @Query("SELECT COALESCE(SUM(po.quantityOrdered), 0) FROM PurchaseOrder po " +
            "WHERE po.businessId = :businessId " +
            "AND po.productId = :productId " +
            "AND po.status IN ('DRAFT', 'PENDING')")
    int getActiveOrderQuantity(Long businessId, Long productId);

    long countByBusinessId(Long businessId);

    long countByBusinessIdAndStatus(
            Long businessId, PurchaseOrder.Status status);

    @Query("SELECT COALESCE(SUM(po.totalCost), 0) FROM PurchaseOrder po " +
            "WHERE po.businessId = :businessId " +
            "AND po.status = 'RECEIVED'")
    BigDecimal getTotalSpentByBusiness(Long businessId);

    // ── Branch-specific ───────────────────────────────────────
    List<PurchaseOrder> findByBusinessIdAndBranchIdOrderByCreatedAtDesc(
            Long businessId, Long branchId);

    List<PurchaseOrder> findByBusinessIdAndBranchIdAndStatus(
            Long businessId, Long branchId, PurchaseOrder.Status status);

    @Query("SELECT COALESCE(SUM(po.quantityOrdered), 0) FROM PurchaseOrder po " +
            "WHERE po.businessId = :businessId " +
            "AND po.branchId = :branchId " +
            "AND po.productId = :productId " +
            "AND po.status IN ('DRAFT', 'PENDING')")
    int getActiveOrderQuantityByBranch(Long businessId, Long branchId, Long productId);

    long countByBusinessIdAndBranchId(Long businessId, Long branchId);

    long countByBusinessIdAndBranchIdAndStatus(
            Long businessId, Long branchId, PurchaseOrder.Status status);

    @Query("SELECT COALESCE(SUM(po.totalCost), 0) FROM PurchaseOrder po " +
            "WHERE po.businessId = :businessId " +
            "AND po.branchId = :branchId " +
            "AND po.status = 'RECEIVED'")
    BigDecimal getTotalSpentByBusinessAndBranch(Long businessId, Long branchId);
}