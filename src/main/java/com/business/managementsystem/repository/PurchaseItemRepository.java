package com.business.managementsystem.repository;

import com.business.managementsystem.model.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {

    List<PurchaseItem> findByPurchaseIdOrderByIdAsc(Long purchaseId);

    @Query("SELECT COUNT(i) FROM PurchaseItem i WHERE i.purchaseId = :purchaseId")
    long countByPurchaseId(@Param("purchaseId") Long purchaseId);
}
