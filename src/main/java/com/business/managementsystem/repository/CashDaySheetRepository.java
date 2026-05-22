package com.business.managementsystem.repository;

import com.business.managementsystem.model.CashDaySheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CashDaySheetRepository extends JpaRepository<CashDaySheet, Long> {

    Optional<CashDaySheet> findByBusinessIdAndBranchIdAndSheetDate(
            Long businessId, Long branchId, LocalDate sheetDate);

    // Find the most recent sheet strictly before a given date
    // Used to auto-fill today's opening balance from yesterday's closing
    @Query("SELECT s FROM CashDaySheet s " +
           "WHERE s.businessId = :businessId AND s.branchId = :branchId " +
           "AND s.sheetDate < :date " +
           "ORDER BY s.sheetDate DESC")
    java.util.List<CashDaySheet> findLatestSheetBefore(
            @Param("businessId") Long businessId,
            @Param("branchId")   Long branchId,
            @Param("date")       LocalDate date);
}
