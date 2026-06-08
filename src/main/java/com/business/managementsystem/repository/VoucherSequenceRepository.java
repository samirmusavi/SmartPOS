package com.business.managementsystem.repository;

import com.business.managementsystem.model.VoucherSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherSequenceRepository extends JpaRepository<VoucherSequence, Long> {

    /**
     * Pessimistic write lock prevents two concurrent requests from getting
     * the same sequence number simultaneously.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VoucherSequence v " +
           "WHERE v.businessId = :businessId AND v.voucherType = :voucherType")
    Optional<VoucherSequence> findForUpdate(
            @Param("businessId")   Long   businessId,
            @Param("voucherType")  String voucherType);
}
