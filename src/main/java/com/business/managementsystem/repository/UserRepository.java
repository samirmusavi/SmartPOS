package com.business.managementsystem.repository;

import com.business.managementsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    List<User> findByBusinessId(Long businessId);
    boolean existsByEmail(String email);
    long countByBusinessId(Long businessId);
}
