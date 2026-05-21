package com.business.managementsystem.repository;

import com.business.managementsystem.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByBusinessIdOrderByNameAsc(Long businessId);

    Optional<Supplier> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByNameAndBusinessId(String name, Long businessId);

    long countByBusinessId(Long businessId);
}