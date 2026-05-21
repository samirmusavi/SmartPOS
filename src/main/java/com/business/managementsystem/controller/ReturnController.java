package com.business.managementsystem.controller;

import com.business.managementsystem.model.Return;
import com.business.managementsystem.service.ReturnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    private final ReturnService returnService;

    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    private Long getBusinessId(String header) {
        if (header == null || header.isBlank())
            throw new RuntimeException("Business ID header is required.");
        return Long.parseLong(header);
    }

    private Long parseBranchId(String header) {
        if (header == null || header.isBlank()) return null;
        return Long.parseLong(header);
    }

    // GET /api/returns/list — paginated, date-filterable return list
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getList(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "20") int    size,
            @RequestParam(required = false)    String search,
            @RequestParam(required = false)    String dateFrom,
            @RequestParam(required = false)    String dateTo) {
        return ResponseEntity.ok(
                returnService.getReturnList(getBusinessId(h), parseBranchId(brh),
                        page, size, search, dateFrom, dateTo));
    }

    // GET /api/returns — branch-aware history
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                returnService.getAllReturns(getBusinessId(h), parseBranchId(brh)));
    }

    // GET /api/returns/summary — branch-aware stat cards
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                returnService.getSummary(getBusinessId(h), parseBranchId(brh)));
    }

    // GET /api/returns/sale/{saleId} — returns for a specific sale (no branch filter)
    @GetMapping("/sale/{saleId}")
    public ResponseEntity<List<Map<String, Object>>> getBySale(
            @PathVariable Long saleId) {
        return ResponseEntity.ok(
                returnService.getReturnsBySale(saleId));
    }

    // GET /api/returns/lookup/{receiptNumber}
    // Global — looks up any receipt from any branch so any branch can process a return
    @GetMapping("/lookup/{receiptNumber}")
    public ResponseEntity<Map<String, Object>> lookupReceipt(
            @PathVariable String receiptNumber,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                returnService.lookupReceipt(receiptNumber, getBusinessId(h)));
    }

    // POST /api/returns — process a return; tags the return with current branchId
    @PostMapping
    public ResponseEntity<Map<String, Object>> processReturn(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @RequestBody Map<String, String> request) {

        Map<String, Object> result = returnService.processReturn(
                getBusinessId(h),
                parseBranchId(brh),
                Long.parseLong(request.get("saleId")),
                Double.parseDouble(request.get("quantityReturned")),
                Return.Reason.valueOf(request.get("reason")),
                request.get("notes"),
                request.get("processedBy")
        );
        return ResponseEntity.ok(result);
    }
}