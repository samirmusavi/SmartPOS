package com.business.managementsystem.repository;

import com.business.managementsystem.model.ReceiptSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceiptSequenceRepository
        extends JpaRepository<ReceiptSequence, Long> {

    // Pessimistic lock prevents two concurrent sales from getting
    // the same sequence number at the same time
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReceiptSequence r " +
            "WHERE r.branchId = :branchId AND r.year = :year")
    Optional<ReceiptSequence> findByBranchIdAndYearForUpdate(
            Long branchId, int year);

    Optional<ReceiptSequence> findByBranchIdAndYear(
            Long branchId, int year);
}