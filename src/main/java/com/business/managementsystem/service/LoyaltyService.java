package com.business.managementsystem.service;

import com.business.managementsystem.model.Customer;
import com.business.managementsystem.model.SaleTransaction;
import com.business.managementsystem.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class LoyaltyService {

    private final CustomerRepository customerRepository;

    public LoyaltyService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Process loyalty points for a completed transaction.
     * Earns 1 point per AED spent (based on subtotal).
     */
    @Transactional
    public void processLoyaltyForTransaction(SaleTransaction transaction) {
        if (transaction.getCustomerId() == null) {
            return;
        }

        Customer customer = customerRepository.findById(transaction.getCustomerId()).orElse(null);
        if (customer == null || !customer.getBusinessId().equals(transaction.getBusinessId())) {
            return;
        }

        // Calculate points based on totalAmount (or subtotal). We'll use totalAmount.
        int pointsEarned = transaction.getTotalAmount().intValue();

        customer.recordPurchase(transaction.getTotalAmount(), pointsEarned);
        customerRepository.save(customer);
    }

    /**
     * Calculate loyalty tier and benefits dynamically.
     */
    public Map<String, Object> getCustomerLoyaltyProfile(Customer customer) {
        Map<String, Object> profile = new HashMap<>();
        
        int points = customer.getLoyaltyPoints();
        String tier;
        int nextTierThreshold;
        String nextTierName;
        
        if (points >= 10000) {
            tier = "GOLD";
            nextTierThreshold = -1;
            nextTierName = "MAX TIER";
        } else if (points >= 3000) {
            tier = "SILVER";
            nextTierThreshold = 10000;
            nextTierName = "GOLD";
        } else {
            tier = "BRONZE";
            nextTierThreshold = 3000;
            nextTierName = "SILVER";
        }

        profile.put("points", points);
        profile.put("tier", tier);
        
        if (nextTierThreshold > 0) {
            profile.put("pointsToNextTier", nextTierThreshold - points);
            profile.put("nextTier", nextTierName);
        } else {
            profile.put("pointsToNextTier", 0);
            profile.put("nextTier", "MAX TIER");
        }
        
        profile.put("totalSpent", customer.getTotalSpent());
        profile.put("visitCount", customer.getVisitCount());

        return profile;
    }
}
