package com.business.managementsystem.repository;

import com.business.managementsystem.model.PartyLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PartyLedgerEntryRepository extends JpaRepository<PartyLedgerEntry, Long> {

    /** All entries for a party ordered chronologically, then by insert order. */
    List<PartyLedgerEntry> findByBusinessIdAndPartyIdOrderByVoucherDateAscCreatedAtAsc(
            Long businessId, Long partyId);

    /** Entries for a party within a date range (inclusive). */
    List<PartyLedgerEntry> findByBusinessIdAndPartyIdAndVoucherDateBetweenOrderByVoucherDateAscCreatedAtAsc(
            Long businessId, Long partyId, LocalDate from, LocalDate to);

    /** Used to locate entries by the originating document (for reversal / display). */
    List<PartyLedgerEntry> findByReferenceIdAndReferenceType(
            Long referenceId, String referenceType);

    /** Hard-deletes all ledger entries linked to a document (used on document delete). */
    @Modifying
    @Query("DELETE FROM PartyLedgerEntry e " +
           "WHERE e.referenceId = :referenceId AND e.referenceType = :referenceType")
    void deleteByReferenceIdAndReferenceType(
            @Param("referenceId")   Long   referenceId,
            @Param("referenceType") String referenceType);

    // ── Running-total queries ────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(e.aedDebit), 0) FROM PartyLedgerEntry e " +
           "WHERE e.businessId = :businessId AND e.partyId = :partyId")
    BigDecimal sumAedDebitByBusinessIdAndPartyId(
            @Param("businessId") Long businessId,
            @Param("partyId")    Long partyId);

    @Query("SELECT COALESCE(SUM(e.aedCredit), 0) FROM PartyLedgerEntry e " +
           "WHERE e.businessId = :businessId AND e.partyId = :partyId")
    BigDecimal sumAedCreditByBusinessIdAndPartyId(
            @Param("businessId") Long businessId,
            @Param("partyId")    Long partyId);

    @Query("SELECT COALESCE(SUM(e.metalDebit), 0) FROM PartyLedgerEntry e " +
           "WHERE e.businessId = :businessId AND e.partyId = :partyId")
    Double sumMetalDebitByBusinessIdAndPartyId(
            @Param("businessId") Long businessId,
            @Param("partyId")    Long partyId);

    @Query("SELECT COALESCE(SUM(e.metalCredit), 0) FROM PartyLedgerEntry e " +
           "WHERE e.businessId = :businessId AND e.partyId = :partyId")
    Double sumMetalCreditByBusinessIdAndPartyId(
            @Param("businessId") Long businessId,
            @Param("partyId")    Long partyId);
}
