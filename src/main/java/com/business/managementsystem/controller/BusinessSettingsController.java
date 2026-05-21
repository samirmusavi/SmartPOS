package com.business.managementsystem.controller;

import com.business.managementsystem.model.Business;
import com.business.managementsystem.repository.BusinessRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/business/settings")
public class BusinessSettingsController {

    private final BusinessRepository businessRepository;

    public BusinessSettingsController(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    @GetMapping
    public ResponseEntity<Business> getSettings(@RequestHeader("X-Business-Id") Long businessId) {
        return businessRepository.findById(businessId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/receipt")
    public ResponseEntity<Business> updateReceiptSettings(
            @RequestHeader("X-Business-Id") Long businessId,
            @RequestBody Map<String, String> request) {
        
        return businessRepository.findById(businessId).map(business -> {
            if (request.containsKey("logo")) business.setReceiptLogoBase64(request.get("logo"));
            if (request.containsKey("footer")) business.setReceiptFooter(request.get("footer"));
            return ResponseEntity.ok(businessRepository.save(business));
        }).orElse(ResponseEntity.notFound().build());
    }
}
