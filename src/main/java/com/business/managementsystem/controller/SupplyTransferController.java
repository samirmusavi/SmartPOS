package com.business.managementsystem.controller;

import com.business.managementsystem.service.SupplyTransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/supply-transfers")
public class SupplyTransferController {

    private final SupplyTransferService service;

    public SupplyTransferController(SupplyTransferService service) {
        this.service = service;
    }

    private Long getBusinessId(String header) {
        if (header == null || header.isBlank())
            throw new RuntimeException("Business ID header is required.");
        return Long.parseLong(header);
    }

    private Long getBranchId(String header) {
        if (header == null || header.isBlank()) return null;
        return Long.parseLong(header);
    }

    // GET /api/supply-transfers
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestHeader(value = "X-Business-Id",
                    required = false) String bh) {
        return ResponseEntity.ok(service.getAll(getBusinessId(bh)));
    }

    // GET /api/supply-transfers/pending-incoming/count
    @GetMapping("/pending-incoming/count")
    public ResponseEntity<Map<String, Object>> getPendingCount(
            @RequestHeader(value = "X-Branch-Id",
                    required = false) String brh) {
        Long branchId = getBranchId(brh);
        long count = branchId != null
                ? service.getPendingIncomingCount(branchId) : 0;
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return ResponseEntity.ok(result);
    }

    // POST /api/supply-transfers
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestHeader(value = "X-Business-Id",
                    required = false) String bh,
            @RequestBody Map<String, Object> request) {

        Long businessId   = getBusinessId(bh);
        Long fromBranchId = Long.parseLong(
                request.get("fromBranchId").toString());
        Long toBranchId   = Long.parseLong(
                request.get("toBranchId").toString());
        Long supplyId     = Long.parseLong(
                request.get("supplyId").toString());
        int quantity      = Integer.parseInt(
                request.get("quantity").toString());
        String notes      = request.containsKey("notes")
                ? (String) request.get("notes") : null;
        String createdBy  = request.containsKey("createdBy")
                ? (String) request.get("createdBy") : null;

        return ResponseEntity.status(HttpStatus.CREATED).body(
                service.createTransfer(businessId, fromBranchId,
                        toBranchId, supplyId, quantity,
                        notes, createdBy));
    }

    // PUT /api/supply-transfers/{id}/complete
    @PutMapping("/{id}/complete")
    public ResponseEntity<Map<String, Object>> complete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id",
                    required = false) String bh,
            @RequestBody Map<String, String> request) {

        String completedBy = request.getOrDefault("completedBy", null);
        return ResponseEntity.ok(
                service.completeTransfer(id, getBusinessId(bh),
                        completedBy));
    }

    // PUT /api/supply-transfers/{id}/cancel
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id",
                    required = false) String bh) {
        return ResponseEntity.ok(
                service.cancelTransfer(id, getBusinessId(bh)));
    }
}
