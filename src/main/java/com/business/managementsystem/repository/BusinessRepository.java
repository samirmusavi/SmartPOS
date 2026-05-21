package com.business.managementsystem.repository;

import com.business.managementsystem.model.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessRepository extends JpaRepository<Business, Long> {

    Optional<Business> findByEmail(String email);
    List<Business> findByStatus(Business.Status status);
    List<Business> findByPlan(Business.Plan plan);
    boolean existsByEmail(String email);
    long countByStatus(Business.Status status);
    long countByPlan(Business.Plan plan);

    // ── Admin Dashboard: MRR by plan (only ACTIVE businesses) ─
    long countByStatusAndPlan(Business.Status status, Business.Plan plan);

    // ── Admin Dashboard: Expiry alerts ────────────────────────
    // Businesses with ACTIVE status whose expiry date falls
    // between now and a future date (e.g., now + 7 days).
    List<Business> findByStatusAndExpiryDateBetween(
            Business.Status status,
            LocalDateTime from, LocalDateTime to);

    // ── Admin Dashboard: Businesses joined before a date ──────
    // Used for historical MRR calculation (how many active
    // businesses existed in each past month).
    List<Business> findByStatusAndJoinedDateBefore(
            Business.Status status, LocalDateTime before);
}