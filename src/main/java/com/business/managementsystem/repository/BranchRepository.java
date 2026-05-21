package com.business.managementsystem.repository;

import com.business.managementsystem.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    List<Branch> findByBusinessIdOrderByIsMainBranchDescNameAsc(Long businessId);

    List<Branch> findByBusinessIdAndStatus(Long businessId, Branch.Status status);

    Optional<Branch> findByBusinessIdAndIsMainBranchTrue(Long businessId);

    Optional<Branch> findByIdAndBusinessId(Long id, Long businessId);

    long countByBusinessId(Long businessId);

    boolean existsByBusinessIdAndName(Long businessId, String name);
}