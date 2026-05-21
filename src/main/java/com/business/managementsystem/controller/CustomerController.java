package com.business.managementsystem.controller;

import com.business.managementsystem.model.Customer;
import com.business.managementsystem.service.CustomerService;
import com.business.managementsystem.service.LoyaltyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final LoyaltyService loyaltyService;
    private final com.business.managementsystem.repository.SaleTransactionRepository saleTransactionRepository;

    public CustomerController(CustomerService customerService,
                              LoyaltyService loyaltyService,
                              com.business.managementsystem.repository.SaleTransactionRepository saleTransactionRepository) {
        this.customerService = customerService;
        this.loyaltyService = loyaltyService;
        this.saleTransactionRepository = saleTransactionRepository;
    }

    private Long getBusinessId(String header) {
        if (header == null || header.isBlank())
            throw new RuntimeException("Business ID header is required.");
        return Long.parseLong(header);
    }

    private Map<String, Object> toMap(Customer c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",           c.getId());
        m.put("fullName",     c.getFullName());
        m.put("phone",        c.getPhone());
        m.put("email",        c.getEmail());
        m.put("notes",        c.getNotes());
        m.put("loyaltyPoints",c.getLoyaltyPoints());
        m.put("totalSpent",   c.getTotalSpent());
        m.put("visitCount",   c.getVisitCount());
        m.put("lastVisitAt",  c.getLastVisitAt() != null
                ? c.getLastVisitAt().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : null);
        m.put("linkedSupplierId", c.getLinkedSupplierId());
        m.put("createdAt",    c.getCreatedAt() != null
                ? c.getCreatedAt().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : null);
        return m;
    }

    // GET /api/customers
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                customerService.getAllCustomers(getBusinessId(h))
                        .stream().map(this::toMap).toList()
        );
    }

    // GET /api/customers/summary
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                customerService.getSummary(getBusinessId(h))
        );
    }

    // GET /api/customers/search?q=
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestParam String q) {
        return ResponseEntity.ok(
                customerService.searchCustomers(getBusinessId(h), q)
                        .stream().map(this::toMap).toList()
        );
    }

    // GET /api/customers/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(
                toMap(customerService.getCustomerById(id, getBusinessId(h)))
        );
    }

    // POST /api/customers
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestBody Map<String, String> request) {
        Customer customer = customerService.createCustomer(
                getBusinessId(h),
                request.get("fullName"),
                request.get("phone"),
                request.get("email"),
                request.get("notes")
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(customer));
    }

    // PUT /api/customers/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestBody Map<String, String> request) {
        Customer customer = customerService.updateCustomer(
                id,
                getBusinessId(h),
                request.get("fullName"),
                request.get("phone"),
                request.get("email"),
                request.get("notes")
        );
        return ResponseEntity.ok(toMap(customer));
    }

    // POST /api/customers/{id}/purchase — record a purchase
    @PostMapping("/{id}/purchase")
    public ResponseEntity<Map<String, Object>> recordPurchase(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestBody Map<String, String> request) {
        Customer customer = customerService.recordPurchase(
                id,
                getBusinessId(h),
                new BigDecimal(request.get("amount"))
        );
        return ResponseEntity.ok(toMap(customer));
    }

    // DELETE /api/customers/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        customerService.deleteCustomer(id, getBusinessId(h));
        return ResponseEntity.noContent().build();
    }

    // GET /api/customers/{id}/loyalty
    @GetMapping("/{id}/loyalty")
    public ResponseEntity<Map<String, Object>> getLoyaltyProfile(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        Customer c = customerService.getCustomerById(id, getBusinessId(h));
        return ResponseEntity.ok(loyaltyService.getCustomerLoyaltyProfile(c));
    }

    // GET /api/customers/{id}/history
    @GetMapping("/{id}/history")
    public ResponseEntity<List<com.business.managementsystem.model.SaleTransaction>> getHistory(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        customerService.getCustomerById(id, getBusinessId(h));
        return ResponseEntity.ok(
                saleTransactionRepository.findByBusinessIdAndCustomerIdOrderByCreatedAtDesc(getBusinessId(h), id)
        );
    }

    // PUT /api/customers/{id}/link-supplier/{supplierId}
    // Links this customer to an existing supplier record.
    @PutMapping("/{id}/link-supplier/{supplierId}")
    public ResponseEntity<Map<String, Object>> linkSupplier(
            @PathVariable Long id,
            @PathVariable Long supplierId,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        Customer c = customerService.getCustomerById(id, getBusinessId(h));
        c.setLinkedSupplierId(supplierId);
        return ResponseEntity.ok(toMap(customerService.save(c)));
    }

    // DELETE /api/customers/{id}/link-supplier
    // Removes the supplier link from a customer.
    @DeleteMapping("/{id}/link-supplier")
    public ResponseEntity<Map<String, Object>> unlinkSupplier(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        Customer c = customerService.getCustomerById(id, getBusinessId(h));
        c.setLinkedSupplierId(null);
        return ResponseEntity.ok(toMap(customerService.save(c)));
    }
}