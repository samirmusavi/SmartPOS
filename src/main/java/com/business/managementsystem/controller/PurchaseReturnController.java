package com.business.managementsystem.controller;

import com.business.managementsystem.service.PurchaseReturnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase-returns")
public class PurchaseReturnController {

    private final PurchaseReturnService service;

    public PurchaseReturnController(PurchaseReturnService service) {
        this.service = service;
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

    // GET /api/purchase-returns/lookup/{invoiceNumber}
    @GetMapping("/lookup/{invoiceNumber}")
    public ResponseEntity<Map<String, Object>> lookupInvoice(
            @PathVariable String invoiceNumber,
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                service.lookupInvoice(getBusinessId(h), parseBranchId(brh), invoiceNumber));
    }

    // POST /api/purchase-returns — process a purchase return
    @PostMapping
    public ResponseEntity<Map<String, Object>> processReturn(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(
                service.processReturn(getBusinessId(h), parseBranchId(brh), body));
    }

    // GET /api/purchase-returns — all returns for history table
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllReturns(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                service.getAllReturns(getBusinessId(h), parseBranchId(brh)));
    }

    // GET /api/purchase-returns/summary — stat cards
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                service.getSummary(getBusinessId(h), parseBranchId(brh)));
    }
}
