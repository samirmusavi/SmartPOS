package com.business.managementsystem.repository;

import com.business.managementsystem.model.PurchaseReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseReturnItemRepository extends JpaRepository<PurchaseReturnItem, Long> {

    List<PurchaseReturnItem> findByPurchaseReturnIdOrderByIdAsc(Long purchaseReturnId);

    /** Total quantity already returned for a given original purchase item */
    @Query("SELECT COALESCE(SUM(i.returnQuantity), 0) FROM PurchaseReturnItem i " +
           "WHERE i.purchaseItemId = :purchaseItemId")
    double getTotalReturnedQtyByPurchaseItemId(
            @Param("purchaseItemId") Long purchaseItemId);
}
