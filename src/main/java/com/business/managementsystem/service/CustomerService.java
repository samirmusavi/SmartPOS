package com.business.managementsystem.service;

import com.business.managementsystem.model.Customer;
import com.business.managementsystem.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    // ── Get all customers ────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers(Long businessId) {
        return customerRepository
                .findByBusinessIdOrderByFullNameAsc(businessId);
    }

    // ── Get top customers by spend ───────────────────────────────
    @Transactional(readOnly = true)
    public List<Customer> getTopCustomers(Long businessId) {
        return customerRepository
                .findByBusinessIdOrderByTotalSpentDesc(businessId);
    }

    // ── Search customers ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Customer> searchCustomers(Long businessId, String query) {
        return customerRepository.searchCustomers(businessId, query);
    }

    // ── Get customer by ID ───────────────────────────────────────
    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id, Long businessId) {
        return customerRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Customer not found."));
    }

    // ── Create customer ──────────────────────────────────────────
    @Transactional
    public Customer createCustomer(Long businessId, String fullName,
                                   String phone, String email, String notes) {
        if (fullName == null || fullName.isBlank()) {
            throw new RuntimeException("Customer name is required.");
        }
        if (phone != null && !phone.isBlank()
                && customerRepository.existsByPhoneAndBusinessId(phone, businessId)) {
            throw new RuntimeException(
                    "A customer with this phone number already exists.");
        }
        if (email != null && !email.isBlank()
                && customerRepository.existsByEmailAndBusinessId(email, businessId)) {
            throw new RuntimeException(
                    "A customer with this email already exists.");
        }
        Customer customer = new Customer(
                businessId, fullName, phone, email, notes);
        return customerRepository.save(customer);
    }

    // ── Update customer ──────────────────────────────────────────
    @Transactional
    public Customer updateCustomer(Long id, Long businessId, String fullName,
                                   String phone, String email, String notes) {
        Customer customer = customerRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Customer not found."));

        if (fullName == null || fullName.isBlank()) {
            throw new RuntimeException("Customer name is required.");
        }

        customer.setFullName(fullName);
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setNotes(notes);
        return customerRepository.save(customer);
    }

    // ── Delete customer ──────────────────────────────────────────
    @Transactional
    public void deleteCustomer(Long id, Long businessId) {
        Customer customer = customerRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Customer not found."));
        customerRepository.delete(customer);
    }

    // ── Record a purchase against a customer ─────────────────────
    @Transactional
    public Customer recordPurchase(Long customerId, Long businessId,
                                   BigDecimal amount) {
        Customer customer = customerRepository
                .findByIdAndBusinessId(customerId, businessId)
                .orElseThrow(() -> new RuntimeException("Customer not found."));
        customer.recordPurchase(amount, 0);
        return customerRepository.save(customer);
    }

    // ── Summary stats ────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(Long businessId) {
        long count           = customerRepository.countByBusinessId(businessId);
        BigDecimal totalRev  = customerRepository
                .getTotalRevenueFromCustomers(businessId);

        List<Customer> top = customerRepository
                .findByBusinessIdOrderByTotalSpentDesc(businessId);

        Map<String, Object> result = new HashMap<>();
        result.put("totalCustomers",  count);
        result.put("totalRevenue",    totalRev != null ? totalRev : BigDecimal.ZERO);
        result.put("avgSpend",        count > 0
                ? totalRev.divide(BigDecimal.valueOf(count),
                2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        result.put("topCustomer",     top.isEmpty() ? null : top.get(0).getFullName());
        result.put("topCustomerSpend", top.isEmpty()
                ? BigDecimal.ZERO : top.get(0).getTotalSpent());
        return result;
    }
}