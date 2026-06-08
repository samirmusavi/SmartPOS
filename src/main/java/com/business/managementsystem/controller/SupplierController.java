package com.business.managementsystem.controller;

import com.business.managementsystem.service.SupplierService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    // GET /api/suppliers/stats
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(supplierService.getStats(getBusinessId(h)));
    }

    // GET /api/suppliers — isSupplier = true only (for suppliers.html)
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                supplierService.getAllSuppliers(getBusinessId(h)));
    }

    // GET /api/suppliers/all — every party regardless of flags (for parties.html)
    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllParties(
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                supplierService.getAllParties(getBusinessId(h)));
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
            @RequestBody Map<String, Object> request) {
        boolean isSupplier = getBool(request, "isSupplier", true);
        boolean isCustomer = getBool(request, "isCustomer", false);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                supplierService.createSupplier(
                        getBusinessId(h),
                        getString(request, "name"),
                        getString(request, "contactPerson"),
                        getString(request, "phone"),
                        getString(request, "email"),
                        getString(request, "address"),
                        getString(request, "notes"),
                        isSupplier, isCustomer,
                        getString(request, "kycStatus"),
                        getString(request, "emiratesId"),
                        getString(request, "passportNumber"),
                        getString(request, "tradeLicenseNumber"),
                        getString(request, "idExpiryDate"),
                        getString(request, "kycNotes")
                )
        );
    }

    // PUT /api/suppliers/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestBody Map<String, Object> request) {
        boolean isSupplier = getBool(request, "isSupplier", true);
        boolean isCustomer = getBool(request, "isCustomer", false);
        return ResponseEntity.ok(
                supplierService.updateSupplier(
                        id, getBusinessId(h),
                        getString(request, "name"),
                        getString(request, "contactPerson"),
                        getString(request, "phone"),
                        getString(request, "email"),
                        getString(request, "address"),
                        getString(request, "notes"),
                        isSupplier, isCustomer,
                        getString(request, "kycStatus"),
                        getString(request, "emiratesId"),
                        getString(request, "passportNumber"),
                        getString(request, "tradeLicenseNumber"),
                        getString(request, "idExpiryDate"),
                        getString(request, "kycNotes")
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

    // ── Party Code endpoints ──────────────────────────────────────────

    /**
     * POST /api/suppliers/generate-party-codes
     * Batch-assigns party codes to all parties that don't have one yet.
     * Idempotent — safe to call on an existing database.
     */
    @PostMapping("/generate-party-codes")
    public ResponseEntity<Map<String, Object>> generatePartyCodes(
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        int count = supplierService.generatePartyCodes(getBusinessId(h));
        return ResponseEntity.ok(Map.of("generated", count));
    }

    // ── KYC Document endpoints ─────────────────────────────────────

    // GET /api/suppliers/{id}/kyc-documents
    @GetMapping("/{id}/kyc-documents")
    public ResponseEntity<List<Map<String, Object>>> getKycDocuments(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                supplierService.getKycDocuments(id, getBusinessId(h)));
    }

    // POST /api/suppliers/{id}/kyc-documents  (multipart)
    @PostMapping("/{id}/kyc-documents")
    public ResponseEntity<Map<String, Object>> uploadKycDocument(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentType", defaultValue = "OTHER") String documentType,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    supplierService.uploadKycDocument(id, getBusinessId(h),
                            file, documentType, uploadedBy));
        } catch (IOException e) {
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }

    // DELETE /api/suppliers/{id}/kyc-documents/{docId}
    @DeleteMapping("/{id}/kyc-documents/{docId}")
    public ResponseEntity<Void> deleteKycDocument(
            @PathVariable Long id,
            @PathVariable Long docId,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        supplierService.deleteKycDocument(id, docId, getBusinessId(h));
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ────────────────────────────────────────────────────────
    private String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString().trim() : null;
    }

    private boolean getBool(Map<String, Object> map, String key, boolean defaultVal) {
        Object v = map.get(key);
        if (v == null) return defaultVal;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(v.toString());
    }
}
