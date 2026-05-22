package com.business.managementsystem.controller;

import com.business.managementsystem.model.CashDaySheet;
import com.business.managementsystem.model.CashEntry;
import com.business.managementsystem.service.CashSheetService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/cash-sheet")
public class CashSheetController {

    private final CashSheetService cashSheetService;

    public CashSheetController(CashSheetService cashSheetService) {
        this.cashSheetService = cashSheetService;
    }

    // ── Helpers ───────────────────────────────────────────────────
    private Long requireBusinessId(String header) {
        if (header == null || header.isBlank())
            throw new RuntimeException("Business ID header is required.");
        return Long.parseLong(header);
    }

    private Long parseBranchId(String header) {
        if (header == null || header.isBlank()) return null;
        return Long.parseLong(header);
    }

    private BigDecimal bd(Object v) {
        if (v == null) return null;
        return new BigDecimal(v.toString());
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        return ((Number) v).intValue();
    }

    // ── GET /api/cash-sheet?date=2026-05-22&branchId=1 ───────────
    // Returns { daySheet, entries (with runningBalance), summary }
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDaySheet(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestParam String date,
            @RequestParam(required = false) Long branchId) {

        Long businessId  = requireBusinessId(bh);
        LocalDate sheetDate = LocalDate.parse(date);

        // Lazily create the sheet on first access
        cashSheetService.getOrCreateDaySheet(businessId, branchId, sheetDate);
        Map<String, Object> response = cashSheetService.getFullDayResponse(businessId, branchId, sheetDate);
        return ResponseEntity.ok(response);
    }

    // ── POST /api/cash-sheet/sync-pos?date=2026-05-22&branchId=1 ─
    @PostMapping("/sync-pos")
    public ResponseEntity<Map<String, Object>> syncPos(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestParam String date,
            @RequestParam(required = false) Long branchId) {

        Long businessId  = requireBusinessId(bh);
        LocalDate sheetDate = LocalDate.parse(date);

        cashSheetService.syncFromPOS(businessId, branchId, sheetDate);
        Map<String, Object> response = cashSheetService.getFullDayResponse(businessId, branchId, sheetDate);
        return ResponseEntity.ok(response);
    }

    // ── POST /api/cash-sheet/entry ────────────────────────────────
    @PostMapping("/entry")
    public ResponseEntity<Map<String, Object>> addEntry(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @RequestBody Map<String, Object> body) {

        Long businessId = requireBusinessId(bh);
        Long branchId   = parseBranchId(brh);

        // branchId can also come from body for flexibility
        if (branchId == null && body.get("branchId") != null)
            branchId = Long.parseLong(body.get("branchId").toString());

        LocalDate date  = LocalDate.parse(body.get("date").toString());
        String partyName   = body.getOrDefault("partyName",   "").toString();
        String description = body.getOrDefault("description", "").toString();
        BigDecimal amountIn  = bd(body.get("amountIn"));
        BigDecimal amountOut = bd(body.get("amountOut"));
        String createdBy = body.getOrDefault("createdBy", "").toString();

        // Optional explicit type for hand-loan entries
        CashEntry.EntryType explicitType = null;
        if (body.get("entryType") != null && !body.get("entryType").toString().isBlank()) {
            try { explicitType = CashEntry.EntryType.valueOf(body.get("entryType").toString()); }
            catch (IllegalArgumentException ignored) { }
        }

        CashEntry entry = cashSheetService.addManualEntry(
                businessId, branchId, date,
                partyName, description,
                amountIn, amountOut, createdBy, explicitType);

        return ResponseEntity.ok(cashSheetService.toEntryMap(entry));
    }

    // ── PUT /api/cash-sheet/entry/{id} ────────────────────────────
    @PutMapping("/entry/{id}")
    public ResponseEntity<Map<String, Object>> updateEntry(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        CashEntry updated = cashSheetService.updateManualEntry(
                id,
                body.getOrDefault("partyName",   "").toString(),
                body.getOrDefault("description", "").toString(),
                bd(body.get("amountIn")),
                bd(body.get("amountOut")));

        return ResponseEntity.ok(cashSheetService.toEntryMap(updated));
    }

    // ── DELETE /api/cash-sheet/entry/{id} ─────────────────────────
    @DeleteMapping("/entry/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        cashSheetService.deleteManualEntry(id);
        return ResponseEntity.noContent().build();
    }

    // ── PUT /api/cash-sheet/day-sheet/{id} ───────────────────────
    // Updates opening balance and/or denomination counts
    @PutMapping("/day-sheet/{id}")
    public ResponseEntity<Map<String, Object>> updateDaySheet(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        Boolean openingOverridden = body.containsKey("openingOverridden")
                ? Boolean.parseBoolean(body.get("openingOverridden").toString()) : null;

        CashDaySheet updated = cashSheetService.updateDaySheet(
                id,
                bd(body.get("openingBalance")),
                openingOverridden,
                toInt(body.get("bundles1000")),
                toInt(body.get("bundles500")),
                toInt(body.get("bundles200")),
                toInt(body.get("bundles100")),
                toInt(body.get("bundles50")),
                toInt(body.get("bundles10")),
                toInt(body.get("bundles5")),
                bd(body.get("mixNotes")),
                bd(body.get("coins")),
                bd(body.get("handLoanTotal")));

        return ResponseEntity.ok(cashSheetService.toSheetMap(updated));
    }

    // ── GET /api/cash-sheet/export?date=2026-05-22&branchId=1 ────
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestParam String date,
            @RequestParam(required = false) Long branchId) throws java.io.IOException {

        Long businessId  = requireBusinessId(bh);
        LocalDate sheetDate = LocalDate.parse(date);

        byte[] excelBytes = cashSheetService.generateExcel(businessId, branchId, sheetDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment",
                "cash-sheet-" + date + ".xlsx");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }
}
