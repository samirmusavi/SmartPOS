package com.business.managementsystem.controller;

import com.business.managementsystem.service.PredictiveInventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/predictions")
public class PredictiveInventoryController {

    private final PredictiveInventoryService predictiveInventoryService;

    public PredictiveInventoryController(PredictiveInventoryService predictiveInventoryService) {
        this.predictiveInventoryService = predictiveInventoryService;
    }

    private Long getBusinessId(String header) {
        if (header == null || header.isBlank())
            throw new RuntimeException("Business ID header is required.");
        return Long.parseLong(header);
    }

    private Long parseBranchId(String header) {
        if (header == null || header.isBlank()) return null;
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // POST /api/predictions/generate-drafts
    @PostMapping("/generate-drafts")
    public ResponseEntity<List<Map<String, Object>>> generateDrafts(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @RequestBody Map<String, String> request) {
        
        String creator = request.getOrDefault("creatorName", "System");
        return ResponseEntity.ok(
                predictiveInventoryService.generateDraftOrders(
                        getBusinessId(bh), parseBranchId(brh), creator
                )
        );
    }
}
