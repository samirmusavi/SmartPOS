package com.business.managementsystem.controller;

import com.business.managementsystem.service.PartyLedgerService;
import com.business.managementsystem.service.StatementPdfService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final StatementPdfService statementPdfService;

    public PartyLedgerController(PartyLedgerService partyLedgerService,
                                  StatementPdfService statementPdfService) {
        this.partyLedgerService  = partyLedgerService;
        this.statementPdfService = statementPdfService;
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

    // ── GET /api/party-ledger/{partyId}/statement/pdf ────────────────
    /**
     * Generates and streams a Statement of Account as a PDF.
     *
     * Query params:
     *   businessId (required)
     *   dateFrom   (optional, yyyy-MM-dd)
     *   dateTo     (optional, yyyy-MM-dd)
     *   branchId   (optional — defaults to the main branch)
     *
     * Returns Content-Type: application/pdf, Content-Disposition: inline
     * so the browser displays it in a new tab.
     */
    @GetMapping("/{partyId}/statement/pdf")
    public ResponseEntity<byte[]> getStatementPdf(
            @PathVariable Long partyId,
            @RequestParam Long businessId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Long branchId) {

        try {
            byte[] pdfBytes = statementPdfService.generateStatementPdf(
                    businessId, partyId, dateFrom, dateTo, branchId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"statement_" + partyId + ".pdf\"");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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

    // ── POST /api/party-ledger/{partyId}/metal-receipt ───────────────
    /**
     * Records a metal receipt (weight-only, no AED movement — MRC voucher).
     *
     * Body:
     * { "businessId":1, "branchId":1, "weightGrams":50.000, "metalType":"GOLD",
     *   "purity":"995", "notes":"...", "partyName":"Ahmed", "date":"2025-01-15", "createdBy":"admin" }
     */
    @PostMapping("/{partyId}/metal-receipt")
    public ResponseEntity<Object> postMetalReceipt(
            @PathVariable Long partyId,
            @RequestBody Map<String, Object> body) {

        Long      businessId  = toLong(body.get("businessId"));
        Long      branchId    = body.get("branchId") != null ? toLong(body.get("branchId")) : null;
        Double    weightGrams = toDouble(body.get("weightGrams"));
        String    metalType   = str(body.get("metalType"), "GOLD");
        String    purity      = str(body.get("purity"), null);
        String    notes       = str(body.get("notes"), null);
        String    partyName   = str(body.get("partyName"), "Party");
        LocalDate date        = body.get("date") != null
                ? LocalDate.parse(body.get("date").toString()) : null;
        String    createdBy   = str(body.get("createdBy"), "system");

        Object entry = partyLedgerService.postMetalReceiptEntry(
                partyId, partyName, weightGrams, metalType, purity,
                notes, date, createdBy, businessId, branchId);
        return ResponseEntity.ok(entry);
    }

    // ── POST /api/party-ledger/{partyId}/metal-payment ───────────────
    /**
     * Records a metal payment (weight-only, no AED movement — MPY voucher).
     *
     * Body: same structure as /metal-receipt
     */
    @PostMapping("/{partyId}/metal-payment")
    public ResponseEntity<Object> postMetalPayment(
            @PathVariable Long partyId,
            @RequestBody Map<String, Object> body) {

        Long      businessId  = toLong(body.get("businessId"));
        Long      branchId    = body.get("branchId") != null ? toLong(body.get("branchId")) : null;
        Double    weightGrams = toDouble(body.get("weightGrams"));
        String    metalType   = str(body.get("metalType"), "GOLD");
        String    purity      = str(body.get("purity"), null);
        String    notes       = str(body.get("notes"), null);
        String    partyName   = str(body.get("partyName"), "Party");
        LocalDate date        = body.get("date") != null
                ? LocalDate.parse(body.get("date").toString()) : null;
        String    createdBy   = str(body.get("createdBy"), "system");

        Object entry = partyLedgerService.postMetalPaymentEntry(
                partyId, partyName, weightGrams, metalType, purity,
                notes, date, createdBy, businessId, branchId);
        return ResponseEntity.ok(entry);
    }

    // ── POST /api/party-ledger/{partyId}/journal ─────────────────────
    /**
     * Records a General Journal Voucher (GJV) — manual correction / opening balance.
     *
     * Body:
     * { "businessId":1, "branchId":1, "aedDebit":0, "aedCredit":5000, "metalDebit":0,
     *   "metalCredit":0, "metalType":"GOLD", "purity":"995",
     *   "reason":"Opening balance adjustment", "partyName":"Ahmed",
     *   "date":"2025-01-01", "createdBy":"admin" }
     *
     * Constraints (enforced by service):
     *  • reason is mandatory
     *  • at least one value must be non-zero
     *  • cannot have both aedDebit > 0 AND aedCredit > 0
     *  • cannot have both metalDebit > 0 AND metalCredit > 0
     */
    @PostMapping("/{partyId}/journal")
    public ResponseEntity<Object> postJournal(
            @PathVariable Long partyId,
            @RequestBody Map<String, Object> body) {

        Long       businessId  = toLong(body.get("businessId"));
        Long       branchId    = body.get("branchId") != null ? toLong(body.get("branchId")) : null;
        BigDecimal aedDebit    = body.get("aedDebit")   != null ? toBd(body.get("aedDebit"))   : BigDecimal.ZERO;
        BigDecimal aedCredit   = body.get("aedCredit")  != null ? toBd(body.get("aedCredit"))  : BigDecimal.ZERO;
        Double     metalDebit  = body.get("metalDebit")  != null ? toDouble(body.get("metalDebit"))  : 0.0;
        Double     metalCredit = body.get("metalCredit") != null ? toDouble(body.get("metalCredit")) : 0.0;
        String     metalType   = str(body.get("metalType"), null);
        String     purity      = str(body.get("purity"), null);
        String     reason      = str(body.get("reason"), null);
        String     partyName   = str(body.get("partyName"), "Party");
        LocalDate  date        = body.get("date") != null
                ? LocalDate.parse(body.get("date").toString()) : null;
        String     createdBy   = str(body.get("createdBy"), "system");

        Object entry = partyLedgerService.postGeneralJournalEntry(
                partyId, partyName, aedDebit, aedCredit, metalDebit, metalCredit,
                metalType, purity, reason, date, createdBy, businessId, branchId);
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

    private Double toDouble(Object v) {
        if (v == null) throw new IllegalArgumentException("Required numeric field missing");
        if (v instanceof Number) return ((Number) v).doubleValue();
        return Double.parseDouble(v.toString().trim());
    }

    private String str(Object v, String defaultValue) {
        return v != null && !v.toString().isBlank() ? v.toString().trim() : defaultValue;
    }
}
