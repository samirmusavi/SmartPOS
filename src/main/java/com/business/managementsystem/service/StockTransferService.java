package com.business.managementsystem.service;

import com.business.managementsystem.model.*;
import com.business.managementsystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Handles inter-branch product transfers.
 *
 * Lifecycle:
 *   CREATE  → deduct from source branch_inventory, status = PENDING
 *   COMPLETE → add to destination branch_inventory, status = COMPLETED
 *   CANCEL  → return qty to source branch_inventory, status = CANCELLED
 *
 * After every state change, product.quantity (global total) is re-synced
 * from the sum across all branch_inventory rows.
 */
@Service
public class StockTransferService {

    private final StockTransferRepository    transferRepository;
    private final ProductRepository          productRepository;
    private final BranchInventoryRepository  branchInvRepo;
    private final BranchRepository           branchRepository;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public StockTransferService(
            StockTransferRepository transferRepository,
            ProductRepository productRepository,
            BranchInventoryRepository branchInvRepo,
            BranchRepository branchRepository) {
        this.transferRepository = transferRepository;
        this.productRepository  = productRepository;
        this.branchInvRepo      = branchInvRepo;
        this.branchRepository   = branchRepository;
    }

    // ── List all transfers for a business ──────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAll(Long businessId) {
        return transferRepository
                .findByBusinessIdOrderByCreatedAtDesc(businessId)
                .stream()
                .map(this::toMap)
                .toList();
    }

    // ── Count pending incoming for a branch ───────────────────────
    @Transactional(readOnly = true)
    public long getPendingIncomingCount(Long branchId) {
        return transferRepository.countByToBranchIdAndStatus(
                branchId, StockTransfer.Status.PENDING);
    }

    // ── Create transfer ───────────────────────────────────────────
    @Transactional
    public Map<String, Object> createTransfer(Long businessId,
                                              Long fromBranchId,
                                              Long toBranchId,
                                              Long productId,
                                              double quantity,
                                              String notes,
                                              String createdBy) {
        // Validate
        if (fromBranchId.equals(toBranchId))
            throw new RuntimeException("Source and destination must be different.");
        if (quantity <= 0)
            throw new RuntimeException("Quantity must be positive.");

        Product product = productRepository
                .findById(productId)
                .filter(p -> p.getBusinessId().equals(businessId))
                .orElseThrow(() -> new RuntimeException("Product not found."));

        Branch fromBranch = branchRepository.findById(fromBranchId)
                .orElseThrow(() -> new RuntimeException("Source branch not found."));
        Branch toBranch = branchRepository.findById(toBranchId)
                .orElseThrow(() -> new RuntimeException("Destination branch not found."));

        // Check source has enough stock
        BranchInventory sourceInv = branchInvRepo
                .findByBranchIdAndProductId(fromBranchId, productId)
                .orElseThrow(() -> new RuntimeException(
                        "Product not found at source branch."));

        if (sourceInv.getQuantity() < quantity)
            throw new RuntimeException(
                    "Insufficient stock. Source has " + sourceInv.getQuantity()
                            + " but transfer requires " + quantity + ".");

        // Deduct from source immediately
        sourceInv.setQuantity(sourceInv.getQuantity() - quantity);
        branchInvRepo.save(sourceInv);

        // Sync global total
        syncGlobalTotal(businessId, productId, product);

        // Create the transfer record
        StockTransfer transfer = new StockTransfer();
        transfer.setBusinessId(businessId);
        transfer.setFromBranchId(fromBranchId);
        transfer.setFromBranchName(fromBranch.getName());
        transfer.setToBranchId(toBranchId);
        transfer.setToBranchName(toBranch.getName());
        transfer.setProductId(productId);
        transfer.setProductName(product.getName());
        transfer.setQuantity(quantity);
        transfer.setNotes(notes);
        transfer.setCreatedBy(createdBy);
        transfer.setStatus(StockTransfer.Status.PENDING);

        StockTransfer saved = transferRepository.save(transfer);
        return toMap(saved);
    }

    // ── Complete transfer ─────────────────────────────────────────
    @Transactional
    public Map<String, Object> completeTransfer(Long id, Long businessId,
                                                String completedBy) {
        StockTransfer transfer = transferRepository
                .findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Transfer not found."));

        if (transfer.getStatus() != StockTransfer.Status.PENDING)
            throw new RuntimeException("Only pending transfers can be completed.");

        Product product = productRepository.findById(transfer.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found."));

        // Add to destination branch_inventory
        BranchInventory destInv = branchInvRepo
                .findByBranchIdAndProductId(transfer.getToBranchId(),
                        transfer.getProductId())
                .orElse(null);

        if (destInv == null) {
            destInv = new BranchInventory();
            destInv.setBranchId(transfer.getToBranchId());
            destInv.setProductId(transfer.getProductId());
            destInv.setQuantity(0);
        }

        destInv.setQuantity(destInv.getQuantity() + transfer.getQuantity());
        branchInvRepo.save(destInv);

        // Sync global total
        syncGlobalTotal(businessId, transfer.getProductId(), product);

        // Mark completed
        transfer.setStatus(StockTransfer.Status.COMPLETED);
        transfer.setCompletedBy(completedBy);
        transferRepository.save(transfer);

        return toMap(transfer);
    }

    // ── Cancel transfer ───────────────────────────────────────────
    @Transactional
    public Map<String, Object> cancelTransfer(Long id, Long businessId) {
        StockTransfer transfer = transferRepository
                .findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Transfer not found."));

        if (transfer.getStatus() != StockTransfer.Status.PENDING)
            throw new RuntimeException("Only pending transfers can be cancelled.");

        Product product = productRepository.findById(transfer.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found."));

        // Return stock to source
        BranchInventory sourceInv = branchInvRepo
                .findByBranchIdAndProductId(transfer.getFromBranchId(),
                        transfer.getProductId())
                .orElse(null);

        if (sourceInv == null) {
            sourceInv = new BranchInventory();
            sourceInv.setBranchId(transfer.getFromBranchId());
            sourceInv.setProductId(transfer.getProductId());
            sourceInv.setQuantity(0);
        }

        sourceInv.setQuantity(sourceInv.getQuantity() + transfer.getQuantity());
        branchInvRepo.save(sourceInv);

        // Sync global total
        syncGlobalTotal(businessId, transfer.getProductId(), product);

        // Mark cancelled
        transfer.setStatus(StockTransfer.Status.CANCELLED);
        transferRepository.save(transfer);

        return toMap(transfer);
    }

    // ── Sync global total from branch sums ────────────────────────
    private void syncGlobalTotal(Long businessId, Long productId,
                                 Product product) {
        double total = branchInvRepo.getTotalQuantityAcrossBranches(
                businessId, productId);
        product.setQuantity(total);
        productRepository.save(product);
    }

    // ── Map transfer to response ──────────────────────────────────
    private Map<String, Object> toMap(StockTransfer t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",             t.getId());
        m.put("productId",      t.getProductId());
        m.put("productName",    t.getProductName());
        m.put("fromBranchId",   t.getFromBranchId());
        m.put("fromBranchName", t.getFromBranchName());
        m.put("toBranchId",     t.getToBranchId());
        m.put("toBranchName",   t.getToBranchName());
        m.put("quantity",       t.getQuantity());
        m.put("status",         t.getStatus().name());
        m.put("notes",          t.getNotes());
        m.put("createdBy",      t.getCreatedBy());
        m.put("completedBy",    t.getCompletedBy());
        m.put("createdAt",      t.getCreatedAt() != null
                ? t.getCreatedAt().format(DT_FMT) : null);
        m.put("updatedAt",      t.getUpdatedAt() != null
                ? t.getUpdatedAt().format(DT_FMT) : null);
        return m;
    }
}
