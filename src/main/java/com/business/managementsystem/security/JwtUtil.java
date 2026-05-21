package com.business.managementsystem.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT token utility — generates and validates Bearer tokens.
 *
 * Token payload (claims):
 *   sub        = userId
 *   businessId = multi-tenant business ID
 *   role       = OWNER | MANAGER | CASHIER
 *   email      = user email
 *   iat        = issued at
 *   exp        = expiration
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Generate a signed JWT for an authenticated user.
     */
    public String generateToken(Long userId, Long businessId,
                                String email, String role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("businessId", businessId)
                .claim("role", role)
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Parse and validate a token, returning its claims.
     * Throws JwtException on invalid/expired tokens.
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if a token is valid (parseable and not expired).
     */
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extract userId from token.
     */
    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    /**
     * Extract businessId from token.
     */
    public Long getBusinessId(String token) {
        return parseToken(token).get("businessId", Long.class);
    }

    /**
     * Extract role from token.
     */
    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    /**
     * Extract email from token.
     */
    public String getEmail(String token) {
        return parseToken(token).get("email", String.class);
    }
}
