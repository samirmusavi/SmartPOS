package com.business.managementsystem.repository;

import com.business.managementsystem.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByBusinessIdOrderByNameAsc(Long businessId);

    List<Supplier> findByBusinessIdAndIsSupplierTrueOrderByNameAsc(Long businessId);

    List<Supplier> findByBusinessIdAndIsCustomerTrueOrderByNameAsc(Long businessId);

    Optional<Supplier> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByNameAndBusinessId(String name, Long businessId);

    long countByBusinessId(Long businessId);

    long countByBusinessIdAndIsSupplierTrue(Long businessId);

    long countByBusinessIdAndIsCustomerTrue(Long businessId);

    long countByBusinessIdAndIsCustomerTrueAndIsSupplierTrue(Long businessId);

    List<Supplier> findByBusinessIdAndNameContainingIgnoreCase(Long businessId, String name);

    // ── Party code methods ───────────────────────────────────────────
    boolean existsByBusinessIdAndPartyCode(Long businessId, String partyCode);

    java.util.Optional<Supplier> findByBusinessIdAndPartyCode(Long businessId, String partyCode);

    /** Used to find all codes that start with the given prefix (to find next available number). */
    List<Supplier> findByBusinessIdAndPartyCodeStartingWith(Long businessId, String prefix);
}