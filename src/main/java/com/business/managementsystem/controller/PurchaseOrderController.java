package com.business.managementsystem.controller;

import com.business.managementsystem.model.PurchaseOrder;
import com.business.managementsystem.service.PurchaseOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService poService;

    public PurchaseOrderController(PurchaseOrderService poService) {
        this.poService = poService;
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

    // GET /api/purchase-orders
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                poService.getAllOrders(getBusinessId(h), parseBranchId(brh)));
    }

    // GET /api/purchase-orders/summary
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                poService.getSummary(getBusinessId(h), parseBranchId(brh)));
    }

    // GET /api/purchase-orders/status/{status}
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Map<String, Object>>> getByStatus(
            @PathVariable String status,
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                poService.getOrdersByStatus(
                        getBusinessId(h),
                        parseBranchId(brh),
                        PurchaseOrder.Status.valueOf(status.toUpperCase())));
    }

    // POST /api/purchase-orders
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @RequestBody Map<String, String> request) {

        String costStr = request.get("unitCost");
        BigDecimal unitCost = (costStr != null && !costStr.isBlank())
                ? new BigDecimal(costStr) : null;

        return ResponseEntity.status(HttpStatus.CREATED).body(
                poService.createOrder(
                        getBusinessId(h),
                        Long.parseLong(request.get("supplierId")),
                        Long.parseLong(request.get("productId")),
                        Double.parseDouble(request.get("quantityOrdered")),
                        unitCost,
                        request.get("expectedDelivery"),
                        request.get("notes"),
                        request.get("createdBy"),
                        parseBranchId(brh)
                )
        );
    }

    // PUT /api/purchase-orders/{id}/receive
    @PutMapping("/{id}/receive")
    public ResponseEntity<Map<String, Object>> receive(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(
                poService.markReceived(
                        id,
                        getBusinessId(h),
                        Double.parseDouble(request.get("quantityReceived")),
                        request.get("notes")
                )
        );
    }

    // PUT /api/purchase-orders/{id}/cancel
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                poService.cancelOrder(id, getBusinessId(h)));
    }

    // PUT /api/purchase-orders/{id}/approve
    @PutMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approve(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                poService.approveDraft(id, getBusinessId(h)));
    }
}