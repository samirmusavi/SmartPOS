package com.business.managementsystem.repository;

import com.business.managementsystem.model.BranchInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchInventoryRepository
        extends JpaRepository<BranchInventory, Long> {

    Optional<BranchInventory> findByBranchIdAndProductId(
            Long branchId, Long productId);

    List<BranchInventory> findByBranchId(Long branchId);

    List<BranchInventory> findByProductId(Long productId);

    // Get total quantity of a product across all branches of a business
    @Query("SELECT COALESCE(SUM(bi.quantity), 0) " +
            "FROM BranchInventory bi " +
            "JOIN Branch b ON b.id = bi.branchId " +
            "WHERE b.businessId = :businessId " +
            "AND bi.productId = :productId")
    double getTotalQuantityAcrossBranches(Long businessId, Long productId);

    // Get all low stock items for a branch
    @Query("SELECT bi FROM BranchInventory bi " +
            "WHERE bi.branchId = :branchId " +
            "AND bi.quantity < :threshold")
    List<BranchInventory> findLowStockByBranch(Long branchId, int threshold);

    // Sum stock across all branches of a business, grouped by productId.
    // Returns Object[] rows: [productId (Long), totalQty (Long)]
    @Query("SELECT bi.productId, SUM(bi.quantity) " +
            "FROM BranchInventory bi " +
            "JOIN Branch b ON b.id = bi.branchId " +
            "WHERE b.businessId = :businessId " +
            "GROUP BY bi.productId")
    List<Object[]> sumQuantityByProductForBusiness(Long businessId);

    // Sum total_weight_grams across all branches of a business, grouped by productId.
    // Returns Object[] rows: [productId (Long), totalWeight (Double)]
    @Query("SELECT bi.productId, SUM(bi.totalWeightGrams) " +
            "FROM BranchInventory bi " +
            "JOIN Branch b ON b.id = bi.branchId " +
            "WHERE b.businessId = :businessId " +
            "GROUP BY bi.productId")
    List<Object[]> sumWeightByProductForBusiness(Long businessId);

    // Sum total_weight_grams for a single product across all branches of a business
    @Query("SELECT COALESCE(SUM(bi.totalWeightGrams), 0) " +
            "FROM BranchInventory bi " +
            "JOIN Branch b ON b.id = bi.branchId " +
            "WHERE b.businessId = :businessId " +
            "AND bi.productId = :productId")
    Double sumWeightAcrossBranches(Long businessId, Long productId);

}