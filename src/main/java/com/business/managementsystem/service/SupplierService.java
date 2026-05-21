package com.business.managementsystem.service;

import com.business.managementsystem.model.Product;
import com.business.managementsystem.model.Supplier;
import com.business.managementsystem.repository.ProductRepository;
import com.business.managementsystem.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductRepository  productRepository;

    public SupplierService(SupplierRepository supplierRepository,
                           ProductRepository productRepository) {
        this.supplierRepository = supplierRepository;
        this.productRepository  = productRepository;
    }

    // ── Get all suppliers ────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllSuppliers(Long businessId) {
        return supplierRepository
                .findByBusinessIdOrderByNameAsc(businessId)
                .stream()
                .map(s -> toMap(s, countProducts(s.getId(), businessId)))
                .toList();
    }

    // ── Get single supplier ──────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getSupplierById(Long id, Long businessId) {
        Supplier s = supplierRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Supplier not found."));
        return toMap(s, countProducts(id, businessId));
    }

    // ── Create supplier ──────────────────────────────────────────
    @Transactional
    public Map<String, Object> createSupplier(Long businessId, String name,
                                              String contactPerson, String phone,
                                              String email, String address,
                                              String notes) {
        if (name == null || name.isBlank())
            throw new RuntimeException("Supplier name is required.");

        Supplier supplier = new Supplier(
                businessId, name, contactPerson,
                phone, email, address, notes);
        return toMap(supplierRepository.save(supplier), 0);
    }

    // ── Update supplier ──────────────────────────────────────────
    @Transactional
    public Map<String, Object> updateSupplier(Long id, Long businessId,
                                              String name, String contactPerson,
                                              String phone, String email,
                                              String address, String notes) {
        Supplier supplier = supplierRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Supplier not found."));

        if (name == null || name.isBlank())
            throw new RuntimeException("Supplier name is required.");

        supplier.setName(name);
        supplier.setContactPerson(contactPerson);
        supplier.setPhone(phone);
        supplier.setEmail(email);
        supplier.setAddress(address);
        supplier.setNotes(notes);
        return toMap(supplierRepository.save(supplier),
                countProducts(id, businessId));
    }

    // ── Delete supplier ──────────────────────────────────────────
    @Transactional
    public void deleteSupplier(Long id, Long businessId) {
        Supplier supplier = supplierRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Supplier not found."));

        // Unlink all products from this supplier first
        List<Product> linked = productRepository
                .findByBusinessIdAndActiveTrue(businessId)
                .stream()
                .filter(p -> id.equals(p.getSupplierId()))
                .toList();

        linked.forEach(p -> {
            p.setSupplierId(null);
            productRepository.save(p);
        });

        supplierRepository.delete(supplier);
    }

    // ── Link a product to a supplier ─────────────────────────────
    @Transactional
    public void linkProduct(Long supplierId, Long productId, Long businessId) {
        supplierRepository.findByIdAndBusinessId(supplierId, businessId)
                .orElseThrow(() -> new RuntimeException("Supplier not found."));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found."));

        if (!product.getBusinessId().equals(businessId))
            throw new RuntimeException("Unauthorized.");

        product.setSupplierId(supplierId);
        productRepository.save(product);
    }

    // ── Unlink a product from its supplier ───────────────────────
    @Transactional
    public void unlinkProduct(Long productId, Long businessId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found."));

        if (!product.getBusinessId().equals(businessId))
            throw new RuntimeException("Unauthorized.");

        product.setSupplierId(null);
        productRepository.save(product);
    }

    // ── Get products for a supplier ──────────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProductsForSupplier(
            Long supplierId, Long businessId) {

        return productRepository
                .findByBusinessIdAndActiveTrue(businessId)
                .stream()
                .filter(p -> supplierId.equals(p.getSupplierId()))
                .map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id",       p.getId());
                    m.put("name",     p.getName());
                    m.put("barcode",  p.getBarcode());
                    m.put("category", p.getCategory());
                    m.put("quantity", p.getQuantity());
                    m.put("price",    p.getPrice());
                    return m;
                })
                .toList();
    }

    // ── Helper: count products linked to a supplier ──────────────
    private int countProducts(Long supplierId, Long businessId) {
        return (int) productRepository
                .findByBusinessIdAndActiveTrue(businessId)
                .stream()
                .filter(p -> supplierId.equals(p.getSupplierId()))
                .count();
    }

    // ── Link supplier ↔ customer ─────────────────────────────────
    @Transactional
    public Map<String, Object> linkCustomer(Long supplierId, Long customerId, Long businessId) {
        Supplier s = supplierRepository.findByIdAndBusinessId(supplierId, businessId)
                .orElseThrow(() -> new RuntimeException("Supplier not found."));
        s.setLinkedCustomerId(customerId);
        return toMap(supplierRepository.save(s), countProducts(supplierId, businessId));
    }

    @Transactional
    public Map<String, Object> unlinkCustomer(Long supplierId, Long businessId) {
        Supplier s = supplierRepository.findByIdAndBusinessId(supplierId, businessId)
                .orElseThrow(() -> new RuntimeException("Supplier not found."));
        s.setLinkedCustomerId(null);
        return toMap(supplierRepository.save(s), countProducts(supplierId, businessId));
    }

    // ── Convert to safe map ──────────────────────────────────────
    private Map<String, Object> toMap(Supplier s, int productCount) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",               s.getId());
        m.put("name",             s.getName());
        m.put("contactPerson",    s.getContactPerson());
        m.put("phone",            s.getPhone());
        m.put("email",            s.getEmail());
        m.put("address",          s.getAddress());
        m.put("notes",            s.getNotes());
        m.put("linkedCustomerId", s.getLinkedCustomerId());
        m.put("productCount",     productCount);
        m.put("createdAt",        s.getCreatedAt() != null
                ? s.getCreatedAt().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : null);
        return m;
    }
}