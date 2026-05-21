package com.business.managementsystem.controller;

import com.business.managementsystem.model.User;
import com.business.managementsystem.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/staff", "/api/users"})
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    private Long getBusinessId(String header) {
        if (header == null || header.isBlank())
            throw new RuntimeException("Business ID header is required.");
        return Long.parseLong(header);
    }

    // GET /api/users/me — current user profile
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe(
            @RequestHeader(value = "X-User-Id", required = false) String uh) {
        if (uh == null || uh.isBlank())
            throw new RuntimeException("User ID header is required.");
        User user = userService.getUserById(Long.parseLong(uh));
        return ResponseEntity.ok(userService.toMap(user));
    }

    // POST /api/users/me/change-password
    @PostMapping("/me/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestHeader(value = "X-User-Id", required = false) String uh,
            @RequestBody Map<String, String> request) {
        if (uh == null || uh.isBlank())
            throw new RuntimeException("User ID header is required.");
        userService.changePassword(
                Long.parseLong(uh),
                request.get("currentPassword"),
                request.get("newPassword")
        );
        return ResponseEntity.ok(Map.of("success", true,
                "message", "Password changed successfully."));
    }

    // GET /api/staff
    // Returns all staff with their assigned branches included
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getStaff(
            @RequestHeader(value = "X-Business-Id",
                    required = false) String h) {
        return ResponseEntity.ok(
                userService.getStaffByBusiness(getBusinessId(h)));
    }

    // POST /api/staff
    // Body: { fullName, email, password, role, branchIds: [1,2] }
    @PostMapping
    public ResponseEntity<Map<String, Object>> addStaff(
            @RequestHeader(value = "X-Business-Id",
                    required = false) String h,
            @RequestBody Map<String, Object> request) {

        @SuppressWarnings("unchecked")
        List<Long> branchIds = request.get("branchIds") != null
                ? ((List<?>) request.get("branchIds")).stream()
                .map(o -> Long.parseLong(o.toString()))
                .toList()
                : List.of();

        Map<String, Object> result = userService.addStaff(
                getBusinessId(h),
                request.get("fullName").toString(),
                request.get("email").toString(),
                request.get("password").toString(),
                User.Role.valueOf(request.get("role").toString()),
                branchIds
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // PUT /api/staff/{id}/status
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(
                userService.updateStaffStatus(
                        id, User.Status.valueOf(request.get("status"))));
    }

    // PUT /api/staff/{id}/role
    @PutMapping("/{id}/role")
    public ResponseEntity<Map<String, Object>> updateRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(
                userService.updateStaffRole(
                        id, User.Role.valueOf(request.get("role"))));
    }

    // PUT /api/staff/{id}/branches
    // Assigns a staff member to one or more branches
    // Body: { branchIds: [1, 2, 3] }
    // Replaces all existing assignments
    @PutMapping("/{id}/branches")
    public ResponseEntity<Map<String, Object>> assignBranches(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id",
                    required = false) String h,
            @RequestBody Map<String, Object> request) {

        @SuppressWarnings("unchecked")
        List<Long> branchIds = request.get("branchIds") != null
                ? ((List<?>) request.get("branchIds")).stream()
                .map(o -> Long.parseLong(o.toString()))
                .toList()
                : List.of();

        return ResponseEntity.ok(
                userService.assignBranchesToUser(
                        id, getBusinessId(h), branchIds));
    }

    // DELETE /api/staff/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteStaff(
            @PathVariable Long id) {
        userService.deleteStaff(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // GET /api/staff/count
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getCount(
            @RequestHeader(value = "X-Business-Id",
                    required = false) String h) {
        long count = userService.getStaffCount(getBusinessId(h));
        return ResponseEntity.ok(Map.of("count", count));
    }
}