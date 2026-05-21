package com.business.managementsystem.service;

import com.business.managementsystem.model.*;
import com.business.managementsystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Handles inter-branch supply transfers.
 *
 * Lifecycle:
 *   CREATE  → deduct from source branch_supply_inventory, status = PENDING
 *   COMPLETE → add to destination branch_supply_inventory, status = COMPLETED
 *   CANCEL  → return qty to source branch_supply_inventory, status = CANCELLED
 *
 * After every state change, supply.quantity (global total) is re-synced
 * from the sum across all branch_supply_inventory rows.
 */
@Service
public class SupplyTransferService {

    private final SupplyTransferRepository        transferRepository;
    private final SupplyRepository                supplyRepository;
    private final BranchSupplyInventoryRepository branchSupplyInvRepo;
    private final BranchRepository                branchRepository;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public SupplyTransferService(
            SupplyTransferRepository transferRepository,
            SupplyRepository supplyRepository,
            BranchSupplyInventoryRepository branchSupplyInvRepo,
            BranchRepository branchRepository) {
        this.transferRepository  = transferRepository;
        this.supplyRepository    = supplyRepository;
        this.branchSupplyInvRepo = branchSupplyInvRepo;
        this.branchRepository    = branchRepository;
    }

    // ── List all transfers for a business ──────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAll(Long businessId) {
        List<SupplyTransfer> transfers =
                transferRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);

        // Pre-load branch names for efficiency
        Map<Long, String> branchNames = new HashMap<>();
        branchRepository
                .findByBusinessIdOrderByIsMainBranchDescNameAsc(businessId)
                .forEach(b -> branchNames.put(b.getId(), b.getName()));

        // Pre-load supply names
        Map<Long, String> supplyNames = new HashMap<>();
        supplyRepository.findByBusinessIdOrderByNameAsc(businessId)
                .forEach(s -> supplyNames.put(s.getId(), s.getName()));

        return transfers.stream()
                .map(t -> toMap(t, branchNames, supplyNames))
                .toList();
    }

    // ── Count pending incoming for a branch ───────────────────────
    @Transactional(readOnly = true)
    public long getPendingIncomingCount(Long branchId) {
        return transferRepository.countByToBranchIdAndStatus(
                branchId, SupplyTransfer.Status.PENDING);
    }

    // ── Create transfer ───────────────────────────────────────────
    @Transactional
    public Map<String, Object> createTransfer(Long businessId,
                                              Long fromBranchId,
                                              Long toBranchId,
                                              Long supplyId,
                                              int quantity,
                                              String notes,
                                              String createdBy) {
        // Validate
        if (fromBranchId.equals(toBranchId))
            throw new RuntimeException("Source and destination must be different.");
        if (quantity <= 0)
            throw new RuntimeException("Quantity must be positive.");

        Supply supply = supplyRepository
                .findByIdAndBusinessId(supplyId, businessId)
                .orElseThrow(() -> new RuntimeException("Supply not found."));

        // Check source has enough stock
        BranchSupplyInventory sourceInv = branchSupplyInvRepo
                .findByBranchIdAndSupplyId(fromBranchId, supplyId)
                .orElseThrow(() -> new RuntimeException(
                        "Supply not found at source branch."));

        if (sourceInv.getQuantity() < quantity)
            throw new RuntimeException(
                    "Insufficient stock. Source has " + sourceInv.getQuantity()
                            + " but transfer requires " + quantity + ".");

        // Deduct from source immediately
        sourceInv.setQuantity(sourceInv.getQuantity() - quantity);
        branchSupplyInvRepo.save(sourceInv);

        // Sync global total
        syncGlobalTotal(businessId, supplyId, supply);

        // Create the transfer record
        SupplyTransfer transfer = new SupplyTransfer(
                businessId, fromBranchId, toBranchId,
                supplyId, quantity, notes, createdBy);
        SupplyTransfer saved = transferRepository.save(transfer);

        return toMapSimple(saved, supply.getName());
    }

    // ── Complete transfer ─────────────────────────────────────────
    @Transactional
    public Map<String, Object> completeTransfer(Long id, Long businessId,
                                                String completedBy) {
        SupplyTransfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transfer not found."));

        if (!transfer.getBusinessId().equals(businessId))
            throw new RuntimeException("Transfer does not belong to this business.");
        if (transfer.getStatus() != SupplyTransfer.Status.PENDING)
            throw new RuntimeException("Only pending transfers can be completed.");

        Supply supply = supplyRepository.findById(transfer.getSupplyId())
                .orElseThrow(() -> new RuntimeException("Supply not found."));

        // Add to destination
        BranchSupplyInventory destInv = branchSupplyInvRepo
                .findByBranchIdAndSupplyId(transfer.getToBranchId(),
                        transfer.getSupplyId())
                .orElse(new BranchSupplyInventory(
                        transfer.getToBranchId(),
                        transfer.getSupplyId(), 0));

        destInv.setQuantity(destInv.getQuantity() + transfer.getQuantity());
        branchSupplyInvRepo.save(destInv);

        // Sync global total
        syncGlobalTotal(businessId, transfer.getSupplyId(), supply);

        // Mark completed
        transfer.setStatus(SupplyTransfer.Status.COMPLETED);
        transfer.setCompletedBy(completedBy);
        transfer.setCompletedAt(LocalDateTime.now());
        transferRepository.save(transfer);

        return toMapSimple(transfer, supply.getName());
    }

    // ── Cancel transfer ───────────────────────────────────────────
    @Transactional
    public Map<String, Object> cancelTransfer(Long id, Long businessId) {
        SupplyTransfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transfer not found."));

        if (!transfer.getBusinessId().equals(businessId))
            throw new RuntimeException("Transfer does not belong to this business.");
        if (transfer.getStatus() != SupplyTransfer.Status.PENDING)
            throw new RuntimeException("Only pending transfers can be cancelled.");

        Supply supply = supplyRepository.findById(transfer.getSupplyId())
                .orElseThrow(() -> new RuntimeException("Supply not found."));

        // Return stock to source
        BranchSupplyInventory sourceInv = branchSupplyInvRepo
                .findByBranchIdAndSupplyId(transfer.getFromBranchId(),
                        transfer.getSupplyId())
                .orElse(new BranchSupplyInventory(
                        transfer.getFromBranchId(),
                        transfer.getSupplyId(), 0));

        sourceInv.setQuantity(sourceInv.getQuantity() + transfer.getQuantity());
        branchSupplyInvRepo.save(sourceInv);

        // Sync global total
        syncGlobalTotal(businessId, transfer.getSupplyId(), supply);

        // Mark cancelled
        transfer.setStatus(SupplyTransfer.Status.CANCELLED);
        transferRepository.save(transfer);

        return toMapSimple(transfer, supply.getName());
    }

    // ── Sync global total from branch sums ────────────────────────
    private void syncGlobalTotal(Long businessId, Long supplyId,
                                 Supply supply) {
        int total = branchSupplyInvRepo
                .getTotalQuantityAcrossBranches(businessId, supplyId);
        supply.setQuantity(total);
        supplyRepository.save(supply);
    }

    // ── Map with branch/supply names (for list view) ──────────────
    private Map<String, Object> toMap(SupplyTransfer t,
                                      Map<Long, String> branchNames,
                                      Map<Long, String> supplyNames) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",             t.getId());
        m.put("supplyId",       t.getSupplyId());
        m.put("supplyName",     supplyNames.getOrDefault(t.getSupplyId(), "Unknown"));
        m.put("fromBranchId",   t.getFromBranchId());
        m.put("fromBranchName", branchNames.getOrDefault(t.getFromBranchId(), "?"));
        m.put("toBranchId",     t.getToBranchId());
        m.put("toBranchName",   branchNames.getOrDefault(t.getToBranchId(), "?"));
        m.put("quantity",       t.getQuantity());
        m.put("status",         t.getStatus().name());
        m.put("notes",          t.getNotes());
        m.put("createdBy",      t.getCreatedBy());
        m.put("completedBy",    t.getCompletedBy());
        m.put("createdAt",      t.getCreatedAt() != null
                ? t.getCreatedAt().format(DT_FMT) : null);
        m.put("completedAt",    t.getCompletedAt() != null
                ? t.getCompletedAt().format(DT_FMT) : null);
        return m;
    }

    // ── Simple map (for single record responses) ──────────────────
    private Map<String, Object> toMapSimple(SupplyTransfer t,
                                            String supplyName) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",           t.getId());
        m.put("supplyName",   supplyName);
        m.put("quantity",     t.getQuantity());
        m.put("status",       t.getStatus().name());
        m.put("createdAt",    t.getCreatedAt() != null
                ? t.getCreatedAt().format(DT_FMT) : null);
        return m;
    }
}
