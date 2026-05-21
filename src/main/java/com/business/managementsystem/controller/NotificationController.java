package com.business.managementsystem.controller;

import com.business.managementsystem.model.PurchaseOrder;
import com.business.managementsystem.repository.ProductRepository;
import com.business.managementsystem.repository.PurchaseOrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * GET /api/notifications
 *
 * Returns a live-computed list of notifications for the current business.
 * No DB table — everything is derived from existing data so it's always fresh.
 *
 * Notification types (in priority order):
 *   1. OUT_OF_STOCK  — product quantity == 0
 *   2. LOW_STOCK     — product quantity > 0 but <= 5
 *   3. DRAFT_PO      — purchase orders in DRAFT status (awaiting confirmation)
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final ProductRepository       productRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public NotificationController(ProductRepository productRepository,
                                  PurchaseOrderRepository purchaseOrderRepository) {
        this.productRepository       = productRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    private Long requireBusinessId(String header) {
        if (header == null || header.isBlank())
            throw new RuntimeException("Business ID header is required.");
        try { return Long.parseLong(header); }
        catch (NumberFormatException e) {
            throw new RuntimeException("Invalid Business ID header.");
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(
            @RequestHeader(value = "X-Business-Id", required = false) String bh) {

        Long businessId = requireBusinessId(bh);
        List<Map<String, Object>> items = new ArrayList<>();

        // ── 1. Out-of-stock products ──────────────────────────────
        productRepository.findByBusinessIdAndActiveTrue(businessId).stream()
                .filter(p -> p.getQuantity() == 0)
                .forEach(p -> {
                    Map<String, Object> n = new LinkedHashMap<>();
                    n.put("id",       "stock-out-" + p.getId());
                    n.put("type",     "OUT_OF_STOCK");
                    n.put("severity", "critical");
                    n.put("icon",     "fa-box-open");
                    n.put("title",    p.getName() + " is out of stock");
                    n.put("message",  "0 units remaining — reorder immediately.");
                    n.put("link",     "/stockout.html");
                    items.add(n);
                });

        // ── 2. Low-stock products (1–5 units) ────────────────────
        productRepository.findByBusinessIdAndActiveTrue(businessId).stream()
                .filter(p -> p.getQuantity() > 0 && p.getQuantity() <= 5)
                .forEach(p -> {
                    Map<String, Object> n = new LinkedHashMap<>();
                    n.put("id",       "stock-low-" + p.getId());
                    n.put("type",     "LOW_STOCK");
                    n.put("severity", "warning");
                    n.put("icon",     "fa-triangle-exclamation");
                    n.put("title",    p.getName() + " is running low");
                    n.put("message",  p.getQuantity() + " unit" +
                                      (p.getQuantity() == 1 ? "" : "s") + " left.");
                    n.put("link",     "/stockout.html");
                    items.add(n);
                });

        // ── 3. Draft purchase orders awaiting action ──────────────
        long draftCount = purchaseOrderRepository
                .countByBusinessIdAndStatus(businessId, PurchaseOrder.Status.DRAFT);
        if (draftCount > 0) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id",       "draft-po");
            n.put("type",     "DRAFT_PO");
            n.put("severity", "info");
            n.put("icon",     "fa-file-invoice");
            n.put("title",    draftCount + " draft purchase order" +
                              (draftCount == 1 ? "" : "s") + " pending");
            n.put("message",  "Review and confirm to send to suppliers.");
            n.put("link",     "/purchase-orders.html");
            items.add(n);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total",    items.size());
        result.put("critical", items.stream()
                .filter(i -> "critical".equals(i.get("severity"))).count());
        result.put("items",    items);

        return ResponseEntity.ok(result);
    }
}
