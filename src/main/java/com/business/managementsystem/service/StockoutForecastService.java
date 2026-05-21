package com.business.managementsystem.service;

import com.business.managementsystem.model.BranchInventory;
import com.business.managementsystem.model.Product;
import com.business.managementsystem.repository.BranchInventoryRepository;
import com.business.managementsystem.repository.ProductRepository;
import com.business.managementsystem.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class StockoutForecastService {

    private final SaleRepository            saleRepository;
    private final ProductRepository         productRepository;
    private final BranchInventoryRepository branchInventoryRepository;

    private static final int VELOCITY_DAYS = 30;

    public StockoutForecastService(SaleRepository saleRepository,
                                   ProductRepository productRepository,
                                   BranchInventoryRepository branchInventoryRepository) {
        this.saleRepository            = saleRepository;
        this.productRepository         = productRepository;
        this.branchInventoryRepository = branchInventoryRepository;
    }

    // ── Main forecast — branch-aware ─────────────────────────────
    // If branchId is provided: uses branch inventory quantities and
    // branch-specific sales velocity (sales at that branch only).
    // If no branchId (owner global view): uses product.quantity and
    // business-wide sales velocity across all branches.
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getForecast(Long businessId,
                                                 Long branchId) {
        LocalDateTime since = LocalDateTime.now()
                .minusDays(VELOCITY_DAYS);

        // Get sales velocity data — branch or business level
        List<Object[]> velocityData = branchId != null
                ? saleRepository.getProductVelocityByBranch(
                businessId, branchId, since)
                : saleRepository.getProductVelocityByBusiness(
                businessId, since);

        // Build productId → units sold map
        Map<Long, Long> soldMap = new HashMap<>();
        for (Object[] row : velocityData) {
            Long productId = ((Number) row[0]).longValue();
            Long unitsSold = ((Number) row[4]).longValue();
            soldMap.put(productId, unitsSold);
        }

        // Get all active products
        List<Product> allProducts = productRepository
                .findByBusinessIdAndActiveTrue(businessId);

        // If branch-aware: build a productId → branch quantity map
        Map<Long, Double> branchStockMap = new HashMap<>();
        if (branchId != null) {
            List<BranchInventory> inventories = branchInventoryRepository
                    .findByBranchId(branchId);
            for (BranchInventory inv : inventories) {
                branchStockMap.put(inv.getProductId(), inv.getQuantity());
            }
        }

        List<Map<String, Object>> results = new ArrayList<>();

        for (Product p : allProducts) {
            long unitsSold = soldMap.getOrDefault(p.getId(), 0L);

            // Use branch stock if branch-aware, otherwise global
            double stock = branchId != null
                    ? branchStockMap.getOrDefault(p.getId(), 0.0)
                    : p.getQuantity();

            double  dailyVelocity    = (double) unitsSold / VELOCITY_DAYS;
            Integer daysUntilStockout;
            String  status;

            if (stock == 0) {
                daysUntilStockout = 0;
                status = "OUT_OF_STOCK";
            } else if (dailyVelocity == 0) {
                daysUntilStockout = null;
                status = "NO_DATA";
            } else {
                daysUntilStockout = (int) Math.floor(stock / dailyVelocity);
                if      (daysUntilStockout <= 3)  status = "CRITICAL";
                else if (daysUntilStockout <= 7)  status = "WARNING";
                else if (daysUntilStockout <= 14) status = "LOW";
                else                              status = "SAFE";
            }

            Map<String, Object> entry = new HashMap<>();
            entry.put("productId",         p.getId());
            entry.put("productName",       p.getName());
            entry.put("category",          p.getCategory());
            entry.put("supplierId",        p.getSupplierId());
            entry.put("currentStock",      stock);
            entry.put("unitsSold30Days",   unitsSold);
            entry.put("dailyVelocity",
                    Math.round(dailyVelocity * 10.0) / 10.0);
            entry.put("daysUntilStockout", daysUntilStockout);
            entry.put("status",            status);
            entry.put("velocityDays",      VELOCITY_DAYS);
            results.add(entry);
        }

        // Sort: OUT_OF_STOCK → CRITICAL → WARNING → LOW → SAFE → NO_DATA
        results.sort((a, b) -> {
            int pa = statusPriority((String) a.get("status"));
            int pb = statusPriority((String) b.get("status"));
            if (pa != pb) return pa - pb;

            Integer da = (Integer) a.get("daysUntilStockout");
            Integer db = (Integer) b.get("daysUntilStockout");
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return da - db;
        });

        return results;
    }

    // Legacy overload — no branch, business-wide view
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getForecast(Long businessId) {
        return getForecast(businessId, null);
    }

    // ── Summary for dashboard widget ─────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getForecastSummary(Long businessId,
                                                  Long branchId) {
        List<Map<String, Object>> all = getForecast(businessId, branchId);

        long critical   = all.stream()
                .filter(m -> "CRITICAL".equals(m.get("status"))).count();
        long warning    = all.stream()
                .filter(m -> "WARNING".equals(m.get("status"))).count();
        long outOfStock = all.stream()
                .filter(m -> "OUT_OF_STOCK".equals(m.get("status"))).count();
        long low        = all.stream()
                .filter(m -> "LOW".equals(m.get("status"))).count();

        List<Map<String, Object>> atRisk = all.stream()
                .filter(m -> !List.of("NO_DATA", "SAFE")
                        .contains(m.get("status")))
                .limit(5)
                .toList();

        Map<String, Object> summary = new HashMap<>();
        summary.put("critical",    critical);
        summary.put("warning",     warning);
        summary.put("outOfStock",  outOfStock);
        summary.put("low",         low);
        summary.put("totalAtRisk", critical + warning + outOfStock + low);
        summary.put("topAtRisk",   atRisk);
        return summary;
    }

    // Legacy overload — no branch
    @Transactional(readOnly = true)
    public Map<String, Object> getForecastSummary(Long businessId) {
        return getForecastSummary(businessId, null);
    }

    private int statusPriority(String status) {
        return switch (status) {
            case "OUT_OF_STOCK" -> 0;
            case "CRITICAL"     -> 1;
            case "WARNING"      -> 2;
            case "LOW"          -> 3;
            case "SAFE"         -> 4;
            default             -> 5; // NO_DATA
        };
    }
}