package com.business.managementsystem.repository;

import com.business.managementsystem.model.Return;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReturnRepository extends JpaRepository<Return, Long> {

    // ── Global (owner view — no branch filter) ────────────────
    List<Return> findByBusinessIdOrderByReturnedAtDesc(Long businessId);

    List<Return> findBySaleId(Long saleId);

    boolean existsBySaleId(Long saleId);

    long countByBusinessId(Long businessId);

    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM Return r " +
            "WHERE r.businessId = :businessId")
    BigDecimal getTotalRefundsByBusiness(Long businessId);

    @Query("SELECT COALESCE(SUM(r.quantityReturned), 0) FROM Return r " +
            "WHERE r.businessId = :businessId")
    Double getTotalUnitsReturnedByBusiness(Long businessId);

    @Query("SELECT COALESCE(SUM(r.quantityReturned), 0) FROM Return r " +
            "WHERE r.saleId = :saleId")
    double getTotalReturnedQtyBySaleId(Long saleId);

    // ── Branch-specific ───────────────────────────────────────
    List<Return> findByBusinessIdAndBranchIdOrderByReturnedAtDesc(
            Long businessId, Long branchId);

    long countByBusinessIdAndBranchId(Long businessId, Long branchId);

    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM Return r " +
            "WHERE r.businessId = :businessId AND r.branchId = :branchId")
    BigDecimal getTotalRefundsByBusinessAndBranch(Long businessId, Long branchId);

    @Query("SELECT COALESCE(SUM(r.quantityReturned), 0) FROM Return r " +
            "WHERE r.businessId = :businessId AND r.branchId = :branchId")
    Double getTotalUnitsReturnedByBusinessAndBranch(Long businessId, Long branchId);

    // ── Paginated search — all branches ──────────────────────
    @Query("SELECT r FROM Return r " +
            "WHERE r.businessId = :businessId " +
            "AND (:q IS NULL OR (" +
            "  LOWER(r.productName) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "  OR LOWER(r.processedBy) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "  OR LOWER(r.invoiceNumber) LIKE LOWER(CONCAT('%', :q, '%'))" +
            ")) " +
            "AND (:from IS NULL OR r.returnedAt >= :from) " +
            "AND (:to IS NULL OR r.returnedAt < :to) " +
            "ORDER BY r.returnedAt DESC")
    Page<Return> searchReturns(
            @Param("businessId") Long businessId,
            @Param("q")          String q,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to,
            Pageable pageable);

    // ── Paginated search — specific branch ────────────────────
    @Query("SELECT r FROM Return r " +
            "WHERE r.businessId = :businessId " +
            "AND r.branchId = :branchId " +
            "AND (:q IS NULL OR (" +
            "  LOWER(r.productName) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "  OR LOWER(r.processedBy) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "  OR LOWER(r.invoiceNumber) LIKE LOWER(CONCAT('%', :q, '%'))" +
            ")) " +
            "AND (:from IS NULL OR r.returnedAt >= :from) " +
            "AND (:to IS NULL OR r.returnedAt < :to) " +
            "ORDER BY r.returnedAt DESC")
    Page<Return> searchReturnsByBranch(
            @Param("businessId") Long businessId,
            @Param("branchId")   Long branchId,
            @Param("q")          String q,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to,
            Pageable pageable);
}