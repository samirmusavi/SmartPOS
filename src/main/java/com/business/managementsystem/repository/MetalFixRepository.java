package com.business.managementsystem.repository;

import com.business.managementsystem.model.MetalFix;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MetalFixRepository extends JpaRepository<MetalFix, Long> {

    Page<MetalFix> findByBusinessIdOrderByCreatedAtDesc(Long businessId, Pageable pageable);

    Page<MetalFix> findByBusinessIdAndBranchIdOrderByCreatedAtDesc(
            Long businessId, Long branchId, Pageable pageable);

    List<MetalFix> findByBusinessIdAndPartyIdOrderByCreatedAtDesc(
            Long businessId, Long partyId);

    List<MetalFix> findByBusinessIdAndStatusOrderByCreatedAtDesc(
            Long businessId, String status);

    List<MetalFix> findByBusinessIdAndBranchIdAndStatusOrderByCreatedAtDesc(
            Long businessId, Long branchId, String status);

    @Query("SELECT f FROM MetalFix f " +
           "WHERE f.businessId = :businessId AND f.branchId = :branchId " +
           "AND f.createdAt >= :from AND f.createdAt < :to " +
           "ORDER BY f.createdAt DESC")
    List<MetalFix> findByBusinessIdAndBranchIdAndCreatedAtBetween(
            @Param("businessId") Long businessId,
            @Param("branchId")   Long branchId,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to);

    /** Summary counts for dashboard cards */
    long countByBusinessIdAndStatus(Long businessId, String status);

    long countByBusinessIdAndBranchIdAndStatus(Long businessId, Long branchId, String status);

    @Query("SELECT COALESCE(SUM(f.totalAed), 0) FROM MetalFix f " +
           "WHERE f.businessId = :businessId AND f.status = :status")
    java.math.BigDecimal sumTotalAedByBusinessIdAndStatus(
            @Param("businessId") Long businessId,
            @Param("status")     String status);
}
