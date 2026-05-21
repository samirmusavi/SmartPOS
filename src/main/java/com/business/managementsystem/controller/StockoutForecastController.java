package com.business.managementsystem.controller;

import com.business.managementsystem.service.StockoutForecastService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stockout")
public class StockoutForecastController {

    private final StockoutForecastService forecastService;

    public StockoutForecastController(StockoutForecastService forecastService) {
        this.forecastService = forecastService;
    }

    private Long parseHeader(String header) {
        if (header == null || header.isBlank()) return null;
        try { return Long.parseLong(header); }
        catch (NumberFormatException e) { return null; }
    }

    private Long requireBusinessId(String header) {
        Long id = parseHeader(header);
        if (id == null)
            throw new RuntimeException("Business ID header is required.");
        return id;
    }

    // GET /api/stockout
    // Returns forecast for all active products.
    // Branch-aware when X-Branch-Id header is present:
    //   - Uses branch inventory stock levels
    //   - Uses branch-specific sales velocity
    // Without X-Branch-Id (owner global view):
    //   - Uses product.quantity (total across all branches)
    //   - Uses business-wide sales velocity
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getForecast(
            @RequestHeader(value = "X-Business-Id",
                    required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",
                    required = false) String brh) {
        return ResponseEntity.ok(
                forecastService.getForecast(
                        requireBusinessId(bh),
                        parseHeader(brh)
                )
        );
    }

    // GET /api/stockout/summary — dashboard widget data
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestHeader(value = "X-Business-Id",
                    required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",
                    required = false) String brh) {
        return ResponseEntity.ok(
                forecastService.getForecastSummary(
                        requireBusinessId(bh),
                        parseHeader(brh)
                )
        );
    }
}