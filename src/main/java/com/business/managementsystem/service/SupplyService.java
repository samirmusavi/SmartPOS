package com.business.managementsystem.service;

import com.business.managementsystem.model.*;
import com.business.managementsystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SupplyService {

    private final SupplyRepository                supplyRepository;
    private final SupplyAdjustmentRepository      adjustmentRepository;
    private final SupplierRepository              supplierRepository;
    private final BranchSupplyInventoryRepository branchSupplyInventoryRepository;
    private final BranchRepository                branchRepository;

    public SupplyService(SupplyRepository supplyRepository,
                         SupplyAdjustmentRepository adjustmentRepository,
                         SupplierRepository supplierRepository,
                         BranchSupplyInventoryRepository branchSupplyInventoryRepository,
                         BranchRepository branchRepository) {
        this.supplyRepository                = supplyRepository;
        this.adjustmentRepository            = adjustmentRepository;
        this.supplierRepository              = supplierRepository;
        this.branchSupplyInventoryRepository = branchSupplyInventoryRepository;
        this.branchRepository                = branchRepository;
    }

    // ── Get all supplies ─────────────────────────────────────────────
    // branchId present → show branch quantity for each supply
    // branchId null    → show global total (supply.quantity)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllSupplies(Long businessId,
                                                    Long branchId) {
        List<Supply> supplies = supplyRepository
                .findByBusinessIdOrderByNameAsc(businessId);

        if (branchId != null) {
            // Build branch qty map once for efficiency
            List<BranchSupplyInventory> branchInv =
                    branchSupplyInventoryRepository.findByBranchId(branchId);
            Map<Long, Integer> branchQtyMap = new HashMap<>();
            branchInv.forEach(bsi -> branchQtyMap.put(bsi.getSupplyId(),
                    bsi.getQuantity()));

            return supplies.stream()
                    .map(s -> toMap(s, branchQtyMap.getOrDefault(s.getId(), 0)))
                    .toList();
        }

        return supplies.stream()
                .map(s -> toMap(s, s.getQuantity()))
                .toList();
    }

    // ── Get low stock supplies ────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLowStockSupplies(Long businessId,
                                                         Long branchId) {
        if (branchId != null) {
            List<BranchSupplyInventory> lowBranchInv =
                    branchSupplyInventoryRepository.findLowStockByBranch(branchId);
            return lowBranchInv.stream().map(bsi -> {
                Supply supply = supplyRepository.findById(bsi.getSupplyId())
                        .orElse(null);
                if (supply == null) return null;
                return toMap(supply, bsi.getQuantity());
            }).filter(Objects::nonNull).toList();
        }
        return supplyRepository
                .findLowStockByBusinessId(businessId)
                .stream()
                .map(s -> toMap(s, s.getQuantity()))
                .toList();
    }

    // ── Get single supply ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getSupplyById(Long id, Long businessId,
                                             Long branchId) {
        Supply supply = supplyRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Supply not found."));
        int qty = getBranchQty(supply.getId(), branchId, supply.getQuantity());
        return toMap(supply, qty);
    }

    // ── Create supply ─────────────────────────────────────────────────
    // Initial quantity is always 0 for all branches.
    // Branch managers adjust their own quantity via adjustStock.
    @Transactional
    public Map<String, Object> createSupply(Long businessId, String name,
                                            String category, String unit,
                                            int minimumThreshold,
                                            BigDecimal costPerUnit,
                                            Long supplierId, String notes) {
        if (name == null || name.isBlank())
            throw new RuntimeException("Supply name is required.");
        if (category == null || category.isBlank())
            throw new RuntimeException("Category is required.");
        if (unit == null || unit.isBlank())
            throw new RuntimeException("Unit is required.");

        // Supply is always created with quantity 0 — global total starts at 0
        Supply supply = new Supply(businessId, name, category, unit,
                0, minimumThreshold, costPerUnit, supplierId, notes);
        Supply saved = supplyRepository.save(supply);

        // Create branch_supply_inventory rows for all branches (qty = 0)
        List<Branch> branches = branchRepository
                .findByBusinessIdOrderByIsMainBranchDescNameAsc(businessId);
        for (Branch branch : branches) {
            branchSupplyInventoryRepository.save(
                    new BranchSupplyInventory(branch.getId(), saved.getId(), 0));
        }

        return toMap(saved, 0);
    }

    // ── Update supply (catalog fields only — no quantity change) ──────
    @Transactional
    public Map<String, Object> updateSupply(Long id, Long businessId,
                                            String name, String category,
                                            String unit, int minimumThreshold,
                                            BigDecimal costPerUnit,
                                            Long supplierId, String notes,
                                            Long branchId) {
        Supply supply = supplyRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Supply not found."));

        supply.setName(name);
        supply.setCategory(category);
        supply.setUnit(unit);
        supply.setMinimumThreshold(minimumThreshold);
        supply.setCostPerUnit(costPerUnit);
        supply.setSupplierId(supplierId);
        supply.setNotes(notes);
        Supply saved = supplyRepository.save(supply);

        int qty = getBranchQty(saved.getId(), branchId, saved.getQuantity());
        return toMap(saved, qty);
    }

    // ── Delete supply ─────────────────────────────────────────────────
    @Transactional
    public void deleteSupply(Long id, Long businessId) {
        Supply supply = supplyRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Supply not found."));

        // Remove all branch inventory rows for this supply
        List<BranchSupplyInventory> rows =
                branchSupplyInventoryRepository.findBySupplyId(id);
        branchSupplyInventoryRepository.deleteAll(rows);

        supplyRepository.delete(supply);
    }

    // ── Adjust supply stock for a specific branch ─────────────────────
    @Transactional
    public Map<String, Object> adjustStock(Long id, Long businessId,
                                           Long branchId,
                                           int newQuantity,
                                           SupplyAdjustment.Type type,
                                           String notes, String adjustedBy) {
        Supply supply = supplyRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Supply not found."));

        if (newQuantity < 0)
            throw new RuntimeException("Quantity cannot be negative.");

        if (branchId != null) {
            // Branch-aware adjustment
            BranchSupplyInventory bsi = branchSupplyInventoryRepository
                    .findByBranchIdAndSupplyId(branchId, id)
                    .orElse(new BranchSupplyInventory(branchId, id, 0));

            int oldQty = bsi.getQuantity();
            bsi.setQuantity(newQuantity);
            branchSupplyInventoryRepository.save(bsi);

            // Sync global total
            int total = branchSupplyInventoryRepository
                    .getTotalQuantityAcrossBranches(businessId, id);
            supply.setQuantity(total);
            supplyRepository.save(supply);

            // Audit trail
            adjustmentRepository.save(new SupplyAdjustment(
                    businessId, id, supply.getName(),
                    oldQty, newQuantity, type, notes, adjustedBy));

            return toMap(supply, newQuantity);
        } else {
            // Global adjustment (owner with no branch — adjusts supply.quantity directly)
            int oldQty = supply.getQuantity();
            supply.setQuantity(newQuantity);
            supplyRepository.save(supply);

            adjustmentRepository.save(new SupplyAdjustment(
                    businessId, id, supply.getName(),
                    oldQty, newQuantity, type, notes, adjustedBy));

            return toMap(supply, newQuantity);
        }
    }

    // ── Get adjustment history for a supply ───────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAdjustmentHistory(Long supplyId) {
        return adjustmentRepository
                .findBySupplyIdOrderByAdjustedAtDesc(supplyId)
                .stream()
                .map(this::adjToMap)
                .toList();
    }

    // ── Summary stats ──────────────────────────────────────────────────
    // Branch-aware: counts based on branch_supply_inventory when branchId present
    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(Long businessId, Long branchId) {
        long total = supplyRepository.countByBusinessId(businessId);
        long lowStock;
        long outStock;

        if (branchId != null) {
            lowStock = branchSupplyInventoryRepository
                    .findLowStockByBranch(branchId).size();
            outStock = branchSupplyInventoryRepository
                    .findOutOfStockByBranch(branchId).size();
        } else {
            lowStock = supplyRepository.countLowStockByBusinessId(businessId);
            outStock = supplyRepository.findOutOfStockByBusinessId(businessId).size();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalSupplies", total);
        result.put("lowStockCount", lowStock);
        result.put("outOfStock",    outStock);
        return result;
    }

    // ── Helper: get branch qty or fall back to global ─────────────────
    private int getBranchQty(Long supplyId, Long branchId, int globalQty) {
        if (branchId == null) return globalQty;
        return branchSupplyInventoryRepository
                .findByBranchIdAndSupplyId(branchId, supplyId)
                .map(BranchSupplyInventory::getQuantity)
                .orElse(0);
    }

    // ── Convert supply + display quantity to map ───────────────────────
    private Map<String, Object> toMap(Supply s, int displayQty) {
        String supplierName  = null;
        String supplierPhone = null;
        if (s.getSupplierId() != null) {
            var supplierOpt = supplierRepository.findById(s.getSupplierId());
            if (supplierOpt.isPresent()) {
                supplierName  = supplierOpt.get().getName();
                supplierPhone = supplierOpt.get().getPhone();
            }
        }

        String stockStatus;
        if (displayQty == 0)                          stockStatus = "OUT";
        else if (displayQty <= s.getMinimumThreshold()) stockStatus = "LOW";
        else                                           stockStatus = "OK";

        Map<String, Object> m = new HashMap<>();
        m.put("id",               s.getId());
        m.put("name",             s.getName());
        m.put("category",         s.getCategory());
        m.put("unit",             s.getUnit());
        m.put("quantity",         displayQty);          // branch qty or global total
        m.put("globalQuantity",   s.getQuantity());     // always global total
        m.put("minimumThreshold", s.getMinimumThreshold());
        m.put("costPerUnit",      s.getCostPerUnit());
        m.put("supplierId",       s.getSupplierId());
        m.put("supplierName",     supplierName);
        m.put("supplierPhone",    supplierPhone);
        m.put("notes",            s.getNotes());
        m.put("stockStatus",      stockStatus);
        m.put("isLow",            displayQty > 0 && displayQty <= s.getMinimumThreshold());
        m.put("isOut",            displayQty == 0);
        m.put("createdAt",        s.getCreatedAt() != null
                ? s.getCreatedAt().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : null);
        return m;
    }

    // ── Convert adjustment to map ─────────────────────────────────────
    private Map<String, Object> adjToMap(SupplyAdjustment a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",          a.getId());
        m.put("supplyId",    a.getSupplyId());
        m.put("supplyName",  a.getSupplyName());
        m.put("oldQuantity", a.getOldQuantity());
        m.put("newQuantity", a.getNewQuantity());
        m.put("adjustment",  a.getAdjustment());
        m.put("type",        a.getType().name());
        m.put("notes",       a.getNotes());
        m.put("adjustedBy",  a.getAdjustedBy());
        m.put("adjustedAt",  a.getAdjustedAt() != null
                ? a.getAdjustedAt().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : null);
        return m;
    }
}