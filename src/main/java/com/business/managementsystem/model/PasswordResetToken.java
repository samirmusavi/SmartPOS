package com.business.managementsystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user this token belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // BCrypt hash of the raw token — never stored plain
    @Column(nullable = false, length = 255)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // Null until consumed
    @Column
    private LocalDateTime usedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PasswordResetToken() {}

    public PasswordResetToken(User user, String tokenHash, LocalDateTime expiresAt) {
        this.user      = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    // Getters & setters
    public Long getId()                        { return id; }
    public User getUser()                      { return user; }
    public String getTokenHash()               { return tokenHash; }
    public LocalDateTime getExpiresAt()        { return expiresAt; }
    public LocalDateTime getUsedAt()           { return usedAt; }
    public void setUsedAt(LocalDateTime t)     { this.usedAt = t; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
}
