package com.business.managementsystem.controller;

import com.business.managementsystem.service.PartyLedgerService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * REST endpoints for the party ledger / statement-of-account feature.
 *
 * All endpoints are scoped to a businessId (passed as a request parameter)
 * and a partyId (in the path).  The partyId follows the negative-namespace
 * convention: positive = supplier, negative = customer (abs value = supplierId).
 */
@RestController
@RequestMapping("/api/party-ledger")
public class PartyLedgerController {

    private final PartyLedgerService partyLedgerService;

    public PartyLedgerController(PartyLedgerService partyLedgerService) {
        this.partyLedgerService = partyLedgerService;
    }

    // ── GET /api/party-ledger/{partyId}/statement ────────────────────
    /**
     * Returns the full statement with running AED and metal balances.
     *
     * Query params:
     *   businessId (required)
     *   dateFrom   (optional, yyyy-MM-dd) — entries before this date form the opening balance
     *   dateTo     (optional, yyyy-MM-dd) — entries after this date are excluded
     */
    @GetMapping("/{partyId}/statement")
    public ResponseEntity<Map<String, Object>> getStatement(
            @PathVariable Long partyId,
            @RequestParam Long businessId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        Map<String, Object> statement =
                partyLedgerService.getPartyStatement(businessId, partyId, dateFrom, dateTo);
        return ResponseEntity.ok(statement);
    }

    // ── GET /api/party-ledger/{partyId}/balance ──────────────────────
    /**
     * Returns the current overall AED and metal balance for a party.
     *
     * Query params:
     *   businessId (required)
     */
    @GetMapping("/{partyId}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(
            @PathVariable Long partyId,
            @RequestParam Long businessId) {

        return ResponseEntity.ok(
                partyLedgerService.getPartyBalance(businessId, partyId));
    }

    // ── POST /api/party-ledger/{partyId}/receipt ─────────────────────
    /**
     * Records a receipt (party paid you).
     *
     * Body:
     * {
     *   "businessId":  1,
     *   "branchId":    1,          // optional
     *   "amount":      5000.00,
     *   "partyName":   "Ahmed",    // display name for the narration
     *   "date":        "2025-01-15", // optional, defaults to today
     *   "createdBy":   "admin"     // optional
     * }
     */
    @PostMapping("/{partyId}/receipt")
    public ResponseEntity<Object> postReceipt(
            @PathVariable Long partyId,
            @RequestBody Map<String, Object> body) {

        Long   businessId = toLong(body.get("businessId"));
        Long   branchId   = body.get("branchId") != null ? toLong(body.get("branchId")) : null;
        BigDecimal amount = toBd(body.get("amount"));
        String partyName  = str(body.get("partyName"), "Party");
        LocalDate date    = body.get("date") != null
                ? LocalDate.parse(body.get("date").toString()) : null;
        String createdBy  = str(body.get("createdBy"), "system");

        Object entry = partyLedgerService.postReceiptEntry(
                partyId, partyName, amount, partyName, date, createdBy, businessId, branchId);
        return ResponseEntity.ok(entry);
    }

    // ── POST /api/party-ledger/{partyId}/payment ─────────────────────
    /**
     * Records a payment (you paid the party).
     *
     * Body: same structure as /receipt
     */
    @PostMapping("/{partyId}/payment")
    public ResponseEntity<Object> postPayment(
            @PathVariable Long partyId,
            @RequestBody Map<String, Object> body) {

        Long   businessId = toLong(body.get("businessId"));
        Long   branchId   = body.get("branchId") != null ? toLong(body.get("branchId")) : null;
        BigDecimal amount = toBd(body.get("amount"));
        String partyName  = str(body.get("partyName"), "Party");
        LocalDate date    = body.get("date") != null
                ? LocalDate.parse(body.get("date").toString()) : null;
        String createdBy  = str(body.get("createdBy"), "system");

        Object entry = partyLedgerService.postPaymentEntry(
                partyId, partyName, amount, partyName, date, createdBy, businessId, branchId);
        return ResponseEntity.ok(entry);
    }

    // ── TYPE COERCION UTILITIES ──────────────────────────────────────

    private Long toLong(Object v) {
        if (v == null) throw new IllegalArgumentException("Required field missing");
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(v.toString().trim());
    }

    private BigDecimal toBd(Object v) {
        if (v == null) throw new IllegalArgumentException("Required amount missing");
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(v.toString());
        return new BigDecimal(v.toString().trim());
    }

    private String str(Object v, String defaultValue) {
        return v != null && !v.toString().isBlank() ? v.toString().trim() : defaultValue;
    }
}
