package com.business.managementsystem.repository;

import com.business.managementsystem.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByBusinessIdOrderByFullNameAsc(Long businessId);

    List<Customer> findByBusinessIdOrderByTotalSpentDesc(Long businessId);

    List<Customer> findByBusinessIdOrderByLastVisitAtDesc(Long businessId);

    Optional<Customer> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByPhoneAndBusinessId(String phone, Long businessId);

    boolean existsByEmailAndBusinessId(String email, Long businessId);

    long countByBusinessId(Long businessId);

    @Query("SELECT COALESCE(SUM(c.totalSpent), 0) FROM Customer c WHERE c.businessId = :businessId")
    java.math.BigDecimal getTotalRevenueFromCustomers(Long businessId);

    @Query("SELECT c FROM Customer c WHERE c.businessId = :businessId " +
            "AND (LOWER(c.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Customer> searchCustomers(Long businessId, String query);
}