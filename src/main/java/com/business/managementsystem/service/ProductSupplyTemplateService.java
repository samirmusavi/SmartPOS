package com.business.managementsystem.service;

import com.business.managementsystem.model.*;
import com.business.managementsystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ProductSupplyTemplateService {

    private final ProductSupplyTemplateRepository templateRepository;
    private final ProductRepository               productRepository;
    private final SupplyRepository                supplyRepository;
    private final SupplyAdjustmentRepository      adjustmentRepository;
    private final BranchSupplyInventoryRepository branchSupplyInventoryRepository;
    private final BranchRepository                branchRepository;

    public ProductSupplyTemplateService(
            ProductSupplyTemplateRepository templateRepository,
            ProductRepository productRepository,
            SupplyRepository supplyRepository,
            SupplyAdjustmentRepository adjustmentRepository,
            BranchSupplyInventoryRepository branchSupplyInventoryRepository,
            BranchRepository branchRepository) {
        this.templateRepository              = templateRepository;
        this.productRepository               = productRepository;
        this.supplyRepository                = supplyRepository;
        this.adjustmentRepository            = adjustmentRepository;
        this.branchSupplyInventoryRepository = branchSupplyInventoryRepository;
        this.branchRepository                = branchRepository;
    }

    // ── Safe number parsing — handles both String and Number from JSON ──
    private long parseLong(Object value) {
        if (value == null) throw new RuntimeException("Missing required field.");
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString().trim());
    }

    private int parseInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString().trim());
    }

    private double parseDouble(Object value) {
        if (value == null) return 1.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString().trim());
    }

    // ── Get template for a product ────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTemplate(Long productId,
                                                 Long businessId) {
        return templateRepository
                .findByProductIdAndBusinessId(productId, businessId)
                .stream()
                .map(this::toMap)
                .toList();
    }

    // ── Save/update template — replaces all entries for the product ───
    @Transactional
    public List<Map<String, Object>> saveTemplate(
            Long productId, Long businessId,
            List<Map<String, Object>> entries) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found."));

        if (!product.getBusinessId().equals(businessId))
            throw new RuntimeException("Unauthorized.");

        templateRepository.deleteByProductId(productId);

        List<ProductSupplyTemplate> saved = new ArrayList<>();

        for (Map<String, Object> entry : entries) {
            Long   supplyId = parseLong(entry.get("supplyId"));
            double qty      = parseDouble(entry.get("quantityPerUnit"));

            if (qty <= 0) continue;

            Supply supply = supplyRepository.findById(supplyId)
                    .orElseThrow(() ->
                            new RuntimeException("Supply not found: " + supplyId));

            ProductSupplyTemplate template = new ProductSupplyTemplate(
                    businessId, product, supply, qty);
            saved.add(templateRepository.save(template));
        }

        return saved.stream().map(this::toMap).toList();
    }

    // ── Delete a single template entry ────────────────────────────────
    @Transactional
    public void deleteTemplateEntry(Long productId, Long supplyId) {
        templateRepository.deleteByProductIdAndSupplyId(productId, supplyId);
    }

    // ── Build deduction preview for a sale ────────────────────────────
    // Uses branch supply quantity when branchId is provided.
    // Falls back to supply.quantity (global) if branchId is null.
    @Transactional(readOnly = true)
    public List<Map<String, Object>> buildDeductionPreview(
            Long businessId,
            Long branchId,
            List<Map<String, Object>> saleItems) {

        Map<Long, Double>  supplyTotals = new HashMap<>();
        Map<Long, String>  supplyNames  = new HashMap<>();
        Map<Long, String>  supplyUnits  = new HashMap<>();
        Map<Long, Integer> supplyStock  = new HashMap<>();

        for (Map<String, Object> item : saleItems) {
            Long productId = parseLong(item.get("productId"));
            int  qtySold   = parseInt(item.get("quantitySold"));

            List<ProductSupplyTemplate> templates =
                    templateRepository.findByProductIdAndBusinessId(
                            productId, businessId);

            for (ProductSupplyTemplate t : templates) {
                Long   sid    = t.getSupply().getId();
                double needed = t.getQuantityPerUnit() * qtySold;

                supplyTotals.merge(sid, needed, Double::sum);
                supplyNames.put(sid, t.getSupply().getName());
                supplyUnits.put(sid, t.getSupply().getUnit());

                // Use branch quantity if branchId is set
                int qty;
                if (branchId != null) {
                    qty = branchSupplyInventoryRepository
                            .findByBranchIdAndSupplyId(branchId, sid)
                            .map(BranchSupplyInventory::getQuantity)
                            .orElse(0);
                } else {
                    qty = t.getSupply().getQuantity();
                }
                supplyStock.put(sid, qty);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : supplyTotals.entrySet()) {
            Long   sid      = entry.getKey();
            double needed   = entry.getValue();
            int    current  = supplyStock.getOrDefault(sid, 0);
            int    toDeduct = (int) Math.ceil(needed);
            int    after    = Math.max(0, current - toDeduct);

            Map<String, Object> m = new HashMap<>();
            m.put("supplyId",     sid);
            m.put("supplyName",   supplyNames.get(sid));
            m.put("unit",         supplyUnits.get(sid));
            m.put("toDeduct",     toDeduct);
            m.put("currentStock", current);
            m.put("stockAfter",   after);
            m.put("insufficient", needed > current);
            result.add(m);
        }

        return result;
    }

    // ── Apply deductions ──────────────────────────────────────────────
    // Deducts from branch_supply_inventory when branchId is present.
    // Falls back to supply.quantity (global) when no branch context.
    @Transactional
    public void applyDeductions(Long businessId,
                                Long branchId,
                                List<Map<String, Object>> deductions,
                                String performedBy) {

        for (Map<String, Object> d : deductions) {
            Long sid      = parseLong(d.get("supplyId"));
            int  toDeduct = parseInt(d.get("toDeduct"));

            if (toDeduct <= 0) continue;

            Supply supply = supplyRepository.findById(sid).orElse(null);
            if (supply == null) continue;

            if (branchId != null) {
                // Deduct from this branch's supply inventory
                BranchSupplyInventory bsi = branchSupplyInventoryRepository
                        .findByBranchIdAndSupplyId(branchId, sid)
                        .orElse(new BranchSupplyInventory(branchId, sid, 0));

                int oldQty = bsi.getQuantity();
                int newQty = Math.max(0, oldQty - toDeduct);
                bsi.setQuantity(newQty);
                branchSupplyInventoryRepository.save(bsi);

                // Sync global total on supply.quantity
                int total = branchSupplyInventoryRepository
                        .getTotalQuantityAcrossBranches(businessId, sid);
                supply.setQuantity(total);
                supplyRepository.save(supply);

                adjustmentRepository.save(new SupplyAdjustment(
                        businessId, sid, supply.getName(),
                        oldQty, newQty,
                        SupplyAdjustment.Type.USED,
                        "Auto-deducted from sale",
                        performedBy));
            } else {
                // Global deduction (no branch context)
                int oldQty = supply.getQuantity();
                int newQty = Math.max(0, oldQty - toDeduct);
                supply.setQuantity(newQty);
                supplyRepository.save(supply);

                adjustmentRepository.save(new SupplyAdjustment(
                        businessId, sid, supply.getName(),
                        oldQty, newQty,
                        SupplyAdjustment.Type.USED,
                        "Auto-deducted from sale",
                        performedBy));
            }
        }
    }

    // ── Convert template to map ───────────────────────────────────────
    private Map<String, Object> toMap(ProductSupplyTemplate t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",              t.getId());
        m.put("supplyId",        t.getSupply().getId());
        m.put("supplyName",      t.getSupply().getName());
        m.put("supplyUnit",      t.getSupply().getUnit());
        m.put("supplyStock",     t.getSupply().getQuantity());
        m.put("quantityPerUnit", t.getQuantityPerUnit());
        return m;
    }
}
