package com.business.managementsystem.repository;

import com.business.managementsystem.model.FixSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FixSettlementRepository extends JpaRepository<FixSettlement, Long> {

    List<FixSettlement> findByMetalFixIdOrderByCreatedAtDesc(Long metalFixId);

    List<FixSettlement> findByBusinessIdAndSettlementDateBetweenOrderBySettlementDateDesc(
            Long businessId, LocalDate from, LocalDate to);

    List<FixSettlement> findByBusinessIdAndPartyIdOrderBySettlementDateDesc(
            Long businessId, Long partyId);
}
