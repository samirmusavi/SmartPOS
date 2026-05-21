package com.business.managementsystem.repository;

import com.business.managementsystem.model.SupplyAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplyAdjustmentRepository
        extends JpaRepository<SupplyAdjustment, Long> {

    List<SupplyAdjustment> findByBusinessIdOrderByAdjustedAtDesc(Long businessId);

    List<SupplyAdjustment> findBySupplyIdOrderByAdjustedAtDesc(Long supplyId);
}