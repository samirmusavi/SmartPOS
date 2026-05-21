package com.business.managementsystem.controller;

import com.business.managementsystem.model.StockAdjustment;
import com.business.managementsystem.service.StockAdjustmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock-adjustments")
public class StockAdjustmentController {

    private final StockAdjustmentService adjustmentService;

    public StockAdjustmentController(StockAdjustmentService adjustmentService) {
        this.adjustmentService = adjustmentService;
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

    // GET /api/stock-adjustments
    // branchId present → this branch's adjustments only
    // branchId absent  → all adjustments for business (owner global view)
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                adjustmentService.getAllAdjustments(
                        getBusinessId(bh), getBranchId(brh)));
    }

    // GET /api/stock-adjustments/summary
    // branchId present → branch-specific stats
    // branchId absent  → business-wide stats
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                adjustmentService.getSummary(
                        getBusinessId(bh), getBranchId(brh)));
    }

    // GET /api/stock-adjustments/product/{productId}
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Map<String, Object>>> getByProduct(
            @PathVariable Long productId,
            @RequestHeader(value = "X-Business-Id", required = false) String bh) {
        return ResponseEntity.ok(
                adjustmentService.getProductAdjustments(
                        getBusinessId(bh), productId));
    }

    // POST /api/stock-adjustments — create adjustment
    @PostMapping
    public ResponseEntity<Map<String, Object>> adjust(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @RequestBody Map<String, String> request) {

        Map<String, Object> result = adjustmentService.adjustStock(
                getBusinessId(bh),
                Long.parseLong(request.get("productId")),
                Double.parseDouble(request.get("newQuantity")),
                StockAdjustment.Reason.valueOf(request.get("reason")),
                request.get("notes"),
                request.get("adjustedBy"),
                getBranchId(brh)
        );
        return ResponseEntity.ok(result);
    }
}