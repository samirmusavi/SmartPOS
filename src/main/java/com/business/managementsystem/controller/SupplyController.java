package com.business.managementsystem.controller;

import com.business.managementsystem.model.SupplyAdjustment;
import com.business.managementsystem.service.SupplyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/supplies")
public class SupplyController {

    private final SupplyService supplyService;

    public SupplyController(SupplyService supplyService) {
        this.supplyService = supplyService;
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

    // GET /api/supplies
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                supplyService.getAllSupplies(getBusinessId(bh), getBranchId(brh)));
    }

    // GET /api/supplies/summary
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                supplyService.getSummary(getBusinessId(bh), getBranchId(brh)));
    }

    // GET /api/supplies/low-stock
    @GetMapping("/low-stock")
    public ResponseEntity<List<Map<String, Object>>> getLowStock(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                supplyService.getLowStockSupplies(getBusinessId(bh), getBranchId(brh)));
    }

    // GET /api/supplies/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                supplyService.getSupplyById(id, getBusinessId(bh), getBranchId(brh)));
    }

    // GET /api/supplies/{id}/history
    @GetMapping("/{id}/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @PathVariable Long id) {
        return ResponseEntity.ok(supplyService.getAdjustmentHistory(id));
    }

    // POST /api/supplies
    // quantity is no longer accepted from the request — always starts at 0
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestBody Map<String, String> request) {

        String costStr = request.get("costPerUnit");
        String suppStr = request.get("supplierId");
        String minStr  = request.get("minimumThreshold");

        return ResponseEntity.status(HttpStatus.CREATED).body(
                supplyService.createSupply(
                        getBusinessId(bh),
                        request.get("name"),
                        request.get("category"),
                        request.get("unit"),
                        minStr != null ? Integer.parseInt(minStr) : 0,
                        costStr != null && !costStr.isBlank()
                                ? new BigDecimal(costStr) : null,
                        suppStr != null && !suppStr.isBlank()
                                ? Long.parseLong(suppStr) : null,
                        request.get("notes")
                )
        );
    }

    // PUT /api/supplies/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @RequestBody Map<String, String> request) {

        String costStr = request.get("costPerUnit");
        String suppStr = request.get("supplierId");
        String minStr  = request.get("minimumThreshold");

        return ResponseEntity.ok(
                supplyService.updateSupply(
                        id,
                        getBusinessId(bh),
                        request.get("name"),
                        request.get("category"),
                        request.get("unit"),
                        minStr != null ? Integer.parseInt(minStr) : 0,
                        costStr != null && !costStr.isBlank()
                                ? new BigDecimal(costStr) : null,
                        suppStr != null && !suppStr.isBlank()
                                ? Long.parseLong(suppStr) : null,
                        request.get("notes"),
                        getBranchId(brh)
                )
        );
    }

    // DELETE /api/supplies/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String bh) {
        supplyService.deleteSupply(id, getBusinessId(bh));
        return ResponseEntity.noContent().build();
    }

    // POST /api/supplies/{id}/adjust
    @PostMapping("/{id}/adjust")
    public ResponseEntity<Map<String, Object>> adjust(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @RequestBody Map<String, String> request) {

        return ResponseEntity.ok(
                supplyService.adjustStock(
                        id,
                        getBusinessId(bh),
                        getBranchId(brh),
                        Integer.parseInt(request.get("newQuantity")),
                        SupplyAdjustment.Type.valueOf(request.get("type")),
                        request.get("notes"),
                        request.get("adjustedBy")
                )
        );
    }
}