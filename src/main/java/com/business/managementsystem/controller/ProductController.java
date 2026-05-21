package com.business.managementsystem.controller;

import com.business.managementsystem.dto.ProductDTO;
import com.business.managementsystem.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
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

    // GET /api/products
    // quantity = branch stock when X-Branch-Id present; global total when absent
    // branch-archived products are excluded when X-Branch-Id present
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAll(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                productService.getAllProducts(getBusinessId(bh), getBranchId(brh)));
    }

    // GET /api/products/archived
    // branchId present → branch-archived products for this branch
    // branchId absent  → globally archived products (product.active = false)
    @GetMapping("/archived")
    public ResponseEntity<List<ProductDTO>> getArchived(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                productService.getArchivedProducts(
                        getBusinessId(bh), getBranchId(brh)));
    }

    // GET /api/products/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Branch-Id", required = false) String brh) {
        return ResponseEntity.ok(
                productService.getProductById(id, getBranchId(brh)));
    }

    // GET /api/products/barcode/{barcode}
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ProductDTO> getByBarcode(
            @PathVariable String barcode,
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                productService.getProductByBarcode(
                        barcode, getBusinessId(bh), getBranchId(brh)));
    }

    // POST /api/products
    @PostMapping
    public ResponseEntity<ProductDTO> create(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @Valid @RequestBody ProductDTO productDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                productService.createProduct(
                        productDTO, getBusinessId(bh), getBranchId(brh)));
    }

    // PUT /api/products/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductDTO productDTO) {
        return ResponseEntity.ok(
                productService.updateProduct(id, productDTO));
    }

    // DELETE /api/products/{id}
    // branchId present → {"action":"BRANCH_ARCHIVED"}
    // branchId absent  → {"action":"ARCHIVED"} or {"action":"DELETED"}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Branch-Id", required = false) String brh) {
        return ResponseEntity.ok(
                productService.deleteProduct(id, getBranchId(brh)));
    }

    // PUT /api/products/{id}/restore
    // branchId present → removes from branch archive
    // branchId absent  → sets product.active = true
    @PutMapping("/{id}/restore")
    public ResponseEntity<ProductDTO> restore(
            @PathVariable Long id,
            @RequestHeader(value = "X-Branch-Id", required = false) String brh) {
        return ResponseEntity.ok(
                productService.restoreProduct(id, getBranchId(brh)));
    }

    // POST /api/products/{id}/image
    // Uploads a product image (multipart/form-data)
    // Returns {"imagePath": "/uploads/products/{businessId}/{filename}"}
    @PostMapping("/{id}/image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestParam("file") MultipartFile file) {
        try {
            String imagePath = productService.saveProductImage(
                    id, getBusinessId(bh), file);
            return ResponseEntity.ok(Map.of("imagePath", imagePath));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save image file."));
        }
    }

    // DELETE /api/products/{id}/image
    // Removes the product image from filesystem and clears the DB field
    @DeleteMapping("/{id}/image")
    public ResponseEntity<Map<String, String>> deleteImage(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String bh) {
        productService.deleteProductImage(id, getBusinessId(bh));
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
    // ============================================================
// ADD THIS METHOD TO ProductController.java
// (anywhere inside the class, e.g. after the deleteImage method)
// ============================================================

    // GET /api/products/total-stock
    // Returns a map of productId → total stock summed across ALL branches.
    // Used by the owner global view to show accurate cross-branch totals.
    @GetMapping("/total-stock")
    public ResponseEntity<Map<Long, Double>> getTotalStock(
            @RequestHeader(value = "X-Business-Id", required = false) String bh) {
        return ResponseEntity.ok(
                productService.getTotalStockAllBranches(getBusinessId(bh)));
    }

    // POST /api/products/import
    // Imports products from a CSV file (multipart/form-data)
    // CSV columns: Name, Barcode, Category, CostPrice, SellingPrice, Quantity
    // Returns: {"imported": N, "skipped": N, "total": N, "errors": [...]}
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importCsv(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = productService.importProductsFromCsv(
                    getBusinessId(bh), getBranchId(brh), file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null
                            ? e.getMessage() : "Import failed"));
        }
    }
}
