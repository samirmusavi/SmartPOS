package com.business.managementsystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT authentication filter — intercepts every request, extracts
 * the Bearer token from the Authorization header, validates it,
 * and sets Spring Security authentication context.
 *
 * If no token is present, the request continues unauthenticated
 * (Spring Security will handle 401/403 based on path rules).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No token — let the request continue unauthenticated
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // Strip "Bearer "

        try {
            Claims claims = jwtUtil.parseToken(token);

            String userId    = claims.getSubject();
            String role      = claims.get("role", String.class);
            Long businessId  = claims.get("businessId", Long.class);

            // Create authentication with role as granted authority
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,       // principal
                            businessId,   // credentials (handy for downstream)
                            authorities
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException e) {
            // Invalid or expired token — clear context and return 401
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"message\":\"Invalid or expired token. Please log in again.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Skip filtering for public paths — improves performance.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/")
                || path.startsWith("/api/admin/")
                || path.endsWith(".html")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/uploads/")
                || path.equals("/favicon.ico")
                || path.equals("/");
    }
}
