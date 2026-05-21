package com.business.managementsystem.controller;

import com.business.managementsystem.service.SupplierService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    private Long getBusinessId(String header) {
        if (header == null || header.isBlank())
            throw new RuntimeException("Business ID header is required.");
        return Long.parseLong(header);
    }

    // GET /api/suppliers
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                supplierService.getAllSuppliers(getBusinessId(h)));
    }

    // GET /api/suppliers/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                supplierService.getSupplierById(id, getBusinessId(h)));
    }

    // GET /api/suppliers/{id}/products
    @GetMapping("/{id}/products")
    public ResponseEntity<List<Map<String, Object>>> getProducts(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                supplierService.getProductsForSupplier(id, getBusinessId(h)));
    }

    // POST /api/suppliers
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                supplierService.createSupplier(
                        getBusinessId(h),
                        request.get("name"),
                        request.get("contactPerson"),
                        request.get("phone"),
                        request.get("email"),
                        request.get("address"),
                        request.get("notes")
                )
        );
    }

    // PUT /api/suppliers/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(
                supplierService.updateSupplier(
                        id, getBusinessId(h),
                        request.get("name"),
                        request.get("contactPerson"),
                        request.get("phone"),
                        request.get("email"),
                        request.get("address"),
                        request.get("notes")
                )
        );
    }

    // DELETE /api/suppliers/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        supplierService.deleteSupplier(id, getBusinessId(h));
        return ResponseEntity.noContent().build();
    }

    // PUT /api/suppliers/link — link product to supplier
    @PutMapping("/link")
    public ResponseEntity<Void> linkProduct(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestBody Map<String, Long> request) {
        supplierService.linkProduct(
                request.get("supplierId"),
                request.get("productId"),
                getBusinessId(h)
        );
        return ResponseEntity.ok().build();
    }

    // PUT /api/suppliers/unlink/{productId}
    @PutMapping("/unlink/{productId}")
    public ResponseEntity<Void> unlinkProduct(
            @PathVariable Long productId,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        supplierService.unlinkProduct(productId, getBusinessId(h));
        return ResponseEntity.ok().build();
    }

    // PUT /api/suppliers/{id}/link-customer/{customerId}
    @PutMapping("/{id}/link-customer/{customerId}")
    public ResponseEntity<Map<String, Object>> linkCustomer(
            @PathVariable Long id,
            @PathVariable Long customerId,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                supplierService.linkCustomer(id, customerId, getBusinessId(h)));
    }

    // DELETE /api/suppliers/{id}/link-customer
    @DeleteMapping("/{id}/link-customer")
    public ResponseEntity<Map<String, Object>> unlinkCustomer(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                supplierService.unlinkCustomer(id, getBusinessId(h)));
    }
}