package com.business.managementsystem.repository;

import com.business.managementsystem.model.BranchSupplyInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchSupplyInventoryRepository
        extends JpaRepository<BranchSupplyInventory, Long> {

    Optional<BranchSupplyInventory> findByBranchIdAndSupplyId(
            Long branchId, Long supplyId);

    List<BranchSupplyInventory> findByBranchId(Long branchId);

    List<BranchSupplyInventory> findBySupplyId(Long supplyId);

    // Sum of this supply's quantity across all branches for a business.
    // Used to sync supply.quantity (global total) after any branch operation.
    @Query("SELECT COALESCE(SUM(bsi.quantity), 0) " +
            "FROM BranchSupplyInventory bsi " +
            "JOIN Branch b ON b.id = bsi.branchId " +
            "WHERE b.businessId = :businessId " +
            "AND bsi.supplyId = :supplyId")
    int getTotalQuantityAcrossBranches(Long businessId, Long supplyId);

    // Low stock entries for a branch — quantity at or below the supply's threshold
    @Query("SELECT bsi FROM BranchSupplyInventory bsi " +
            "JOIN Supply s ON s.id = bsi.supplyId " +
            "WHERE bsi.branchId = :branchId " +
            "AND bsi.quantity <= s.minimumThreshold " +
            "ORDER BY bsi.quantity ASC")
    List<BranchSupplyInventory> findLowStockByBranch(Long branchId);

    // Out-of-stock entries for a branch
    @Query("SELECT bsi FROM BranchSupplyInventory bsi " +
            "WHERE bsi.branchId = :branchId AND bsi.quantity = 0")
    List<BranchSupplyInventory> findOutOfStockByBranch(Long branchId);
}