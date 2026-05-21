package com.business.managementsystem.repository;

import com.business.managementsystem.model.Sale;
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
public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findByProductId(Long productId);

    // Receipt number lookup — used by receipt detail endpoint
    Optional<Sale> findByReceiptNumber(String receiptNumber);

    // All sale items belonging to one transaction
    List<Sale> findByTransactionId(Long transactionId);

    List<Sale> findByBusinessIdOrderBySaleDateDesc(Long businessId);

    List<Sale> findByBusinessIdAndBranchIdOrderBySaleDateDesc(
            Long businessId, Long branchId);

    List<Sale> findTop10ByBusinessIdOrderBySaleDateDesc(Long businessId);

    List<Sale> findTop10ByBusinessIdAndBranchIdOrderBySaleDateDesc(
            Long businessId, Long branchId);

    // ── Business-level aggregates (all branches) ──────────────
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.businessId = :businessId")
    BigDecimal getTotalRevenueByBusiness(Long businessId);

    @Query("SELECT COALESCE(SUM(s.quantitySold * s.costAtSale), 0) FROM Sale s WHERE s.businessId = :businessId")
    Double getTotalCOGSByBusiness(Long businessId);

    @Query("SELECT COALESCE(SUM(s.quantitySold), 0) FROM Sale s WHERE s.businessId = :businessId")
    Double getTotalUnitsSoldByBusiness(Long businessId);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.businessId = :businessId")
    long getTotalSalesCountByBusiness(Long businessId);

    @Query("SELECT s.product.name, SUM(s.quantitySold), SUM(s.totalAmount) " +
            "FROM Sale s WHERE s.businessId = :businessId " +
            "GROUP BY s.product.id, s.product.name " +
            "ORDER BY SUM(s.quantitySold) DESC LIMIT 5")
    List<Object[]> getTopSellingProductsByBusiness(Long businessId);

    @Query("SELECT s.product.category, " +
            "SUM(s.quantitySold), " +
            "SUM(s.totalAmount), " +
            "SUM(s.quantitySold * s.costAtSale) " +
            "FROM Sale s WHERE s.businessId = :businessId " +
            "GROUP BY s.product.category " +
            "ORDER BY SUM(s.totalAmount) DESC")
    List<Object[]> getProfitByCategoryByBusiness(Long businessId);

    @Query("SELECT s.product.category, SUM(s.quantitySold), SUM(s.totalAmount) " +
            "FROM Sale s WHERE s.businessId = :businessId " +
            "GROUP BY s.product.category " +
            "ORDER BY SUM(s.totalAmount) DESC")
    List<Object[]> getSalesByCategoryByBusiness(Long businessId);

    // ── Branch-level aggregates ───────────────────────────────
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s " +
            "WHERE s.businessId = :businessId AND s.branchId = :branchId")
    BigDecimal getTotalRevenueByBranch(Long businessId, Long branchId);

    @Query("SELECT COALESCE(SUM(s.quantitySold * s.costAtSale), 0) FROM Sale s " +
            "WHERE s.businessId = :businessId AND s.branchId = :branchId")
    Double getTotalCOGSByBranch(Long businessId, Long branchId);

    @Query("SELECT COUNT(s) FROM Sale s " +
            "WHERE s.businessId = :businessId AND s.branchId = :branchId")
    long getTotalSalesCountByBranch(Long businessId, Long branchId);

    @Query("SELECT COALESCE(SUM(s.quantitySold), 0) FROM Sale s " +
            "WHERE s.businessId = :businessId AND s.branchId = :branchId")
    Double getTotalUnitsSoldByBranch(Long businessId, Long branchId);

    @Query("SELECT s.product.name, SUM(s.quantitySold), SUM(s.totalAmount) " +
            "FROM Sale s WHERE s.businessId = :businessId AND s.branchId = :branchId " +
            "GROUP BY s.product.id, s.product.name " +
            "ORDER BY SUM(s.quantitySold) DESC LIMIT 5")
    List<Object[]> getTopSellingProductsByBranch(Long businessId, Long branchId);

    @Query("SELECT s.product.category, " +
            "SUM(s.quantitySold), " +
            "SUM(s.totalAmount), " +
            "SUM(s.quantitySold * s.costAtSale) " +
            "FROM Sale s WHERE s.businessId = :businessId AND s.branchId = :branchId " +
            "GROUP BY s.product.category " +
            "ORDER BY SUM(s.totalAmount) DESC")
    List<Object[]> getProfitByCategoryByBranch(Long businessId, Long branchId);

    // ── Owner dashboard: revenue per branch ───────────────────
    @Query("SELECT s.branchId, COALESCE(SUM(s.totalAmount), 0) " +
            "FROM Sale s WHERE s.businessId = :businessId " +
            "GROUP BY s.branchId")
    List<Object[]> getRevenueByBranch(Long businessId);

    // ── Stockout Forecast ─────────────────────────────────────
    @Query("SELECT s.product.id, s.product.name, s.product.category, " +
            "s.product.quantity, SUM(s.quantitySold) " +
            "FROM Sale s " +
            "WHERE s.businessId = :businessId " +
            "AND s.saleDate >= :since " +
            "AND s.product.active = true " +
            "GROUP BY s.product.id, s.product.name, " +
            "s.product.category, s.product.quantity " +
            "ORDER BY s.product.quantity ASC")
    List<Object[]> getProductVelocityByBusiness(Long businessId, LocalDateTime since);

    @Query("SELECT s.product.id, s.product.name, s.product.category, " +
            "s.product.quantity, SUM(s.quantitySold) " +
            "FROM Sale s " +
            "WHERE s.businessId = :businessId " +
            "AND s.branchId = :branchId " +
            "AND s.saleDate >= :since " +
            "AND s.product.active = true " +
            "GROUP BY s.product.id, s.product.name, " +
            "s.product.category, s.product.quantity " +
            "ORDER BY s.product.quantity ASC")
    List<Object[]> getProductVelocityByBranch(
            Long businessId, Long branchId, LocalDateTime since);

    // ── Admin global queries ──────────────────────────────────
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s")
    BigDecimal getTotalRevenue();

    @Query("SELECT COALESCE(SUM(s.quantitySold * s.costAtSale), 0) FROM Sale s")
    BigDecimal getTotalCOGS();

    @Query("SELECT COALESCE(SUM(s.quantitySold), 0) FROM Sale s")
    Double getTotalUnitsSold();

    @Query("SELECT COUNT(s) FROM Sale s")
    long getTotalSalesCount();

    List<Sale> findTop10ByOrderBySaleDateDesc();

    @Query("SELECT s.product.name, SUM(s.quantitySold), SUM(s.totalAmount) " +
            "FROM Sale s GROUP BY s.product.id, s.product.name " +
            "ORDER BY SUM(s.quantitySold) DESC LIMIT 5")
    List<Object[]> getTopSellingProducts();

    @Query("SELECT s.product.category, " +
            "SUM(s.quantitySold), " +
            "SUM(s.totalAmount), " +
            "SUM(s.quantitySold * s.costAtSale) " +
            "FROM Sale s GROUP BY s.product.category " +
            "ORDER BY SUM(s.totalAmount) DESC")
    List<Object[]> getProfitByCategory();

    @Query("SELECT s.product.category, SUM(s.quantitySold), SUM(s.totalAmount) " +
            "FROM Sale s GROUP BY s.product.category " +
            "ORDER BY SUM(s.totalAmount) DESC")
    List<Object[]> getSalesByCategory();

    // ── Admin Dashboard: Top 5 businesses by total revenue ────
    // Returns [businessId, SUM(totalAmount)] ordered DESC.
    // Use Pageable.ofSize(5) to limit results.
    @Query("SELECT s.businessId, SUM(s.totalAmount) " +
            "FROM Sale s WHERE s.saleDate >= :since " +
            "GROUP BY s.businessId " +
            "ORDER BY SUM(s.totalAmount) DESC")
    List<Object[]> getTopBusinessesByRevenue(
            @Param("since") LocalDateTime since, Pageable pageable);
}

