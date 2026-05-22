package com.business.managementsystem.repository;

import com.business.managementsystem.model.CashEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CashEntryRepository extends JpaRepository<CashEntry, Long> {

    // All entries for a day sheet, sorted so they display in the right order
    List<CashEntry> findByBusinessIdAndBranchIdAndSheetDateOrderBySortOrderAscCreatedAtAsc(
            Long businessId, Long branchId, LocalDate sheetDate);

    // Used to find the highest sortOrder so we can append new manual entries after existing ones
    @Query("SELECT COALESCE(MAX(e.sortOrder), 0) FROM CashEntry e " +
           "WHERE e.businessId = :businessId AND e.branchId = :branchId " +
           "AND e.sheetDate = :date")
    int findMaxSortOrderByDate(
            @Param("businessId") Long businessId,
            @Param("branchId")   Long branchId,
            @Param("date")       LocalDate date);

    // Delete only auto-synced entries (SALE / EXPENSE / RETURN) — manual entries are untouched
    @Modifying
    @Query("DELETE FROM CashEntry e WHERE e.businessId = :businessId " +
           "AND e.branchId = :branchId AND e.sheetDate = :date " +
           "AND e.entryType IN :types")
    void deleteSyncedEntriesByDate(
            @Param("businessId") Long businessId,
            @Param("branchId")   Long branchId,
            @Param("date")       LocalDate date,
            @Param("types")      java.util.List<CashEntry.EntryType> types);
}
