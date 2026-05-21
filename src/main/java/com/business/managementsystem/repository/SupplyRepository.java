package com.business.managementsystem.repository;

import com.business.managementsystem.model.Supply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplyRepository extends JpaRepository<Supply, Long> {

    List<Supply> findByBusinessIdOrderByNameAsc(Long businessId);

    Optional<Supply> findByIdAndBusinessId(Long id, Long businessId);

    List<Supply> findByBusinessIdAndCategory(Long businessId, String category);

    // All supplies at or below their minimum threshold
    @Query("SELECT s FROM Supply s WHERE s.businessId = :businessId " +
            "AND s.quantity <= s.minimumThreshold " +
            "ORDER BY s.quantity ASC")
    List<Supply> findLowStockByBusinessId(Long businessId);

    // All supplies with zero quantity
    @Query("SELECT s FROM Supply s WHERE s.businessId = :businessId " +
            "AND s.quantity = 0")
    List<Supply> findOutOfStockByBusinessId(Long businessId);

    long countByBusinessId(Long businessId);

    @Query("SELECT COUNT(s) FROM Supply s WHERE s.businessId = :businessId " +
            "AND s.quantity <= s.minimumThreshold")
    long countLowStockByBusinessId(Long businessId);

    List<Supply> findBySupplierIdAndBusinessId(Long supplierId, Long businessId);
}