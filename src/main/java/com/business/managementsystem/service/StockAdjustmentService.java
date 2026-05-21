package com.business.managementsystem.service;

import com.business.managementsystem.model.BranchInventory;
import com.business.managementsystem.model.Product;
import com.business.managementsystem.model.StockAdjustment;
import com.business.managementsystem.repository.BranchInventoryRepository;
import com.business.managementsystem.repository.ProductRepository;
import com.business.managementsystem.repository.StockAdjustmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockAdjustmentService {

    private final StockAdjustmentRepository adjustmentRepository;
    private final ProductRepository         productRepository;
    private final BranchInventoryRepository branchInventoryRepository;

    public StockAdjustmentService(
            StockAdjustmentRepository adjustmentRepository,
            ProductRepository productRepository,
            BranchInventoryRepository branchInventoryRepository) {
        this.adjustmentRepository     = adjustmentRepository;
        this.productRepository        = productRepository;
        this.branchInventoryRepository = branchInventoryRepository;
    }

    // ── Get all adjustments ───────────────────────────────────────────
    // branchId present → adjustments for that branch only
    // branchId null    → all adjustments for the business (owner global)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllAdjustments(Long businessId,
                                                       Long branchId) {
        List<StockAdjustment> adjustments = (branchId != null)
                ? adjustmentRepository
                .findByBusinessIdAndBranchIdOrderByAdjustedAtDesc(
                        businessId, branchId)
                : adjustmentRepository
                .findByBusinessIdOrderByAdjustedAtDesc(businessId);

        return adjustments.stream().map(this::toMap).toList();
    }

    // ── Get adjustments for a specific product ────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProductAdjustments(
            Long businessId, Long productId) {
        return adjustmentRepository
                .findByBusinessIdAndProductId(businessId, productId)
                .stream()
                .map(this::toMap)
                .toList();
    }

    // ── Create an adjustment — branch-aware ───────────────────────────
    // branchId present: adjusts branch_inventory, syncs product.quantity,
    //                   records adjustment with branchId
    // branchId null:    adjusts product.quantity directly (legacy / global)
    @Transactional
    public Map<String, Object> adjustStock(Long businessId,
                                           Long productId,
                                           double newQuantity,
                                           StockAdjustment.Reason reason,
                                           String notes,
                                           String adjustedBy,
                                           Long branchId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found."));

        if (!product.getBusinessId().equals(businessId))
            throw new RuntimeException("Unauthorized.");

        if (newQuantity < 0)
            throw new RuntimeException("Quantity cannot be negative.");

        double oldQuantity;

        if (branchId != null) {
            // Branch-aware: adjust the branch_inventory entry
            BranchInventory inv = branchInventoryRepository
                    .findByBranchIdAndProductId(branchId, productId)
                    .orElse(new BranchInventory(branchId, productId, 0));

            oldQuantity = inv.getQuantity();
            inv.setQuantity(newQuantity);
            branchInventoryRepository.save(inv);

            // Sync product.quantity = total stock across all branches
            double total = branchInventoryRepository
                    .getTotalQuantityAcrossBranches(businessId, productId);
            product.setQuantity(total);
            productRepository.save(product);

        } else {
            // Legacy / global: adjust product directly
            oldQuantity = product.getQuantity();
            product.setQuantity(newQuantity);
            productRepository.save(product);
        }

        // Record audit trail — include branchId so it can be filtered later
        StockAdjustment adjustment = new StockAdjustment(
                businessId, branchId, product,
                oldQuantity, newQuantity, reason, notes, adjustedBy
        );
        StockAdjustment saved = adjustmentRepository.save(adjustment);
        return toMap(saved);
    }

    // Legacy overload — no branch context
    @Transactional
    public Map<String, Object> adjustStock(Long businessId,
                                           Long productId,
                                           int newQuantity,
                                           StockAdjustment.Reason reason,
                                           String notes,
                                           String adjustedBy) {
        return adjustStock(businessId, productId, newQuantity,
                reason, notes, adjustedBy, null);
    }

    // ── Summary stats ──────────────────────────────────────────────────
    // branchId present → branch-specific stats
    // branchId null    → business-wide stats
    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(Long businessId, Long branchId) {
        long count;
        int  totalLost;
        int  totalGained;

        if (branchId != null) {
            count       = adjustmentRepository
                    .countByBusinessIdAndBranchId(businessId, branchId);
            totalLost   = adjustmentRepository
                    .getTotalStockLostByBranch(businessId, branchId);
            totalGained = adjustmentRepository
                    .getTotalStockGainedByBranch(businessId, branchId);
        } else {
            count       = adjustmentRepository.countByBusinessId(businessId);
            totalLost   = adjustmentRepository.getTotalStockLost(businessId);
            totalGained = adjustmentRepository.getTotalStockGained(businessId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalAdjustments", count);
        result.put("totalStockLost",   Math.abs(totalLost));
        result.put("totalStockGained", totalGained);
        return result;
    }

    // ── Convert to safe map ────────────────────────────────────────────
    private Map<String, Object> toMap(StockAdjustment sa) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",          sa.getId());
        m.put("branchId",    sa.getBranchId());
        m.put("productId",   sa.getProduct().getId());
        m.put("productName", sa.getProduct().getName());
        m.put("oldQuantity", sa.getOldQuantity());
        m.put("newQuantity", sa.getNewQuantity());
        m.put("adjustment",  sa.getAdjustment());
        m.put("reason",      sa.getReason().name());
        m.put("notes",       sa.getNotes());
        m.put("adjustedBy",  sa.getAdjustedBy());
        m.put("adjustedAt",  sa.getAdjustedAt() != null
                ? sa.getAdjustedAt().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : null);
        return m;
    }
}