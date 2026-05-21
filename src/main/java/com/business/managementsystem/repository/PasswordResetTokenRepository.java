package com.business.managementsystem.repository;

import com.business.managementsystem.model.PasswordResetToken;
import com.business.managementsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    List<PasswordResetToken> findByUser(User user);

    // Invalidate all prior tokens for a user when a new reset is requested
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.user = :user")
    void deleteAllByUser(User user);
}
