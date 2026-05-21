package com.business.managementsystem.controller;

import com.business.managementsystem.service.ProductSupplyTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/supply-templates")
public class ProductSupplyTemplateController {

    private final ProductSupplyTemplateService templateService;

    public ProductSupplyTemplateController(
            ProductSupplyTemplateService templateService) {
        this.templateService = templateService;
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

    // GET /api/supply-templates/{productId}
    @GetMapping("/{productId}")
    public ResponseEntity<List<Map<String, Object>>> getTemplate(
            @PathVariable Long productId,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                templateService.getTemplate(productId, getBusinessId(h)));
    }

    // POST /api/supply-templates/{productId}
    // Saves (replaces) the supply template for a product
    // Body: [{supplyId, quantityPerUnit}, ...]
    @PostMapping("/{productId}")
    public ResponseEntity<List<Map<String, Object>>> saveTemplate(
            @PathVariable Long productId,
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestBody List<Map<String, Object>> entries) {
        return ResponseEntity.ok(
                templateService.saveTemplate(productId, getBusinessId(h), entries));
    }

    // POST /api/supply-templates/preview
    // Returns what supplies will be deducted for a set of sale items.
    // Uses branch supply quantity when X-Branch-Id is present.
    // Body: [{productId, quantitySold}, ...]
    @PostMapping("/preview")
    public ResponseEntity<List<Map<String, Object>>> preview(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @RequestBody List<Map<String, Object>> saleItems) {
        return ResponseEntity.ok(
                templateService.buildDeductionPreview(
                        getBusinessId(bh), getBranchId(brh), saleItems));
    }

    // POST /api/supply-templates/apply
    // Applies supply deductions after cashier confirms.
    // Deducts from branch_supply_inventory when X-Branch-Id is present.
    // Body: { performedBy: "...", deductions: [{supplyId, toDeduct}, ...] }
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> apply(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @RequestBody Map<String, Object> request) {

        String performedBy = request.get("performedBy").toString();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> raw =
                (List<Map<String, Object>>) request.get("deductions");

        // Re-map entries so Jackson Integer/Long deserialization doesn't matter
        List<Map<String, Object>> deductions = raw.stream().map(d -> {
            Map<String, Object> safe = new HashMap<>();
            safe.put("supplyId", d.get("supplyId"));
            safe.put("toDeduct", d.get("toDeduct"));
            return safe;
        }).toList();

        templateService.applyDeductions(
                getBusinessId(bh), getBranchId(brh), deductions, performedBy);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("applied", deductions.size());
        return ResponseEntity.ok(result);
    }
}
