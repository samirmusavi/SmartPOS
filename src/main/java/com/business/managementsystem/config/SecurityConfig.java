package com.business.managementsystem.config;

import com.business.managementsystem.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for JWT-based stateless authentication.
 *
 * Public paths:
 *   - /api/auth/**         → login & register
 *   - /api/admin/**        → admin portal (has its own auth)
 *   - Static resources     → HTML, CSS, JS, uploads, favicon
 *
 * Protected paths:
 *   - /api/**              → requires valid JWT Bearer token
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — stateless JWT API, no cookies/sessions
            .csrf(csrf -> csrf.disable())

            // Stateless session — no server-side sessions
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Path-based authorization
            .authorizeHttpRequests(auth -> auth
                // Public auth endpoints
                .requestMatchers("/api/auth/**").permitAll()

                // Admin portal — has its own auth mechanism
                .requestMatchers("/api/admin/**").permitAll()

                // Static resources
                .requestMatchers(
                    "/*.html", "/css/**", "/js/**",
                    "/uploads/**", "/favicon.ico", "/",
                    "/service-worker.js", "/manifest.json"
                ).permitAll()

                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()

                // Everything else is public (fallback)
                .anyRequest().permitAll()
            )

            // Add JWT filter before Spring's default auth filter
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}