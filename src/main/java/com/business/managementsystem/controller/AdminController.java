package com.business.managementsystem.controller;

import com.business.managementsystem.model.Admin;
import com.business.managementsystem.model.Business;
import com.business.managementsystem.model.User;
import com.business.managementsystem.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final com.business.managementsystem.service.IndustryTemplateService industryTemplateService;

    public AdminController(AdminService adminService,
                           com.business.managementsystem.service.IndustryTemplateService industryTemplateService) {
        this.adminService = adminService;
        this.industryTemplateService = industryTemplateService;
    }

    // ── LOGIN ────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        Admin admin = adminService.login(username, password);
        Map<String, Object> response = new HashMap<>();
        response.put("success",  true);
        response.put("adminId",  admin.getId());
        response.put("fullName", admin.getFullName());
        response.put("message",  "Login successful");
        return ResponseEntity.ok(response);
    }

    // ── DASHBOARD STATS ──────────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // ── GET ALL BUSINESSES ───────────────────────────────────
    @GetMapping("/businesses")
    public ResponseEntity<List<Business>> getAllBusinesses() {
        return ResponseEntity.ok(adminService.getAllBusinesses());
    }

    // ── GET BUSINESS BY ID ───────────────────────────────────
    @GetMapping("/businesses/{id}")
    public ResponseEntity<Business> getBusinessById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getBusinessById(id));
    }

    // ── CREATE BUSINESS ──────────────────────────────────────
    @PostMapping("/businesses")
    public ResponseEntity<Business> createBusiness(
            @RequestBody Map<String, String> request) {
        Business business = adminService.createBusiness(
                request.get("businessName"),
                request.get("ownerName"),
                request.get("email"),
                request.get("phone"),
                Business.Plan.valueOf(request.get("plan"))
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(business);
    }

    // ── UPDATE STATUS ────────────────────────────────────────
    @PutMapping("/businesses/{id}/status")
    public ResponseEntity<Business> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        Business business = adminService.updateBusinessStatus(
                id, Business.Status.valueOf(request.get("status"))
        );
        return ResponseEntity.ok(business);
    }

    // ── UPDATE PLAN ──────────────────────────────────────────
    @PutMapping("/businesses/{id}/plan")
    public ResponseEntity<Business> updatePlan(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        Business business = adminService.updateBusinessPlan(
                id, Business.Plan.valueOf(request.get("plan"))
        );
        return ResponseEntity.ok(business);
    }

    // ── DELETE BUSINESS ──────────────────────────────────────
    @DeleteMapping("/businesses/{id}")
    public ResponseEntity<Void> deleteBusiness(@PathVariable Long id) {
        adminService.deleteBusiness(id);
        return ResponseEntity.noContent().build();
    }

    // ── GET USERS BY BUSINESS ────────────────────────────────
    @GetMapping("/businesses/{id}/users")
    public ResponseEntity<List<User>> getUsersByBusiness(
            @PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUsersByBusiness(id));
    }

    // ── APPLY NICHE TEMPLATE ────────────────────────────────
    @PostMapping("/businesses/{id}/niche-template")
    public ResponseEntity<Map<String, String>> applyNicheTemplate(
            @PathVariable Long id,
            @RequestParam String niche) {
        industryTemplateService.applyTemplate(id, niche);
        Map<String, String> res = new HashMap<>();
        res.put("message", "Template applied successfully.");
        return ResponseEntity.ok(res);
    }
}