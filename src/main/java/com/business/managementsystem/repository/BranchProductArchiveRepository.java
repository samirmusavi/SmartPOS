package com.business.managementsystem.repository;

import com.business.managementsystem.model.BranchProductArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface BranchProductArchiveRepository
        extends JpaRepository<BranchProductArchive, Long> {

    // Check if a product is archived at a specific branch
    Optional<BranchProductArchive> findByBranchIdAndProductId(
            Long branchId, Long productId);

    // All archived product IDs for a branch — used to filter the product list
    @Query("SELECT b.productId FROM BranchProductArchive b " +
            "WHERE b.branchId = :branchId")
    Set<Long> findArchivedProductIdsByBranchId(Long branchId);

    // All archive entries for a branch — used to build the archived products list
    List<BranchProductArchive> findByBranchId(Long branchId);

    // Remove a specific archive entry (restore at this branch)
    @Modifying
    @Transactional
    @Query("DELETE FROM BranchProductArchive b " +
            "WHERE b.branchId = :branchId AND b.productId = :productId")
    void deleteByBranchIdAndProductId(Long branchId, Long productId);

    // Remove all branch archive entries for a product
    // (used when the product is globally deleted)
    @Modifying
    @Transactional
    @Query("DELETE FROM BranchProductArchive b WHERE b.productId = :productId")
    void deleteAllByProductId(Long productId);
}