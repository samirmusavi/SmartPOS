package com.business.managementsystem.repository;

import com.business.managementsystem.model.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    /** Lookup by invoice number + business for purchase-return flow */
    Optional<Purchase> findByInvoiceNumberAndBusinessId(String invoiceNumber, Long businessId);

    // ── Paginated search — all branches ──────────────────────
    @Query("SELECT p FROM Purchase p " +
           "WHERE p.businessId = :businessId " +
           "AND (:q IS NULL OR (" +
           "  LOWER(p.invoiceNumber) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "  OR LOWER(p.supplierName) LIKE LOWER(CONCAT('%', :q, '%'))" +
           ")) " +
           "AND (:from IS NULL OR p.purchaseDate >= :from) " +
           "AND (:to   IS NULL OR p.purchaseDate <  :to) " +
           "ORDER BY p.purchaseDate DESC")
    Page<Purchase> searchPurchases(
            @Param("businessId") Long businessId,
            @Param("q")          String q,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to,
            Pageable pageable);

    // ── Paginated search — specific branch ───────────────────
    @Query("SELECT p FROM Purchase p " +
           "WHERE p.businessId = :businessId " +
           "AND p.branchId = :branchId " +
           "AND (:q IS NULL OR (" +
           "  LOWER(p.invoiceNumber) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "  OR LOWER(p.supplierName) LIKE LOWER(CONCAT('%', :q, '%'))" +
           ")) " +
           "AND (:from IS NULL OR p.purchaseDate >= :from) " +
           "AND (:to   IS NULL OR p.purchaseDate <  :to) " +
           "ORDER BY p.purchaseDate DESC")
    Page<Purchase> searchPurchasesByBranch(
            @Param("businessId") Long businessId,
            @Param("branchId")   Long branchId,
            @Param("q")          String q,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to,
            Pageable pageable);

    // ── Sum total for date range ──────────────────────────────
    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM Purchase p " +
           "WHERE p.businessId = :businessId " +
           "AND p.branchId = :branchId " +
           "AND p.purchaseDate >= :from AND p.purchaseDate < :to")
    BigDecimal sumTotalByBranchAndDateRange(
            @Param("businessId") Long businessId,
            @Param("branchId")   Long branchId,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to);

    // ── Count per branch+date for invoice sequence ────────────
    @Query("SELECT COUNT(p) FROM Purchase p " +
           "WHERE p.branchId = :branchId " +
           "AND p.purchaseDate >= :from AND p.purchaseDate < :to")
    long countByBranchIdAndDateRange(
            @Param("branchId") Long branchId,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to);

    // ── Open UNFIXED_AT_TRADE positions (remainingOpenWeightGrams > 0) ───────────
    // Used by the Fixing UI to present a list of positions available to price.

    @Query("SELECT p FROM Purchase p " +
           "WHERE p.businessId = :businessId " +
           "AND p.originalPricingMethod = 'UNFIXED_AT_TRADE' " +
           "AND p.fixingCompletionStatus = 'OPEN' " +
           "ORDER BY p.purchaseDate DESC")
    List<Purchase> findOpenUnfixedPurchases(
            @Param("businessId") Long businessId);

    @Query("SELECT p FROM Purchase p " +
           "WHERE p.businessId = :businessId " +
           "AND p.branchId = :branchId " +
           "AND p.originalPricingMethod = 'UNFIXED_AT_TRADE' " +
           "AND p.fixingCompletionStatus = 'OPEN' " +
           "ORDER BY p.purchaseDate DESC")
    List<Purchase> findOpenUnfixedPurchasesByBranch(
            @Param("businessId") Long businessId,
            @Param("branchId")   Long branchId);

    // ── Cash purchases for cash-sheet sync ───────────────────
    @Query("SELECT p FROM Purchase p " +
           "WHERE p.businessId = :businessId " +
           "AND p.branchId = :branchId " +
           "AND p.paymentMethod = 'CASH' " +
           "AND p.purchaseDate >= :from AND p.purchaseDate < :to " +
           "ORDER BY p.purchaseDate ASC")
    List<Purchase> findCashPurchasesByBranchAndDateRange(
            @Param("businessId") Long businessId,
            @Param("branchId")   Long branchId,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to);

    // ── By-payment-method breakdown for summary ───────────────
    @Query("SELECT p.paymentMethod, COUNT(p), COALESCE(SUM(p.totalAmount), 0) " +
           "FROM Purchase p " +
           "WHERE p.businessId = :businessId " +
           "AND p.branchId = :branchId " +
           "AND p.purchaseDate >= :from AND p.purchaseDate < :to " +
           "GROUP BY p.paymentMethod")
    List<Object[]> groupByPaymentMethod(
            @Param("businessId") Long businessId,
            @Param("branchId")   Long branchId,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to);
}
