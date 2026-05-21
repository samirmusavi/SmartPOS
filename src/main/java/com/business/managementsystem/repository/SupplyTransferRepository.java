package com.business.managementsystem.repository;

import com.business.managementsystem.model.SupplyTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplyTransferRepository
        extends JpaRepository<SupplyTransfer, Long> {

    List<SupplyTransfer> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    long countByToBranchIdAndStatus(Long toBranchId, SupplyTransfer.Status status);
}
