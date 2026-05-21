package com.business.managementsystem.controller;

import com.business.managementsystem.model.Branch;
import com.business.managementsystem.service.BranchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    private Long getBusinessId(String header) {
        if (header == null || header.isBlank())
            throw new RuntimeException("Business ID header is required.");
        return Long.parseLong(header);
    }

    // GET /api/branches — all branches for business
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                branchService.getBranches(getBusinessId(h)));
    }

    // GET /api/branches/active — active branches only
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActive(
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                branchService.getActiveBranches(getBusinessId(h)));
    }

    // GET /api/branches/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOne(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                branchService.getBranch(id, getBusinessId(h)));
    }

    // GET /api/branches/{id}/inventory
    @GetMapping("/{id}/inventory")
    public ResponseEntity<List<Map<String, Object>>> getInventory(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                branchService.getBranchInventory(id, getBusinessId(h)));
    }

    // POST /api/branches
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                branchService.createBranch(
                        getBusinessId(h),
                        request.get("name"),
                        request.get("address"),
                        request.get("phone"),
                        request.get("city"),
                        request.get("country")
                )
        );
    }

    // PUT /api/branches/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestBody Map<String, String> request) {

        Branch.Status status = null;
        if (request.get("status") != null) {
            status = Branch.Status.valueOf(request.get("status"));
        }

        return ResponseEntity.ok(
                branchService.updateBranch(
                        id, getBusinessId(h),
                        request.get("name"),
                        request.get("address"),
                        request.get("phone"),
                        request.get("city"),
                        request.get("country"),
                        status
                )
        );
    }
}