package com.business.managementsystem.controller;

import com.business.managementsystem.model.Customer;
import com.business.managementsystem.model.SaleTransaction;
import com.business.managementsystem.repository.SaleTransactionRepository;
import com.business.managementsystem.service.CustomerService;
import com.business.managementsystem.service.LoyaltyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final LoyaltyService loyaltyService;
    private final SaleTransactionRepository saleTransactionRepository;

    public CustomerController(CustomerService customerService,
                              LoyaltyService loyaltyService,
                              SaleTransactionRepository saleTransactionRepository) {
        this.customerService          = customerService;
        this.loyaltyService           = loyaltyService;
        this.saleTransactionRepository = saleTransactionRepository;
    }

    private Long getBusinessId(String header) {
        if (header == null || header.isBlank())
            throw new RuntimeException("Business ID header is required.");
        return Long.parseLong(header);
    }

    // GET /api/customers — merged: customer table + supplier table WHERE isCustomer=true
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(customerService.getAllCustomersMerged(getBusinessId(h)));
    }

    // GET /api/customers/summary
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(customerService.getMergedSummary(getBusinessId(h)));
    }

    // GET /api/customers/search?q=
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestParam String q) {
        return ResponseEntity.ok(customerService.searchMerged(getBusinessId(h), q));
    }

    // GET /api/customers/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        return ResponseEntity.ok(customerService.getCustomerOrPartyById(id, getBusinessId(h)));
    }

    // POST /api/customers — always saves to customer table
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
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.toMap(customer));
    }

    // PUT /api/customers/{id} — routes by id sign
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(
                customerService.updateCustomerOrParty(
                        id, getBusinessId(h),
                        request.get("fullName"),
                        request.get("phone"),
                        request.get("email"),
                        request.get("notes")
                )
        );
    }

    // DELETE /api/customers/{id}
    // Customer table → hard delete. Supplier-backed (id < 0) → sets isCustomer=false.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        customerService.deleteCustomerOrParty(id, getBusinessId(h));
        return ResponseEntity.noContent().build();
    }

    // POST /api/customers/{id}/purchase
    @PostMapping("/{id}/purchase")
    public ResponseEntity<Map<String, Object>> recordPurchase(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestBody Map<String, String> request) {
        if (id < 0) {
            return ResponseEntity.ok(customerService.getCustomerOrPartyById(id, getBusinessId(h)));
        }
        Customer c = customerService.recordPurchase(id, getBusinessId(h),
                new BigDecimal(request.get("amount")));
        return ResponseEntity.ok(customerService.toMap(c));
    }

    // GET /api/customers/{id}/loyalty
    @GetMapping("/{id}/loyalty")
    public ResponseEntity<Map<String, Object>> getLoyaltyProfile(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        if (id < 0) {
            Map<String, Object> profile = new HashMap<>();
            profile.put("customerId",    id);
            profile.put("loyaltyPoints", 0);
            profile.put("tier",          "BRONZE");
            profile.put("totalSpent",    BigDecimal.ZERO);
            profile.put("visitCount",    0);
            return ResponseEntity.ok(profile);
        }
        Customer c = customerService.getCustomerById(id, getBusinessId(h));
        return ResponseEntity.ok(loyaltyService.getCustomerLoyaltyProfile(c));
    }

    // GET /api/customers/{id}/history
    @GetMapping("/{id}/history")
    public ResponseEntity<List<SaleTransaction>> getHistory(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        if (id < 0) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(
                saleTransactionRepository.findByBusinessIdAndCustomerIdOrderByCreatedAtDesc(
                        getBusinessId(h), id));
    }

    // PUT /api/customers/{id}/link-supplier/{supplierId}
    @PutMapping("/{id}/link-supplier/{supplierId}")
    public ResponseEntity<Map<String, Object>> linkSupplier(
            @PathVariable Long id,
            @PathVariable Long supplierId,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        if (id < 0) {
            return ResponseEntity.ok(customerService.getCustomerOrPartyById(id, getBusinessId(h)));
        }
        Customer c = customerService.getCustomerById(id, getBusinessId(h));
        c.setLinkedSupplierId(supplierId);
        return ResponseEntity.ok(customerService.toMap(customerService.save(c)));
    }

    // DELETE /api/customers/{id}/link-supplier
    @DeleteMapping("/{id}/link-supplier")
    public ResponseEntity<Map<String, Object>> unlinkSupplier(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        if (id < 0) {
            return ResponseEntity.ok(customerService.getCustomerOrPartyById(id, getBusinessId(h)));
        }
        Customer c = customerService.getCustomerById(id, getBusinessId(h));
        c.setLinkedSupplierId(null);
        return ResponseEntity.ok(customerService.toMap(customerService.save(c)));
    }
}
