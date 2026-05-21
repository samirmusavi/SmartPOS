package com.business.managementsystem.repository;

import com.business.managementsystem.model.SaleTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface SaleTransactionRepository
        extends JpaRepository<SaleTransaction, Long> {

    // Lookup by receipt number — used by receipt detail endpoint
    Optional<SaleTransaction> findByReceiptNumber(String receiptNumber);

    // All transactions for a business — most recent first
    List<SaleTransaction> findByBusinessIdOrderByCreatedAtDesc(
            Long businessId);

    // All transactions for a specific branch
    List<SaleTransaction> findByBusinessIdAndBranchIdOrderByCreatedAtDesc(
            Long businessId, Long branchId);

    // All transactions for a specific customer
    List<SaleTransaction> findByBusinessIdAndCustomerIdOrderByCreatedAtDesc(
            Long businessId, Long customerId);

    // Recent 10 for dashboard widget
    List<SaleTransaction> findTop10ByBusinessIdOrderByCreatedAtDesc(
            Long businessId);

    List<SaleTransaction> findTop10ByBusinessIdAndBranchIdOrderByCreatedAtDesc(
            Long businessId, Long branchId);

    // Search by receipt number (partial match)
    @Query("SELECT t FROM SaleTransaction t " +
            "WHERE t.businessId = :businessId " +
            "AND LOWER(t.receiptNumber) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY t.createdAt DESC")
    List<SaleTransaction> searchByReceiptNumber(
            Long businessId, String query);

    @Query("SELECT t FROM SaleTransaction t " +
            "WHERE t.businessId = :businessId " +
            "AND t.branchId = :branchId " +
            "AND LOWER(t.receiptNumber) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY t.createdAt DESC")
    List<SaleTransaction> searchByReceiptNumberAndBranch(
            Long businessId, Long branchId, String query);

    // Count for stats
    long countByBusinessId(Long businessId);
    long countByBusinessIdAndBranchId(Long businessId, Long branchId);

    // ── Paginated search: all branches ───────────────────────────────────────
    @Query("SELECT t FROM SaleTransaction t " +
            "WHERE t.businessId = :businessId " +
            "AND (:q IS NULL OR (" +
            "  LOWER(t.receiptNumber) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "  OR LOWER(t.customerName) LIKE LOWER(CONCAT('%', :q, '%'))" +
            ")) " +
            "AND (:from IS NULL OR t.createdAt >= :from) " +
            "AND (:to IS NULL OR t.createdAt < :to) " +
            "ORDER BY t.createdAt DESC")
    Page<SaleTransaction> searchTransactions(
            @Param("businessId") Long businessId,
            @Param("q")          String q,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to,
            Pageable pageable);

    // ── Paginated search: specific branch ─────────────────────────────────
    @Query("SELECT t FROM SaleTransaction t " +
            "WHERE t.businessId = :businessId " +
            "AND t.branchId = :branchId " +
            "AND (:q IS NULL OR (" +
            "  LOWER(t.receiptNumber) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "  OR LOWER(t.customerName) LIKE LOWER(CONCAT('%', :q, '%'))" +
            ")) " +
            "AND (:from IS NULL OR t.createdAt >= :from) " +
            "AND (:to IS NULL OR t.createdAt < :to) " +
            "ORDER BY t.createdAt DESC")
    Page<SaleTransaction> searchTransactionsByBranch(
            @Param("businessId") Long businessId,
            @Param("branchId")   Long branchId,
            @Param("q")          String q,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to,
            Pageable pageable);

    // ── Admin Dashboard: Last Active per Business ─────────────
    // Returns [businessId, MAX(createdAt)] for all businesses.
    // Used to show "Last Active" column in admin businesses table.
    @Query("SELECT t.businessId, MAX(t.createdAt) " +
            "FROM SaleTransaction t GROUP BY t.businessId")
    List<Object[]> findLatestActivityPerBusiness();

    // ── Admin Dashboard: Active Business IDs Since ────────────
    // Returns distinct businessIds that had at least one transaction
    // after the given date. Used for Health Score calculation.
    @Query("SELECT DISTINCT t.businessId " +
            "FROM SaleTransaction t WHERE t.createdAt >= :since")
    Set<Long> findActiveBusinessIdsSince(
            @Param("since") LocalDateTime since);
}