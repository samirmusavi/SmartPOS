package com.business.managementsystem.controller;

import com.business.managementsystem.service.MetalFixService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for the Hold-Price Fix (HPF / Metal Fix) feature.
 *
 * Fix lifecycle:
 *   POST   /api/metal-fixes               → createFix   (OPEN)
 *   PUT    /api/metal-fixes/{id}/fix       → markAsFixed (FIXED)
 *   PUT    /api/metal-fixes/{id}/settle    → settleFix   (SETTLED)
 */
@RestController
@RequestMapping("/api/metal-fixes")
public class MetalFixController {

    private final MetalFixService metalFixService;

    public MetalFixController(MetalFixService metalFixService) {
        this.metalFixService = metalFixService;
    }

    // ── POST /api/metal-fixes ────────────────────────────────────────
    /**
     * Creates a new metal fix (HPF).
     *
     * Required body fields:
     *   businessId, partyId, partyName, fixType (SALE_FIX|PURCHASE_FIX),
     *   metalType (GOLD|SILVER), weightGrams, transactionRate
     *
     * Optional: branchId, purity, discountPremium, discountPremiumType,
     *           exchangeRate, marginPercent, linkedSaleId, linkedPurchaseId,
     *           notes, createdBy
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createFix(
            @RequestBody Map<String, Object> body) {
        String createdBy = body.get("createdBy") != null
                ? body.get("createdBy").toString() : "system";
        return ResponseEntity.ok(metalFixService.createFix(body, createdBy));
    }

    // ── GET /api/metal-fixes ─────────────────────────────────────────
    /**
     * Lists fixes with optional filters.
     *
     * Query params:
     *   businessId (required)
     *   branchId   (optional)
     *   status     (optional: OPEN | FIXED | SETTLED)
     *   page       (default 0)
     *   size       (default 20)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getFixes(
            @RequestParam Long businessId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                metalFixService.getFixes(businessId, branchId, status, page, size));
    }

    // ── GET /api/metal-fixes/open ────────────────────────────────────
    /**
     * Returns all OPEN fixes for a business (and optional branch).
     * Useful for dropdown/select pickers in the UI.
     *
     * Query params:
     *   businessId (required)
     *   branchId   (optional)
     */
    @GetMapping("/open")
    public ResponseEntity<List<Map<String, Object>>> getOpenFixes(
            @RequestParam Long businessId,
            @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(metalFixService.getOpenFixes(businessId, branchId));
    }

    // ── GET /api/metal-fixes/dashboard ──────────────────────────────
    /**
     * Returns summary counts and open AED exposure for dashboard cards.
     *
     * Query params:
     *   businessId (required)
     *   branchId   (optional)
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(
            @RequestParam Long businessId,
            @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(metalFixService.getDashboardCounts(businessId, branchId));
    }

    // ── GET /api/metal-fixes/party ──────────────────────────────────
    /**
     * Returns all fixes for a specific party.
     *
     * Query params:
     *   businessId (required)
     *   partyId    (required)
     */
    @GetMapping("/party")
    public ResponseEntity<List<Map<String, Object>>> getFixesByParty(
            @RequestParam Long businessId,
            @RequestParam Long partyId) {
        return ResponseEntity.ok(metalFixService.getFixesByParty(businessId, partyId));
    }

    // ── GET /api/metal-fixes/{id} ────────────────────────────────────
    /**
     * Returns a single fix with its settlements.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getFixById(
            @PathVariable Long id) {
        return ResponseEntity.ok(metalFixService.getFixById(id));
    }

    // ── GET /api/metal-fixes/unfixed-sales ──────────────────────────
    /**
     * Returns all UNFIXED_AT_TRADE sales with remaining open weight.
     * Used by the fixing UI to present a pick-list of positions available to price.
     *
     * Query params:
     *   businessId (required)
     *   branchId   (optional)
     */
    @GetMapping("/unfixed-sales")
    public ResponseEntity<List<Map<String, Object>>> getOpenUnfixedSales(
            @RequestParam Long businessId,
            @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(metalFixService.getOpenUnfixedSales(businessId, branchId));
    }

    // ── GET /api/metal-fixes/unfixed-purchases ───────────────────────
    /**
     * Returns all UNFIXED_AT_TRADE purchases with remaining open weight.
     *
     * Query params:
     *   businessId (required)
     *   branchId   (optional)
     */
    @GetMapping("/unfixed-purchases")
    public ResponseEntity<List<Map<String, Object>>> getOpenUnfixedPurchases(
            @RequestParam Long businessId,
            @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(metalFixService.getOpenUnfixedPurchases(businessId, branchId));
    }

    // ── GET /api/metal-fixes/exposure ───────────────────────────
    /**
     * Derives open unfixed-position exposure from existing Sale/Purchase records.
     * Returns total open weight, net position, and optional AED estimation.
     * NO new persisted table — pure aggregation over existing UNFIXED_AT_TRADE records.
     *
     * Query params:
     *   businessId   (required)
     *   branchId     (optional)
     *   ozRate       (optional: live $/oz spot rate for AED estimation)
     *   exchangeRate (optional: USD→AED rate, defaults to 3.6740)
     */
    @GetMapping("/exposure")
    public ResponseEntity<Map<String, Object>> getExposureDashboard(
            @RequestParam Long businessId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) BigDecimal ozRate,
            @RequestParam(required = false) BigDecimal exchangeRate) {
        return ResponseEntity.ok(
                metalFixService.getExposureDashboard(businessId, branchId, ozRate, exchangeRate));
    }

    // ── PUT /api/metal-fixes/{id}/fix ────────────────────────────────
    /**
     * Marks a fix as FIXED at a confirmed market rate.
     *
     * Required body: fixedRate
     * Optional:      fixedDate (yyyy-MM-dd), notes
     */
    @PutMapping("/{id}/fix")
    public ResponseEntity<Map<String, Object>> markAsFixed(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(metalFixService.markAsFixed(id, body));
    }

    // ── PUT /api/metal-fixes/{id}/settle ────────────────────────────
    /**
     * Settles a fix — creates FixSettlement + SFX ledger entry.
     *
     * Required body: fixedRate
     * Optional:      settlementDate (yyyy-MM-dd), exchangeRate, notes, settledBy
     */
    @PutMapping("/{id}/settle")
    public ResponseEntity<Map<String, Object>> settleFix(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String settledBy = body.get("settledBy") != null
                ? body.get("settledBy").toString() : "system";
        return ResponseEntity.ok(metalFixService.settleFix(id, body, settledBy));
    }
}
