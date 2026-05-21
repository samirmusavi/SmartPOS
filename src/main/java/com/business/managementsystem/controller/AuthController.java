package com.business.managementsystem.controller;

import com.business.managementsystem.model.Business;
import com.business.managementsystem.service.AuthService;
import com.business.managementsystem.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService         authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService,
                          PasswordResetService passwordResetService) {
        this.authService          = authService;
        this.passwordResetService = passwordResetService;
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> request) {
        Map<String, Object> session = authService.clientLogin(
                request.get("email"),
                request.get("password")
        );
        return ResponseEntity.ok(session);
    }

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = authService.registerOwner(
                request.get("businessName"),
                request.get("ownerName"),
                request.get("businessEmail"),
                request.get("phone"),
                Business.Plan.BASIC,
                request.get("email"),
                request.get("password")
        );
        return ResponseEntity.ok(response);
    }

    // POST /api/auth/forgot-password
    // Always returns 200 with the same message — never reveals if email exists
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @RequestBody Map<String, String> request) {
        try {
            passwordResetService.requestReset(request.get("email"));
        } catch (Exception ignored) {
            // Swallow all errors — caller always sees success
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "If an account with that email exists, a reset link has been sent."
        ));
    }

    // POST /api/auth/reset-password
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @RequestBody Map<String, String> request) {
        passwordResetService.resetPassword(
                request.get("token"),
                request.get("newPassword")
        );
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password updated successfully. You can now log in."
        ));
    }
}
