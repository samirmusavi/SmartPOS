package com.business.managementsystem.service;

import com.business.managementsystem.model.PasswordResetToken;
import com.business.managementsystem.model.User;
import com.business.managementsystem.repository.PasswordResetTokenRepository;
import com.business.managementsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository               userRepository;
    private final PasswordEncoder              passwordEncoder;
    private final EmailService                 emailService;

    @Value("${app.base-url:http://localhost:8082}")
    private String baseUrl;

    // Simple in-memory rate limiter: email → last request time
    private final ConcurrentHashMap<String, LocalDateTime> rateLimitMap = new ConcurrentHashMap<>();
    private static final int RATE_LIMIT_MINUTES = 2;
    private static final int TOKEN_EXPIRY_MINUTES = 30;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService    = emailService;
    }

    /**
     * Initiates a password reset for the given email.
     * Always returns the same message whether the email exists or not
     * (security: don't reveal whether an account exists).
     */
    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) return;
        String normalised = email.trim().toLowerCase();

        // Rate-limit: allow one request per email per RATE_LIMIT_MINUTES
        LocalDateTime lastRequest = rateLimitMap.get(normalised);
        if (lastRequest != null &&
                lastRequest.plusMinutes(RATE_LIMIT_MINUTES).isAfter(LocalDateTime.now())) {
            // Silently swallow — caller always sees the same success message
            return;
        }
        rateLimitMap.put(normalised, LocalDateTime.now());

        // Look up user — if not found, do nothing (don't reveal)
        Optional<User> userOpt = userRepository.findByEmail(normalised);
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();

        // Invalidate any existing tokens for this user
        tokenRepository.deleteAllByUser(user);

        // Generate a secure random raw token
        String rawToken = UUID.randomUUID().toString().replace("-", "") +
                          UUID.randomUUID().toString().replace("-", "");

        // Hash it for storage
        String tokenHash = passwordEncoder.encode(rawToken);

        // Persist
        PasswordResetToken resetToken = new PasswordResetToken(
                user,
                tokenHash,
                LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES)
        );
        tokenRepository.save(resetToken);

        // Send email with the RAW token in the link
        String resetLink = baseUrl + "/reset-password.html?token=" + rawToken;
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetLink);
    }

    /**
     * Validates the token and updates the user's password.
     * Throws RuntimeException with a user-friendly message on any failure.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new RuntimeException("Invalid or expired reset link.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters.");
        }

        // Find all non-used, non-expired tokens and BCrypt-compare
        List<PasswordResetToken> candidates = tokenRepository.findAll().stream()
                .filter(t -> !t.isUsed() && !t.isExpired())
                .toList();

        PasswordResetToken matched = null;
        for (PasswordResetToken candidate : candidates) {
            if (passwordEncoder.matches(rawToken, candidate.getTokenHash())) {
                matched = candidate;
                break;
            }
        }

        if (matched == null) {
            throw new RuntimeException("This reset link is invalid or has expired. Please request a new one.");
        }

        // Mark token as used
        matched.setUsedAt(LocalDateTime.now());
        tokenRepository.save(matched);

        // Update password
        User user = matched.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
