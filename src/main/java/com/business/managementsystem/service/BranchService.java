package com.business.managementsystem.service;

import com.business.managementsystem.model.*;
import com.business.managementsystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class BranchService {

    private final BranchRepository                branchRepository;
    private final BranchInventoryRepository       inventoryRepository;
    private final BranchSupplyInventoryRepository supplyInventoryRepository;
    private final ProductRepository               productRepository;
    private final SupplyRepository                supplyRepository;
    private final BusinessRepository              businessRepository;

    public BranchService(BranchRepository branchRepository,
                         BranchInventoryRepository inventoryRepository,
                         BranchSupplyInventoryRepository supplyInventoryRepository,
                         ProductRepository productRepository,
                         SupplyRepository supplyRepository,
                         BusinessRepository businessRepository) {
        this.branchRepository         = branchRepository;
        this.inventoryRepository      = inventoryRepository;
        this.supplyInventoryRepository = supplyInventoryRepository;
        this.productRepository        = productRepository;
        this.supplyRepository         = supplyRepository;
        this.businessRepository       = businessRepository;
    }

    // ── Get all branches for a business ──────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBranches(Long businessId) {
        return branchRepository
                .findByBusinessIdOrderByIsMainBranchDescNameAsc(businessId)
                .stream()
                .map(this::toMap)
                .toList();
    }

    // ── Get active branches only ──────────────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActiveBranches(Long businessId) {
        return branchRepository
                .findByBusinessIdAndStatus(businessId, Branch.Status.ACTIVE)
                .stream()
                .map(this::toMap)
                .toList();
    }

    // ── Get single branch ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getBranch(Long id, Long businessId) {
        Branch branch = branchRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Branch not found."));
        return toMap(branch);
    }

    // ── Create branch ─────────────────────────────────────────────
    @Transactional
    public Map<String, Object> createBranch(Long businessId,
                                            String name,
                                            String address,
                                            String phone,
                                            String city,
                                            String country) {
        if (branchRepository.existsByBusinessIdAndName(businessId, name))
            throw new RuntimeException(
                    "A branch with this name already exists.");

        long current = branchRepository.countByBusinessId(businessId);
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found."));

        int limit = getBranchLimit(business.getPlan().name());
        if (current >= limit)
            throw new RuntimeException(
                    "Your plan allows a maximum of " + limit +
                            " branch" + (limit == 1 ? "" : "es") +
                            ". Please upgrade to add more.");

        Branch branch = new Branch();
        branch.setBusinessId(businessId);
        branch.setName(name);
        branch.setAddress(address);
        branch.setPhone(phone);
        branch.setCity(city);
        branch.setCountry(country != null ? country : "UAE");
        branch.setMainBranch(false);
        branch.setStatus(Branch.Status.ACTIVE);

        Branch saved = branchRepository.save(branch);

        // Initialize product inventory entries (qty = 0) for new branch
        List<Product> products = productRepository
                .findByBusinessIdAndActiveTrue(businessId);
        for (Product p : products) {
            inventoryRepository.save(
                    new BranchInventory(saved.getId(), p.getId(), 0));
        }

        // Initialize supply inventory entries (qty = 0) for new branch
        List<Supply> supplies = supplyRepository
                .findByBusinessIdOrderByNameAsc(businessId);
        for (Supply s : supplies) {
            supplyInventoryRepository.save(
                    new BranchSupplyInventory(saved.getId(), s.getId(), 0));
        }

        return toMap(saved);
    }

    // ── Update branch ─────────────────────────────────────────────
    @Transactional
    public Map<String, Object> updateBranch(Long id, Long businessId,
                                            String name, String address,
                                            String phone, String city,
                                            String country,
                                            Branch.Status status) {
        Branch branch = branchRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Branch not found."));

        if (!branch.getName().equals(name) &&
                branchRepository.existsByBusinessIdAndName(businessId, name))
            throw new RuntimeException("A branch with this name already exists.");

        branch.setName(name);
        branch.setAddress(address);
        branch.setPhone(phone);
        branch.setCity(city);
        branch.setCountry(country != null ? country : "UAE");
        if (status != null && !branch.isMainBranch()) {
            branch.setStatus(status);
        }

        return toMap(branchRepository.save(branch));
    }

    // ── Get inventory for a branch (products) ────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBranchInventory(Long branchId,
                                                        Long businessId) {
        branchRepository.findByIdAndBusinessId(branchId, businessId)
                .orElseThrow(() -> new RuntimeException("Branch not found."));

        List<Product> products = productRepository
                .findByBusinessIdAndActiveTrue(businessId);

        Map<Long, Product> productMap = new HashMap<>();
        products.forEach(p -> productMap.put(p.getId(), p));

        List<BranchInventory> inventories = inventoryRepository
                .findByBranchId(branchId);
        Map<Long, Double> invMap = new HashMap<>();
        inventories.forEach(inv -> invMap.put(inv.getProductId(),
                inv.getQuantity()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Product p : products) {
            double qty = invMap.getOrDefault(p.getId(), 0.0);
            Map<String, Object> m = new HashMap<>();
            m.put("productId",   p.getId());
            m.put("productName", p.getName());
            m.put("barcode",     p.getBarcode());
            m.put("category",    p.getCategory());
            m.put("price",       p.getPrice());
            m.put("costPrice",   p.getCostPrice());
            m.put("quantity",    qty);
            result.add(m);
        }
        return result;
    }

    // ── Migration: ensure every business has a Main Branch ────────
    @Transactional
    public void migrateExistingBusinesses() {
        List<Business> businesses = businessRepository.findAll();
        for (Business business : businesses) {
            Optional<Branch> existing = branchRepository
                    .findByBusinessIdAndIsMainBranchTrue(business.getId());

            if (existing.isEmpty()) {
                Branch main = new Branch(
                        business.getId(),
                        "Main Branch",
                        business.getBusinessName() != null
                                ? business.getBusinessName() : "Main",
                        "UAE",
                        true
                );
                Branch saved = branchRepository.save(main);

                List<Product> products = productRepository
                        .findByBusinessIdAndActiveTrue(business.getId());
                for (Product p : products) {
                    boolean exists = inventoryRepository
                            .findByBranchIdAndProductId(saved.getId(), p.getId())
                            .isPresent();
                    if (!exists) {
                        inventoryRepository.save(
                                new BranchInventory(
                                        saved.getId(), p.getId(), p.getQuantity()));
                    }
                }

                System.out.println("[SmartPOS] Migrated business " +
                        business.getId() + " → Main Branch created (id=" +
                        saved.getId() + ")");
            }
        }
    }

    // ── Migration: ensure every supply has a branch_supply_inventory row ──
    // For each branch, for each supply of that business,
    // create a row with quantity = 0 if it doesn't exist yet.
    // Existing supplies that already have data are not touched.
    @Transactional
    public void migrateSupplyInventory() {
        List<Business> businesses = businessRepository.findAll();
        for (Business business : businesses) {
            List<Branch> branches = branchRepository
                    .findByBusinessIdOrderByIsMainBranchDescNameAsc(
                            business.getId());
            List<Supply> supplies = supplyRepository
                    .findByBusinessIdOrderByNameAsc(business.getId());

            for (Branch branch : branches) {
                for (Supply supply : supplies) {
                    boolean exists = supplyInventoryRepository
                            .findByBranchIdAndSupplyId(
                                    branch.getId(), supply.getId())
                            .isPresent();
                    if (!exists) {
                        // New row defaults to 0 — branch managers
                        // adjust their own quantity
                        supplyInventoryRepository.save(
                                new BranchSupplyInventory(
                                        branch.getId(), supply.getId(), 0));
                    }
                }
            }
        }
    }

    // ── Branch limit per plan ─────────────────────────────────────
    private int getBranchLimit(String plan) {
        if (plan == null) return 1;
        return switch (plan.toUpperCase()) {
            case "BUSINESS"   -> 3;
            case "ENTERPRISE" -> Integer.MAX_VALUE;
            default           -> 1; // BASIC
        };
    }

    // ── Convert to map ────────────────────────────────────────────
    private Map<String, Object> toMap(Branch b) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",           b.getId());
        m.put("businessId",   b.getBusinessId());
        m.put("name",         b.getName());
        m.put("address",      b.getAddress());
        m.put("phone",        b.getPhone());
        m.put("city",         b.getCity());
        m.put("country",      b.getCountry());
        m.put("isMainBranch", b.isMainBranch());
        m.put("status",       b.getStatus().name());
        m.put("createdAt",    b.getCreatedAt() != null
                ? b.getCreatedAt().toString() : null);
        return m;
    }
}