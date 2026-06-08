package com.business.managementsystem.service;

import com.business.managementsystem.model.*;
import com.business.managementsystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PurchaseReturnService {

    private final PurchaseRepository          purchaseRepo;
    private final PurchaseItemRepository      purchaseItemRepo;
    private final PurchaseReturnRepository    returnRepo;
    private final PurchaseReturnItemRepository returnItemRepo;
    private final BranchInventoryRepository   invRepo;
    private final ProductRepository           productRepo;
    private final BranchRepository            branchRepo;

    public PurchaseReturnService(
            PurchaseRepository purchaseRepo,
            PurchaseItemRepository purchaseItemRepo,
            PurchaseReturnRepository returnRepo,
            PurchaseReturnItemRepository returnItemRepo,
            BranchInventoryRepository invRepo,
            ProductRepository productRepo,
            BranchRepository branchRepo) {
        this.purchaseRepo    = purchaseRepo;
        this.purchaseItemRepo = purchaseItemRepo;
        this.returnRepo      = returnRepo;
        this.returnItemRepo  = returnItemRepo;
        this.invRepo         = invRepo;
        this.productRepo     = productRepo;
        this.branchRepo      = branchRepo;
    }

    // ── Lookup purchase invoice for return ─────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> lookupInvoice(Long businessId,
                                             Long branchId,
                                             String invoiceNumber) {
        Purchase purchase = purchaseRepo
                .findByInvoiceNumberAndBusinessId(invoiceNumber.toUpperCase().trim(), businessId)
                .orElseThrow(() -> new RuntimeException(
                        "Purchase invoice not found: " + invoiceNumber));

        List<PurchaseItem> items = purchaseItemRepo
                .findByPurchaseIdOrderByIdAsc(purchase.getId());

        List<Map<String, Object>> itemMaps = new ArrayList<>();
        for (PurchaseItem item : items) {
            double alreadyReturned = returnItemRepo
                    .getTotalReturnedQtyByPurchaseItemId(item.getId());
            double remaining = BigDecimal.valueOf(item.getQuantity())
                    .subtract(BigDecimal.valueOf(alreadyReturned))
                    .setScale(4, RoundingMode.HALF_UP).doubleValue();
            if (remaining <= 0.0001) continue; // fully returned

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("purchaseItemId", item.getId());
            m.put("productId",      item.getProductId());
            m.put("productName",    item.getProductName());
            m.put("purity",         item.getPurity());
            m.put("unitType",       item.getUnitType());
            m.put("quantity",       item.getQuantity());
            m.put("weightGrams",    item.getWeightGrams());
            m.put("unitPrice",      item.getUnitPrice());
            m.put("totalPrice",     item.getTotalPrice());
            m.put("remainingQty",   remaining);
            itemMaps.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("purchaseId",    purchase.getId());
        result.put("invoiceNumber", purchase.getInvoiceNumber());
        result.put("supplierName",  purchase.getSupplierName());
        result.put("purchaseDate",  purchase.getPurchaseDate() != null
                ? purchase.getPurchaseDate()
                          .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : null);
        result.put("totalAmount",   purchase.getTotalAmount());
        result.put("paymentMethod", purchase.getPaymentMethod() != null
                ? purchase.getPaymentMethod().name() : null);
        result.put("createdBy",     purchase.getCreatedBy());
        result.put("items",         itemMaps);
        return result;
    }

    // ── Process return ─────────────────────────────────────────
    @Transactional
    public Map<String, Object> processReturn(Long businessId,
                                             Long branchId,
                                             Map<String, Object> body) {
        Long   purchaseId   = toLong(body.get("purchaseId"));
        String supplierName = str(body.get("supplierName"), "Unknown Supplier");
        String notes        = str(body.get("notes"), null);
        String createdBy    = str(body.get("createdBy"), "System");
        String reasonStr    = str(body.get("reason"), "OTHER");

        PurchaseReturn.Reason reason = PurchaseReturn.Reason.OTHER;
        try { reason = PurchaseReturn.Reason.valueOf(reasonStr); }
        catch (IllegalArgumentException ignored) {}

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemList =
                (List<Map<String, Object>>) body.getOrDefault("items", Collections.emptyList());
        if (itemList.isEmpty())
            throw new RuntimeException("No items provided for return.");

        // ── Build and save the return header ──────────────────
        PurchaseReturn pr = new PurchaseReturn();
        pr.setBusinessId(businessId);
        pr.setBranchId(branchId);
        pr.setPurchaseId(purchaseId);
        pr.setSupplierName(supplierName);
        pr.setReason(reason);
        pr.setNotes(notes);
        pr.setCreatedBy(createdBy);
        pr.setReturnInvoiceNumber(generateInvoiceNumber(branchId));
        pr.setTotalReturnAmount(BigDecimal.ZERO); // updated below

        PurchaseReturn saved = returnRepo.save(pr);

        BigDecimal totalReturnAmount = BigDecimal.ZERO;
        List<Map<String, Object>> savedItemMaps = new ArrayList<>();

        for (Map<String, Object> iBody : itemList) {
            Long   purchaseItemId = toLong(iBody.get("purchaseItemId"));
            Long   productId      = toLong(iBody.get("productId"));
            String productName    = str(iBody.get("productName"), "Unknown");
            String unitType       = str(iBody.get("unitType"), "PCS");
            String purity         = str(iBody.get("purity"), null);
            double returnQty      = toDouble(iBody.get("returnQuantity"));
            Double returnWeight   = iBody.get("returnWeightGrams") != null
                    ? toDouble(iBody.get("returnWeightGrams")) : null;
            BigDecimal unitPrice  = bd(iBody.get("unitPrice"), BigDecimal.ZERO);

            if (returnQty <= 0) continue;

            // ── Validate and compute proportional weight ──────
            if (purchaseItemId != null) {
                double alreadyReturned = returnItemRepo
                        .getTotalReturnedQtyByPurchaseItemId(purchaseItemId);
                PurchaseItem orig = purchaseItemRepo.findById(purchaseItemId).orElse(null);
                if (orig != null) {
                    double remaining = BigDecimal.valueOf(orig.getQuantity())
                            .subtract(BigDecimal.valueOf(alreadyReturned))
                            .setScale(4, RoundingMode.HALF_UP).doubleValue();
                    if (returnQty > remaining + 0.0001) {
                        throw new RuntimeException(
                                "Cannot return " + returnQty + " of \"" + productName +
                                "\" — only " + remaining + " available for return.");
                    }
                    // Auto-calculate proportional weight for PCS items
                    if (returnWeight == null
                            && "PCS".equals(unitType)
                            && orig.getWeightGrams() != null
                            && orig.getWeightGrams() > 0
                            && orig.getQuantity() > 0) {
                        returnWeight = BigDecimal.valueOf(returnQty)
                                .multiply(BigDecimal.valueOf(
                                        orig.getWeightGrams() / orig.getQuantity()))
                                .setScale(4, RoundingMode.HALF_UP).doubleValue();
                    }
                }
            }

            // ── Create return item ────────────────────────────
            BigDecimal lineTotal = unitPrice
                    .multiply(BigDecimal.valueOf(returnQty))
                    .setScale(2, RoundingMode.HALF_UP);

            PurchaseReturnItem ri = new PurchaseReturnItem();
            ri.setPurchaseReturnId(saved.getId());
            ri.setPurchaseItemId(purchaseItemId);
            ri.setProductId(productId);
            ri.setProductName(productName);
            ri.setPurity(purity);
            ri.setUnitType(unitType);
            ri.setReturnQuantity(returnQty);
            ri.setReturnWeightGrams(returnWeight);
            ri.setUnitPrice(unitPrice);
            ri.setTotalReturnAmount(lineTotal);
            returnItemRepo.save(ri);

            totalReturnAmount = totalReturnAmount.add(lineTotal);
            savedItemMaps.add(toItemMap(ri));

            // ── Deduct inventory ──────────────────────────────
            if (productId != null) {
                double invQtyToRemove = "GRAM".equals(unitType)
                        ? (returnWeight != null ? returnWeight : returnQty)
                        : returnQty;
                Double weightToRemove = null;
                if ("PCS".equals(unitType) && returnWeight != null && returnWeight > 0) {
                    weightToRemove = -returnWeight;
                }
                adjustInventory(branchId, productId, -invQtyToRemove, weightToRemove);
                syncProductTotalQty(businessId, productId);
                if (weightToRemove != null) syncProductTotalWeight(businessId, productId);
            }
        }

        // Update total on the return header
        saved.setTotalReturnAmount(totalReturnAmount);
        returnRepo.save(saved);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id",                  saved.getId());
        response.put("returnInvoiceNumber", saved.getReturnInvoiceNumber());
        response.put("purchaseId",          saved.getPurchaseId());
        response.put("supplierName",        saved.getSupplierName());
        response.put("totalReturnAmount",   saved.getTotalReturnAmount());
        response.put("returnedAt",          saved.getReturnedAt() != null
                ? saved.getReturnedAt()
                       .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : null);
        response.put("createdBy",           saved.getCreatedBy());
        response.put("items",               savedItemMaps);
        return response;
    }

    // ── Get all returns (for history table) ────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllReturns(Long businessId, Long branchId) {
        List<PurchaseReturn> returns = branchId != null
                ? returnRepo.findByBusinessIdAndBranchIdOrderByReturnedAtDesc(businessId, branchId)
                : returnRepo.findByBusinessIdOrderByReturnedAtDesc(businessId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (PurchaseReturn r : returns) {
            Map<String, Object> m = toReturnMap(r);
            List<PurchaseReturnItem> items =
                    returnItemRepo.findByPurchaseReturnIdOrderByIdAsc(r.getId());

            // Build a quick product summary (up to 3 names)
            String productsSummary = items.stream()
                    .map(PurchaseReturnItem::getProductName)
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(3)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("—");

            List<Map<String, Object>> itemMapList = new ArrayList<>();
            for (PurchaseReturnItem it : items) { itemMapList.add(toItemMap(it)); }
            m.put("items",           itemMapList);
            m.put("productsSummary", productsSummary);
            m.put("itemCount",       items.size());
            result.add(m);
        }
        return result;
    }

    // ── Summary stats ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(Long businessId, Long branchId) {
        long totalCount = branchId != null
                ? returnRepo.countByBusinessIdAndBranchId(businessId, branchId)
                : returnRepo.countByBusinessId(businessId);

        BigDecimal totalRecovered = branchId != null
                ? returnRepo.getTotalRecoveredByBranch(businessId, branchId)
                : returnRepo.getTotalRecoveredByBusiness(businessId);

        List<PurchaseReturn> returns = branchId != null
                ? returnRepo.findByBusinessIdAndBranchIdOrderByReturnedAtDesc(businessId, branchId)
                : returnRepo.findByBusinessIdOrderByReturnedAtDesc(businessId);

        long totalItems = 0;
        for (PurchaseReturn r : returns) {
            totalItems += returnItemRepo
                    .findByPurchaseReturnIdOrderByIdAsc(r.getId()).size();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalReturns",    totalCount);
        result.put("totalRecovered",  totalRecovered);
        result.put("totalItemsReturned", totalItems);
        return result;
    }

    // ── Invoice number: PRET-{BRANCH}-{YYYYMMDD}-{SEQ:03d} ────
    private String generateInvoiceNumber(Long branchId) {
        String branchCode = branchRepo.findById(branchId)
                .map(b -> b.getName().toUpperCase().replaceAll("[^A-Z0-9]", ""))
                .map(s -> s.length() > 4 ? s.substring(0, 4) : s)
                .orElse("POS");
        if (branchCode.isBlank()) branchCode = "POS";

        LocalDate date   = LocalDate.now();
        String dateStr   = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to   = date.plusDays(1).atStartOfDay();
        long seq = returnRepo.countByBranchAndDateRange(branchId, from, to) + 1;
        return String.format("PRET-%s-%s-%03d", branchCode, dateStr, seq);
    }

    // ── Inventory helpers ──────────────────────────────────────
    private void adjustInventory(Long branchId, Long productId,
                                 double delta, Double weightDelta) {
        BranchInventory inv = invRepo.findByBranchIdAndProductId(branchId, productId)
                .orElse(new BranchInventory(branchId, productId, 0));
        double newQty = BigDecimal.valueOf(inv.getQuantity())
                .add(BigDecimal.valueOf(delta))
                .setScale(4, RoundingMode.HALF_UP).doubleValue();
        inv.setQuantity(Math.max(0, newQty));
        if (weightDelta != null && weightDelta != 0) {
            double cw = inv.getTotalWeightGrams() != null ? inv.getTotalWeightGrams() : 0.0;
            double nw = BigDecimal.valueOf(cw)
                    .add(BigDecimal.valueOf(weightDelta))
                    .setScale(4, RoundingMode.HALF_UP).doubleValue();
            inv.setTotalWeightGrams(Math.max(0, nw));
        }
        invRepo.save(inv);
    }

    private void syncProductTotalQty(Long businessId, Long productId) {
        productRepo.findById(productId).ifPresent(prod -> {
            double total = invRepo.getTotalQuantityAcrossBranches(businessId, productId);
            prod.setQuantity(total);
            productRepo.save(prod);
        });
    }

    private void syncProductTotalWeight(Long businessId, Long productId) {
        productRepo.findById(productId).ifPresent(prod -> {
            Double total = invRepo.sumWeightAcrossBranches(businessId, productId);
            prod.setTotalWeightGrams(total != null ? total : 0.0);
            productRepo.save(prod);
        });
    }

    // ── Map converters ─────────────────────────────────────────
    private Map<String, Object> toReturnMap(PurchaseReturn r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                  r.getId());
        m.put("businessId",          r.getBusinessId());
        m.put("branchId",            r.getBranchId());
        m.put("purchaseId",          r.getPurchaseId());
        m.put("supplierName",        r.getSupplierName());
        m.put("returnInvoiceNumber", r.getReturnInvoiceNumber());
        m.put("totalReturnAmount",   r.getTotalReturnAmount());
        m.put("reason",              r.getReason() != null ? r.getReason().name() : null);
        m.put("notes",               r.getNotes());
        m.put("createdBy",           r.getCreatedBy());
        m.put("returnedAt",          r.getReturnedAt() != null
                ? r.getReturnedAt()
                   .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : null);
        return m;
    }

    private Map<String, Object> toItemMap(PurchaseReturnItem i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                 i.getId());
        m.put("purchaseReturnId",   i.getPurchaseReturnId());
        m.put("purchaseItemId",     i.getPurchaseItemId());
        m.put("productId",          i.getProductId());
        m.put("productName",        i.getProductName());
        m.put("purity",             i.getPurity());
        m.put("unitType",           i.getUnitType());
        m.put("returnQuantity",     i.getReturnQuantity());
        m.put("returnWeightGrams",  i.getReturnWeightGrams());
        m.put("unitPrice",          i.getUnitPrice());
        m.put("totalReturnAmount",  i.getTotalReturnAmount());
        return m;
    }

    // ── Type helpers ───────────────────────────────────────────
    private static String str(Object v, String def) {
        if (v == null) return def;
        String s = v.toString().trim();
        return s.isEmpty() ? def : s;
    }

    private static BigDecimal bd(Object v, BigDecimal def) {
        if (v == null) return def;
        try { return new BigDecimal(v.toString()); }
        catch (NumberFormatException e) { return def; }
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        try { return Long.parseLong(v.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    private static double toDouble(Object v) {
        if (v == null) return 0;
        try { return Double.parseDouble(v.toString()); }
        catch (NumberFormatException e) { return 0; }
    }
}
