package com.business.managementsystem.service;

import com.business.managementsystem.model.PurchaseOrder;
import com.business.managementsystem.repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PredictiveInventoryService {

    private final StockoutForecastService stockoutForecastService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderService    purchaseOrderService;

    // We target 30 days of inventory cover
    private static final int TARGET_DAYS_COVER = 30;

    public PredictiveInventoryService(StockoutForecastService stockoutForecastService,
                                      PurchaseOrderRepository purchaseOrderRepository,
                                      PurchaseOrderService purchaseOrderService) {
        this.stockoutForecastService = stockoutForecastService;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderService    = purchaseOrderService;
    }

    @Transactional
    public List<Map<String, Object>> generateDraftOrders(Long businessId, Long branchId, String creatorName) {
        List<Map<String, Object>> generatedDrafts = new ArrayList<>();

        // Get the forecast for this context
        List<Map<String, Object>> forecast = stockoutForecastService.getForecast(businessId, branchId);

        for (Map<String, Object> item : forecast) {
            String status = (String) item.get("status");

            // Only generate drafts for items that are running low or out
            if (List.of("OUT_OF_STOCK", "CRITICAL", "WARNING", "LOW").contains(status)) {
                Long productId = ((Number) item.get("productId")).longValue();
                int currentStock = ((Number) item.get("currentStock")).intValue();
                double dailyVelocity = ((Number) item.get("dailyVelocity")).doubleValue();
                Long supplierId = item.get("supplierId") != null ? ((Number) item.get("supplierId")).longValue() : null;

                // Skip if no supplier defined for this product
                if (supplierId == null) {
                    continue;
                }

                // Check active orders (DRAFT or PENDING) to avoid duplicate ordering
                int alreadyOrdered = branchId != null
                        ? purchaseOrderRepository.getActiveOrderQuantityByBranch(businessId, branchId, productId)
                        : purchaseOrderRepository.getActiveOrderQuantity(businessId, productId);

                // Calculate target stock (e.g. 30 days worth of sales)
                // If velocity is 5/day, target is 150. If we have 20, we need 130.
                int targetStock = (int) Math.ceil(dailyVelocity * TARGET_DAYS_COVER);
                
                // Ensure a reasonable minimum order quantity (e.g. 10) if velocity is very low but it's out of stock
                if (targetStock < 10) targetStock = 10;

                int needed = targetStock - currentStock - alreadyOrdered;

                if (needed > 0) {
                    // Generate DRAFT
                    Map<String, Object> draft = purchaseOrderService.createOrder(
                            businessId,
                            supplierId,
                            productId,
                            needed,
                            null, // unit cost depends on agreement, leave null or fetch default
                            null, // expected delivery TBD
                            "Auto-generated predictive reorder (" + status + ")",
                            creatorName,
                            branchId
                    );
                    
                    // The standard createOrder creates PENDING by default. 
                    // Wait, PurchaseOrderService createOrder sets it to PENDING!
                    // Let's load the PO and switch to DRAFT.
                    Long poId = ((Number) draft.get("id")).longValue();
                    PurchaseOrder po = purchaseOrderRepository.findById(poId).orElseThrow();
                    po.setStatus(PurchaseOrder.Status.DRAFT);
                    purchaseOrderRepository.save(po);
                    
                    draft.put("status", "DRAFT");
                    generatedDrafts.add(draft);
                }
            }
        }

        return generatedDrafts;
    }
}
