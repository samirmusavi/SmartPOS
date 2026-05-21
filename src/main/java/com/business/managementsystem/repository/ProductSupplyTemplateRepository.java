package com.business.managementsystem.repository;

import com.business.managementsystem.model.ProductSupplyTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSupplyTemplateRepository
        extends JpaRepository<ProductSupplyTemplate, Long> {

    // All templates for a specific product
    @Query("SELECT t FROM ProductSupplyTemplate t " +
            "JOIN FETCH t.supply " +
            "WHERE t.product.id = :productId " +
            "AND t.businessId = :businessId")
    List<ProductSupplyTemplate> findByProductIdAndBusinessId(
            Long productId, Long businessId);

    // All templates for a business
    @Query("SELECT t FROM ProductSupplyTemplate t " +
            "JOIN FETCH t.product " +
            "JOIN FETCH t.supply " +
            "WHERE t.businessId = :businessId")
    List<ProductSupplyTemplate> findByBusinessId(Long businessId);

    // Find specific product + supply link
    Optional<ProductSupplyTemplate> findByProductIdAndSupplyId(
            Long productId, Long supplyId);

    // ── Delete methods MUST have @Modifying + @Transactional ─────
    // Without these, Spring Data JPA will not execute the DELETE statement
    @Modifying
    @Transactional
    @Query("DELETE FROM ProductSupplyTemplate t WHERE t.product.id = :productId")
    void deleteByProductId(Long productId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ProductSupplyTemplate t " +
            "WHERE t.product.id = :productId AND t.supply.id = :supplyId")
    void deleteByProductIdAndSupplyId(Long productId, Long supplyId);
}