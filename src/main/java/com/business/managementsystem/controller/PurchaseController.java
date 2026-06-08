package com.business.managementsystem.controller;

import com.business.managementsystem.service.PurchaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    // ── Extract headers ───────────────────────────────────────
    private Long getBusinessId(String header) {
        if (header == null || header.isBlank())
            throw new RuntimeException("Business ID header is required.");
        return Long.parseLong(header.trim());
    }

    private Long getBranchId(String header) {
        if (header == null || header.isBlank()) return null;
        return Long.parseLong(header.trim());
    }

    // ── POST /api/purchases ───────────────────────────────────
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @RequestBody Map<String, Object> body) {

        try {
            Map<String, Object> result = purchaseService.createPurchase(
                    getBusinessId(bh), getBranchId(brh), body);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to create purchase."));
        }
    }

    // ── GET /api/purchases ────────────────────────────────────
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "20") int    size,
            @RequestParam(required = false)    String dateFrom,
            @RequestParam(required = false)    String dateTo,
            @RequestParam(required = false)    String search) {

        Map<String, Object> result = purchaseService.getPurchases(
                getBusinessId(bh), getBranchId(brh),
                page, size, dateFrom, dateTo, search);
        return ResponseEntity.ok(result);
    }

    // ── GET /api/purchases/summary ────────────────────────────
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {

        Map<String, Object> result = purchaseService.getSummary(
                getBusinessId(bh), getBranchId(brh), dateFrom, dateTo);
        return ResponseEntity.ok(result);
    }

    // ── GET /api/purchases/{id} ───────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String bh) {

        try {
            return ResponseEntity.ok(purchaseService.getPurchaseById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── DELETE /api/purchases/{id} ────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String bh) {

        try {
            purchaseService.deletePurchase(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
