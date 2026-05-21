package com.business.managementsystem.service;

import com.business.managementsystem.model.BranchInventory;
import com.business.managementsystem.model.Business;
import com.business.managementsystem.model.Product;
import com.business.managementsystem.model.PurchaseOrder;
import com.business.managementsystem.model.Supplier;
import com.business.managementsystem.repository.BranchInventoryRepository;
import com.business.managementsystem.repository.BusinessRepository;
import com.business.managementsystem.repository.ProductRepository;
import com.business.managementsystem.repository.PurchaseOrderRepository;
import com.business.managementsystem.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository   poRepository;
    private final ProductRepository         productRepository;
    private final SupplierRepository        supplierRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final BusinessRepository        businessRepository;
    private final EmailService              emailService;

    public PurchaseOrderService(PurchaseOrderRepository poRepository,
                                ProductRepository productRepository,
                                SupplierRepository supplierRepository,
                                BranchInventoryRepository branchInventoryRepository,
                                BusinessRepository businessRepository,
                                EmailService emailService) {
        this.poRepository              = poRepository;
        this.productRepository         = productRepository;
        this.supplierRepository        = supplierRepository;
        this.branchInventoryRepository = branchInventoryRepository;
        this.businessRepository        = businessRepository;
        this.emailService              = emailService;
    }

    // ── Get all purchase orders — branch-aware ────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllOrders(Long businessId, Long branchId) {
        if (branchId != null) {
            return poRepository
                    .findByBusinessIdAndBranchIdOrderByCreatedAtDesc(businessId, branchId)
                    .stream().map(this::toMap).toList();
        }
        return poRepository
                .findByBusinessIdOrderByCreatedAtDesc(businessId)
                .stream().map(this::toMap).toList();
    }

    // ── Get by status — branch-aware ──────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOrdersByStatus(Long businessId, Long branchId,
                                                       PurchaseOrder.Status status) {
        if (branchId != null) {
            return poRepository
                    .findByBusinessIdAndBranchIdAndStatus(businessId, branchId, status)
                    .stream().map(this::toMap).toList();
        }
        return poRepository
                .findByBusinessIdAndStatus(businessId, status)
                .stream().map(this::toMap).toList();
    }

    // ── Create purchase order + send email to supplier ─────────
    @Transactional
    public Map<String, Object> createOrder(Long businessId,
                                           Long supplierId,
                                           Long productId,
                                           double quantityOrdered,
                                           BigDecimal unitCost,
                                           String expectedDelivery,
                                           String notes,
                                           String createdBy,
                                           Long branchId) {

        Supplier supplier = supplierRepository.findByIdAndBusinessId(supplierId, businessId)
                .orElseThrow(() -> new RuntimeException("Supplier not found."));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found."));

        if (!product.getBusinessId().equals(businessId))
            throw new RuntimeException("Unauthorized.");

        if (quantityOrdered <= 0)
            throw new RuntimeException("Quantity must be greater than zero.");

        PurchaseOrder po = new PurchaseOrder();
        po.setBusinessId(businessId);
        po.setSupplierId(supplierId);
        po.setSupplierName(supplier.getName());
        po.setProductId(productId);
        po.setProductName(product.getName());
        po.setQuantityOrdered(quantityOrdered);
        po.setStatus(PurchaseOrder.Status.PENDING);
        po.setCreatedBy(createdBy);
        po.setNotes(notes);
        if (branchId != null) po.setBranchId(branchId);

        if (unitCost != null && unitCost.compareTo(BigDecimal.ZERO) > 0) {
            po.setUnitCost(unitCost);
            po.setTotalCost(unitCost.multiply(BigDecimal.valueOf(quantityOrdered)));
        }

        if (expectedDelivery != null && !expectedDelivery.isBlank()) {
            po.setExpectedDelivery(LocalDate.parse(expectedDelivery));
        }

        PurchaseOrder saved = poRepository.save(po);

        // ── Send email to supplier ────────────────────────────────
        // Get business name for the email
        String businessName = "SmartPOS Client";
        try {
            businessName = businessRepository.findById(businessId)
                    .map(Business::getBusinessName).orElse("SmartPOS Client");
        } catch (Exception ignored) {}

        // Send email asynchronously (non-blocking — failure doesn't break the order)
        try {
            emailService.sendPurchaseOrderEmail(
                    supplier.getEmail(),
                    supplier.getName(),
                    businessName,
                    product.getName(),
                    quantityOrdered,
                    unitCost,
                    saved.getTotalCost(),
                    expectedDelivery,
                    notes,
                    createdBy
            );
        } catch (Exception e) {
            System.err.println("Email sending failed (non-fatal): " + e.getMessage());
        }

        return toMap(saved);
    }

    // Legacy overload — no branch context
    @Transactional
    public Map<String, Object> createOrder(Long businessId,
                                           Long supplierId,
                                           Long productId,
                                           double quantityOrdered,
                                           BigDecimal unitCost,
                                           String expectedDelivery,
                                           String notes,
                                           String createdBy) {
        return createOrder(businessId, supplierId, productId,
                quantityOrdered, unitCost, expectedDelivery,
                notes, createdBy, null);
    }

    // ── Mark as received — branch-aware stock addition ────────
    @Transactional
    public Map<String, Object> markReceived(Long id, Long businessId,
                                            double quantityReceived, String notes) {
        PurchaseOrder po = poRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found."));

        if (po.getStatus() != PurchaseOrder.Status.PENDING && po.getStatus() != PurchaseOrder.Status.DRAFT)
            throw new RuntimeException("Only PENDING or DRAFT orders can be received.");

        if (quantityReceived <= 0)
            throw new RuntimeException("Quantity received must be greater than zero.");

        Product product = productRepository.findById(po.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found."));

        Long branchId = po.getBranchId();

        if (branchId != null) {
            BranchInventory inv = branchInventoryRepository
                    .findByBranchIdAndProductId(branchId, po.getProductId())
                    .orElse(new BranchInventory(branchId, po.getProductId(), 0));
            inv.setQuantity(inv.getQuantity() + quantityReceived);
            branchInventoryRepository.save(inv);
            double total = branchInventoryRepository
                    .getTotalQuantityAcrossBranches(businessId, po.getProductId());
            product.setQuantity(total);
            productRepository.save(product);
        } else {
            product.setQuantity(product.getQuantity() + quantityReceived);
            productRepository.save(product);
        }

        po.setStatus(PurchaseOrder.Status.RECEIVED);
        po.setQuantityReceived(quantityReceived);
        po.setReceivedDate(LocalDate.now());
        if (notes != null && !notes.isBlank()) {
            po.setNotes(po.getNotes() != null ? po.getNotes() + " | " + notes : notes);
        }

        return toMap(poRepository.save(po));
    }

    // ── Cancel purchase order ─────────────────────────────────
    @Transactional
    public Map<String, Object> cancelOrder(Long id, Long businessId) {
        PurchaseOrder po = poRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found."));

        if (po.getStatus() != PurchaseOrder.Status.PENDING && po.getStatus() != PurchaseOrder.Status.DRAFT)
            throw new RuntimeException("Only PENDING or DRAFT orders can be cancelled.");

        po.setStatus(PurchaseOrder.Status.CANCELLED);
        return toMap(poRepository.save(po));
    }

    // ── Summary stats — branch-aware ──────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(Long businessId, Long branchId) {
        Map<String, Object> result = new HashMap<>();

        if (branchId != null) {
            result.put("total",     poRepository.countByBusinessIdAndBranchId(businessId, branchId));
            result.put("draft",     poRepository.countByBusinessIdAndBranchIdAndStatus(businessId, branchId, PurchaseOrder.Status.DRAFT));
            result.put("pending",   poRepository.countByBusinessIdAndBranchIdAndStatus(businessId, branchId, PurchaseOrder.Status.PENDING));
            result.put("received",  poRepository.countByBusinessIdAndBranchIdAndStatus(businessId, branchId, PurchaseOrder.Status.RECEIVED));
            result.put("cancelled", poRepository.countByBusinessIdAndBranchIdAndStatus(businessId, branchId, PurchaseOrder.Status.CANCELLED));
            BigDecimal spent = poRepository.getTotalSpentByBusinessAndBranch(businessId, branchId);
            result.put("totalSpent", spent != null ? spent : BigDecimal.ZERO);
        } else {
            result.put("total",     poRepository.countByBusinessId(businessId));
            result.put("draft",     poRepository.countByBusinessIdAndStatus(businessId, PurchaseOrder.Status.DRAFT));
            result.put("pending",   poRepository.countByBusinessIdAndStatus(businessId, PurchaseOrder.Status.PENDING));
            result.put("received",  poRepository.countByBusinessIdAndStatus(businessId, PurchaseOrder.Status.RECEIVED));
            result.put("cancelled", poRepository.countByBusinessIdAndStatus(businessId, PurchaseOrder.Status.CANCELLED));
            BigDecimal spent = poRepository.getTotalSpentByBusiness(businessId);
            result.put("totalSpent", spent != null ? spent : BigDecimal.ZERO);
        }

        return result;
    }

    // ── Approve Draft ─────────────────────────────────────────
    @Transactional
    public Map<String, Object> approveDraft(Long id, Long businessId) {
        PurchaseOrder po = poRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found."));

        if (po.getStatus() != PurchaseOrder.Status.DRAFT)
            throw new RuntimeException("Only DRAFT orders can be approved.");

        po.setStatus(PurchaseOrder.Status.PENDING);
        PurchaseOrder saved = poRepository.save(po);

        // Fetch supplier info for email
        Supplier supplier = supplierRepository.findById(po.getSupplierId()).orElse(null);
        if (supplier != null) {
            String businessName = businessRepository.findById(businessId)
                    .map(Business::getBusinessName).orElse("SmartPOS Client");
            
            try {
                emailService.sendPurchaseOrderEmail(
                        supplier.getEmail(),
                        supplier.getName(),
                        businessName,
                        po.getProductName(),
                        po.getQuantityOrdered(),
                        po.getUnitCost(),
                        po.getTotalCost(),
                        po.getExpectedDelivery() != null ? po.getExpectedDelivery().toString() : null,
                        po.getNotes(),
                        po.getCreatedBy()
                );
            } catch (Exception e) {
                System.err.println("Email sending failed (non-fatal): " + e.getMessage());
            }
        }

        return toMap(saved);
    }

    // ── Convert to map ────────────────────────────────────────
    private Map<String, Object> toMap(PurchaseOrder po) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        DateTimeFormatter df  = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Map<String, Object> m = new HashMap<>();
        m.put("id",               po.getId());
        m.put("branchId",         po.getBranchId());
        m.put("supplierId",       po.getSupplierId());
        m.put("supplierName",     po.getSupplierName());
        m.put("productId",        po.getProductId());
        m.put("productName",      po.getProductName());
        m.put("quantityOrdered",  po.getQuantityOrdered());
        m.put("quantityReceived", po.getQuantityReceived());
        m.put("unitCost",         po.getUnitCost());
        m.put("totalCost",        po.getTotalCost());
        m.put("status",           po.getStatus().name());
        m.put("notes",            po.getNotes());
        m.put("createdBy",        po.getCreatedBy());
        m.put("expectedDelivery", po.getExpectedDelivery() != null
                ? po.getExpectedDelivery().format(df) : null);
        m.put("receivedDate",     po.getReceivedDate() != null
                ? po.getReceivedDate().format(df) : null);
        m.put("createdAt",        po.getCreatedAt() != null
                ? po.getCreatedAt().format(dtf) : null);
        m.put("updatedAt",        po.getUpdatedAt() != null
                ? po.getUpdatedAt().format(dtf) : null);
        return m;
    }
}