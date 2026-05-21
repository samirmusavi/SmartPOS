package com.business.managementsystem.repository;

import com.business.managementsystem.model.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockTransferRepository
        extends JpaRepository<StockTransfer, Long> {

    // All transfers involving a branch (sent or received)
    @Query("SELECT t FROM StockTransfer t " +
            "WHERE t.businessId = :businessId " +
            "AND (t.fromBranchId = :branchId OR t.toBranchId = :branchId) " +
            "ORDER BY t.createdAt DESC")
    List<StockTransfer> findByBranch(Long businessId, Long branchId);

    // All transfers for a business (owner view)
    List<StockTransfer> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    // Pending transfers TO a specific branch (need to confirm receipt)
    List<StockTransfer> findByToBranchIdAndStatus(
            Long toBranchId, StockTransfer.Status status);

    // Pending transfers FROM a specific branch
    List<StockTransfer> findByFromBranchIdAndStatus(
            Long fromBranchId, StockTransfer.Status status);

    Optional<StockTransfer> findByIdAndBusinessId(Long id, Long businessId);

    long countByToBranchIdAndStatus(Long toBranchId, StockTransfer.Status status);
}