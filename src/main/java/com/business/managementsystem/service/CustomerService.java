package com.business.managementsystem.service;

import com.business.managementsystem.model.Customer;
import com.business.managementsystem.model.Supplier;
import com.business.managementsystem.repository.CustomerRepository;
import com.business.managementsystem.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierService    supplierService;

    public CustomerService(CustomerRepository customerRepository,
                           SupplierRepository supplierRepository,
                           SupplierService    supplierService) {
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.supplierService    = supplierService;
    }

    @Transactional
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    // ── Customer table only (used by SaleService etc.) ───────────────────
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers(Long businessId) {
        return customerRepository.findByBusinessIdOrderByFullNameAsc(businessId);
    }

    // ── Merged list: customer table + supplier table WHERE isCustomer=true ──
    // Customer table records use positive IDs.
    // Supplier-backed records use negative IDs (-supplierId).
    // Deduplication: if a party and a customer share phone or email, the
    // customer-table record wins (it has loyalty points / spend history).
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllCustomersMerged(Long businessId) {
        // Step 1: customer table
        List<Map<String, Object>> merged = new ArrayList<>(
                customerRepository.findByBusinessIdOrderByFullNameAsc(businessId)
                        .stream().map(this::toMap).collect(Collectors.toList())
        );

        // Build dedup sets from customer-table records
        Set<String> seenPhones = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();
        for (Map<String, Object> m : merged) {
            String p = (String) m.get("phone");
            String e = (String) m.get("email");
            if (p != null && !p.isBlank()) seenPhones.add(p.toLowerCase());
            if (e != null && !e.isBlank()) seenEmails.add(e.toLowerCase());
        }

        // Step 2: supplier table WHERE isCustomer=true (negative IDs)
        for (Supplier s : supplierRepository.findByBusinessIdAndIsCustomerTrueOrderByNameAsc(businessId)) {
            String p = s.getPhone();
            String e = s.getEmail();
            boolean dup =
                    (p != null && !p.isBlank() && seenPhones.contains(p.toLowerCase())) ||
                    (e != null && !e.isBlank() && seenEmails.contains(e.toLowerCase()));
            if (!dup) {
                merged.add(toCustomerMapFromSupplier(s));
                if (p != null && !p.isBlank()) seenPhones.add(p.toLowerCase());
                if (e != null && !e.isBlank()) seenEmails.add(e.toLowerCase());
            }
        }

        merged.sort(Comparator.comparing(m -> ((String) m.get("fullName")).toLowerCase()));
        return merged;
    }

    // ── Get by id — positive = customer table, negative = supplier table ─
    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerOrPartyById(Long id, Long businessId) {
        if (id < 0) {
            Supplier s = supplierRepository.findByIdAndBusinessId(-id, businessId)
                    .orElseThrow(() -> new RuntimeException("Customer not found."));
            return toCustomerMapFromSupplier(s);
        }
        return toMap(customerRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Customer not found.")));
    }

    // ── Get top customers by spend (customer table only) ─────────────────
    @Transactional(readOnly = true)
    public List<Customer> getTopCustomers(Long businessId) {
        return customerRepository.findByBusinessIdOrderByTotalSpentDesc(businessId);
    }

    // ── Search — merged across both tables ────────────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchMerged(Long businessId, String query) {
        List<Map<String, Object>> result = new ArrayList<>(
                customerRepository.searchCustomers(businessId, query)
                        .stream().map(this::toMap).collect(Collectors.toList())
        );

        Set<String> seenPhones = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();
        for (Map<String, Object> m : result) {
            String p = (String) m.get("phone");
            String e = (String) m.get("email");
            if (p != null && !p.isBlank()) seenPhones.add(p.toLowerCase());
            if (e != null && !e.isBlank()) seenEmails.add(e.toLowerCase());
        }

        String lower = query == null ? "" : query.toLowerCase();
        supplierRepository.findByBusinessIdAndIsCustomerTrueOrderByNameAsc(businessId)
                .stream()
                .filter(s -> s.getName().toLowerCase().contains(lower)
                        || (s.getPhone() != null && s.getPhone().toLowerCase().contains(lower))
                        || (s.getEmail() != null && s.getEmail().toLowerCase().contains(lower)))
                .forEach(s -> {
                    String p = s.getPhone();
                    String e = s.getEmail();
                    boolean dup =
                            (p != null && !p.isBlank() && seenPhones.contains(p.toLowerCase())) ||
                            (e != null && !e.isBlank() && seenEmails.contains(e.toLowerCase()));
                    if (!dup) result.add(toCustomerMapFromSupplier(s));
                });

        return result;
    }

    // ── Get customer by ID (customer table only — used by SaleService etc.) ─
    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id, Long businessId) {
        return customerRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Customer not found."));
    }

    // ── Create customer — always saves to customer table ─────────────────
    @Transactional
    public Customer createCustomer(Long businessId, String fullName,
                                   String phone, String email, String notes) {
        if (fullName == null || fullName.isBlank())
            throw new RuntimeException("Customer name is required.");
        if (phone != null && !phone.isBlank()
                && customerRepository.existsByPhoneAndBusinessId(phone, businessId))
            throw new RuntimeException("A customer with this phone number already exists.");
        if (email != null && !email.isBlank()
                && customerRepository.existsByEmailAndBusinessId(email, businessId))
            throw new RuntimeException("A customer with this email already exists.");
        return customerRepository.save(new Customer(businessId, fullName, phone, email, notes));
    }

    // ── Update — routes by id sign ────────────────────────────────────────
    @Transactional
    public Map<String, Object> updateCustomerOrParty(Long id, Long businessId,
                                                      String fullName, String phone,
                                                      String email, String notes) {
        if (id < 0) {
            Supplier s = supplierRepository.findByIdAndBusinessId(-id, businessId)
                    .orElseThrow(() -> new RuntimeException("Customer not found."));
            if (fullName == null || fullName.isBlank())
                throw new RuntimeException("Name is required.");
            s.setName(fullName);
            s.setPhone(phone);
            s.setEmail(email);
            s.setNotes(notes);
            return toCustomerMapFromSupplier(supplierRepository.save(s));
        }
        return toMap(updateCustomer(id, businessId, fullName, phone, email, notes));
    }

    // ── Update customer table record (internal use) ───────────────────────
    @Transactional
    public Customer updateCustomer(Long id, Long businessId, String fullName,
                                   String phone, String email, String notes) {
        Customer c = customerRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Customer not found."));
        if (fullName == null || fullName.isBlank())
            throw new RuntimeException("Customer name is required.");
        c.setFullName(fullName);
        c.setPhone(phone);
        c.setEmail(email);
        c.setNotes(notes);
        return customerRepository.save(c);
    }

    // ── Delete — routes by id sign ────────────────────────────────────────
    // Customer table → hard delete. Supplier-backed → soft delete (isCustomer=false).
    @Transactional
    public void deleteCustomerOrParty(Long id, Long businessId) {
        if (id < 0) {
            Supplier s = supplierRepository.findByIdAndBusinessId(-id, businessId)
                    .orElseThrow(() -> new RuntimeException("Customer not found."));
            s.setCustomer(false);
            supplierRepository.save(s);
            return;
        }
        deleteCustomer(id, businessId);
    }

    // ── Delete customer table record ──────────────────────────────────────
    @Transactional
    public void deleteCustomer(Long id, Long businessId) {
        Customer c = customerRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Customer not found."));
        customerRepository.delete(c);
    }

    // ── Promote customer → also a supplier (one-click "Both") ───────────
    /**
     * Promotes a customer so they also appear in the Suppliers list.
     *
     * Two paths depending on how the customer record is identified:
     *
     * Negative ID (supplier-backed customer, id = -supplierId):
     *   The supplier record already exists in the supplier table with
     *   isCustomer=true, isSupplier=false.  Simply flip isSupplier=true.
     *   No new record is created.
     *
     * Positive ID (customer table record):
     *   1. Duplicate guard: search for an existing supplier row with
     *      matching phone or email.  If found, reuse it (set isSupplier=true,
     *      isCustomer=true if not already), then link customer.linkedSupplierId.
     *   2. If no match: create a new Supplier row (isSupplier=true, isCustomer=true)
     *      copying the customer's name/phone/email/notes, generate a party code,
     *      and link customer.linkedSupplierId.
     *   The customer table record is kept intact so loyalty/spend data is preserved.
     *   The deduplication in getAllCustomersMerged() ensures no duplicate appears
     *   in customers.html (customer-table record wins when phone/email match).
     *
     * parties.html automatically reflects both badges because it reads the
     * isSupplier/isCustomer flags directly from the supplier table. No change there.
     */
    @Transactional
    public Map<String, Object> promoteToSupplier(Long id, Long businessId) {

        // ── Path 1: supplier-backed customer (negative ID) ───────────────
        if (id < 0) {
            Long supplierId = -id;
            Supplier s = supplierRepository.findByIdAndBusinessId(supplierId, businessId)
                    .orElseThrow(() -> new RuntimeException("Customer not found."));
            if (s.isSupplier()) {
                throw new RuntimeException(s.getName() + " is already a supplier.");
            }
            s.setSupplier(true);
            return toCustomerMapFromSupplier(supplierRepository.save(s));
        }

        // ── Path 2: pure customer-table record (positive ID) ─────────────
        Customer c = customerRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Customer not found."));

        // If already linked, validate the link is still valid and active
        if (c.getLinkedSupplierId() != null) {
            Optional<Supplier> linked =
                    supplierRepository.findById(c.getLinkedSupplierId());
            if (linked.isPresent()) {
                Supplier s = linked.get();
                if (s.isSupplier()) {
                    throw new RuntimeException(c.getFullName() + " is already a supplier.");
                }
                // Linked record exists but isSupplier=false — promote it now
                s.setSupplier(true);
                supplierRepository.save(s);
                return toMap(c);
            }
            // Stale linkedSupplierId (supplier was deleted) — clear it and continue
            c.setLinkedSupplierId(null);
        }

        // Duplicate guard: find existing supplier with matching phone or email
        Supplier matched = null;
        for (Supplier s : supplierRepository.findByBusinessIdOrderByNameAsc(businessId)) {
            boolean phoneMatch = c.getPhone() != null && !c.getPhone().isBlank()
                    && c.getPhone().equalsIgnoreCase(s.getPhone());
            boolean emailMatch = c.getEmail() != null && !c.getEmail().isBlank()
                    && c.getEmail().equalsIgnoreCase(s.getEmail());
            if (phoneMatch || emailMatch) {
                matched = s;
                break;
            }
        }

        Supplier supplier;
        if (matched != null) {
            // Reuse the existing supplier record — set any missing flags
            supplier = matched;
            if (!supplier.isSupplier()) supplier.setSupplier(true);
            if (!supplier.isCustomer()) supplier.setCustomer(true);
            supplier = supplierRepository.save(supplier);
        } else {
            // Create a fresh supplier record mirroring the customer's data
            supplier = new Supplier(businessId, c.getFullName(), null,
                    c.getPhone(), c.getEmail(), null, c.getNotes());
            supplier.setSupplier(true);
            supplier.setCustomer(true);   // keeps them in the customer list too
            supplier.setKycStatus("NOT_VERIFIED");
            supplier.setPartyCode(supplierService.generatePartyCode(c.getFullName(), businessId));
            supplier = supplierRepository.save(supplier);
        }

        // Link customer table record → supplier record
        c.setLinkedSupplierId(supplier.getId());
        customerRepository.save(c);

        return toMap(c);
    }

    // ── Record purchase (customer table only) ────────────────────────────
    @Transactional
    public Customer recordPurchase(Long customerId, Long businessId, BigDecimal amount) {
        Customer c = customerRepository.findByIdAndBusinessId(customerId, businessId)
                .orElseThrow(() -> new RuntimeException("Customer not found."));
        c.recordPurchase(amount, 0);
        return customerRepository.save(c);
    }

    // ── Merged summary stats ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getMergedSummary(Long businessId) {
        long custCount  = customerRepository.countByBusinessId(businessId);
        long partyCount = supplierRepository.countByBusinessIdAndIsCustomerTrue(businessId);
        BigDecimal totalRev = customerRepository.getTotalRevenueFromCustomers(businessId);
        if (totalRev == null) totalRev = BigDecimal.ZERO;

        List<Customer> top = customerRepository.findByBusinessIdOrderByTotalSpentDesc(businessId);

        Map<String, Object> result = new HashMap<>();
        result.put("totalCustomers",   custCount + partyCount);
        result.put("totalRevenue",     totalRev);
        result.put("avgSpend",         custCount > 0
                ? totalRev.divide(BigDecimal.valueOf(custCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        result.put("topCustomer",      top.isEmpty() ? null : top.get(0).getFullName());
        result.put("topCustomerSpend", top.isEmpty() ? BigDecimal.ZERO : top.get(0).getTotalSpent());
        return result;
    }

    // ── Legacy summary alias (backward compat) ───────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(Long businessId) {
        return getMergedSummary(businessId);
    }

    // ── Legacy search (customer table only — backward compat) ────────────
    @Transactional(readOnly = true)
    public List<Customer> searchCustomers(Long businessId, String query) {
        return customerRepository.searchCustomers(businessId, query);
    }

    // ── Map Customer entity → response map ───────────────────────────────
    public Map<String, Object> toMap(Customer c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",              c.getId());
        m.put("fullName",        c.getFullName());
        m.put("phone",           c.getPhone());
        m.put("email",           c.getEmail());
        m.put("notes",           c.getNotes());
        m.put("loyaltyPoints",   c.getLoyaltyPoints());
        m.put("totalSpent",      c.getTotalSpent());
        m.put("visitCount",      c.getVisitCount());
        m.put("lastVisitAt",     c.getLastVisitAt() != null
                ? c.getLastVisitAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : null);
        m.put("linkedSupplierId", c.getLinkedSupplierId());
        m.put("createdAt",       c.getCreatedAt() != null
                ? c.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : null);
        return m;
    }

    // ── Map Supplier → customer response (negative ID namespace) ─────────
    private Map<String, Object> toCustomerMapFromSupplier(Supplier s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",              -s.getId());      // negative = supplier-backed
        m.put("fullName",        s.getName());
        m.put("phone",           s.getPhone());
        m.put("email",           s.getEmail());
        m.put("notes",           s.getNotes());
        m.put("loyaltyPoints",   0);
        m.put("totalSpent",      BigDecimal.ZERO);
        m.put("visitCount",      0);
        m.put("lastVisitAt",     null);
        // Expose supplier badge in customers.html when this party is also a supplier
        m.put("linkedSupplierId", s.isSupplier() ? s.getId() : null);
        m.put("createdAt",       s.getCreatedAt() != null
                ? s.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : null);
        return m;
    }
}
