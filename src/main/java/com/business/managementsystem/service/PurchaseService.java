package com.business.managementsystem.service;

import com.business.managementsystem.model.*;
import com.business.managementsystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PurchaseService {

    private final PurchaseRepository     purchaseRepo;
    private final PurchaseItemRepository itemRepo;
    private final ProductRepository      productRepo;
    private final BranchInventoryRepository invRepo;
    private final BranchRepository       branchRepo;

    @Lazy
    @Autowired
    private PartyLedgerService partyLedgerService;

    @Autowired
    private SupplierRepository supplierRepository;

    public PurchaseService(PurchaseRepository purchaseRepo,
                           PurchaseItemRepository itemRepo,
                           ProductRepository productRepo,
                           BranchInventoryRepository invRepo,
                           BranchRepository branchRepo) {
        this.purchaseRepo = purchaseRepo;
        this.itemRepo     = itemRepo;
        this.productRepo  = productRepo;
        this.invRepo      = invRepo;
        this.branchRepo   = branchRepo;
    }

    // ── Create purchase ───────────────────────────────────────
    @Transactional
    public Map<String, Object> createPurchase(Long businessId, Long branchId,
                                              Map<String, Object> body) {

        // ── 1. Build the Purchase header ──────────────────────
        Purchase p = new Purchase();
        p.setBusinessId(businessId);
        p.setBranchId(branchId);

        // Supplier
        if (body.get("supplierId") != null)
            p.setSupplierId(toLong(body.get("supplierId")));
        p.setSupplierName(str(body.get("supplierName"), "Unknown"));

        // Purchase date (defaults to now)
        if (body.get("purchaseDate") != null) {
            p.setPurchaseDate(LocalDateTime.parse(body.get("purchaseDate").toString()));
        } else {
            p.setPurchaseDate(LocalDateTime.now());
        }

        // Payment method
        Purchase.PaymentMethod pm = Purchase.PaymentMethod.CASH;
        try { pm = Purchase.PaymentMethod.valueOf(str(body.get("paymentMethod"), "CASH")); }
        catch (IllegalArgumentException ignored) {}
        p.setPaymentMethod(pm);

        // Amounts
        BigDecimal total = bd(body.get("totalAmount"), BigDecimal.ZERO);
        BigDecimal paid  = bd(body.get("amountPaid"),  total); // default: fully paid
        if (pm == Purchase.PaymentMethod.GOLD_EXCHANGE) paid = total; // always fully "paid"

        BigDecimal due = total.subtract(paid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        p.setTotalAmount(total);
        p.setAmountPaid(paid);
        p.setDueAmount(due);
        p.setStatus(deriveStatus(total, paid));

        // Notes
        String notes = str(body.get("notes"), "");
        p.setNotes(notes.isBlank() ? null : notes);

        // Gold Exchange payment details (only meaningful when pm == GOLD_EXCHANGE)
        if (body.get("goldPaymentPurity") != null)
            p.setGoldPaymentPurity(str(body.get("goldPaymentPurity"), null));
        if (body.get("goldPaymentWeightGrams") != null)
            p.setGoldPaymentWeightGrams(toDouble(body.get("goldPaymentWeightGrams")) > 0
                    ? toDouble(body.get("goldPaymentWeightGrams")) : null);
        if (body.get("goldPaymentOzRate") != null)
            p.setGoldPaymentOzRate(bd(body.get("goldPaymentOzRate"), null));
        if (body.get("goldPaymentValue") != null)
            p.setGoldPaymentValue(bd(body.get("goldPaymentValue"), null));
        if (body.get("cashAmountPaid") != null)
            p.setCashAmountPaid(bd(body.get("cashAmountPaid"), null));

        // Exchange rate
        if (body.get("exchangeRate") != null)
            p.setExchangeRate(bd(body.get("exchangeRate"), new BigDecimal("3.6740")));

        p.setCreatedBy(str(body.get("createdBy"), null));

        // ── 2. Generate invoice number ────────────────────────
        String invoiceNumber = generateInvoiceNumber(businessId, branchId, p.getPurchaseDate().toLocalDate());
        p.setInvoiceNumber(invoiceNumber);

        Purchase saved = purchaseRepo.save(p);

        // ── 3. Process items ──────────────────────────────────
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemList =
                (List<Map<String, Object>>) body.getOrDefault("items", Collections.emptyList());

        List<PurchaseItem> savedItems = new ArrayList<>();
        for (Map<String, Object> iBody : itemList) {
            PurchaseItem item = buildItem(iBody, saved.getId(), businessId, branchId);
            savedItems.add(itemRepo.save(item));
        }

        // ── Party ledger: post entry when purchase is linked to a supplier party ──
        if (saved.getSupplierId() != null) {
            supplierRepository.findById(saved.getSupplierId()).ifPresent(party ->
                    partyLedgerService.postPurchaseEntry(saved, party));
        }

        return toResponse(saved, savedItems);
    }

    // ── List purchases ────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getPurchases(Long businessId, Long branchId,
                                            int page, int size,
                                            String dateFrom, String dateTo,
                                            String search) {
        Pageable pageable = PageRequest.of(page, size);
        String q    = (search  != null && !search.isBlank())  ? search  : null;
        LocalDateTime from = dateFrom != null && !dateFrom.isBlank()
                ? LocalDate.parse(dateFrom).atStartOfDay() : null;
        LocalDateTime to   = dateTo   != null && !dateTo.isBlank()
                ? LocalDate.parse(dateTo).plusDays(1).atStartOfDay() : null;

        Page<Purchase> resultPage = (branchId != null)
                ? purchaseRepo.searchPurchasesByBranch(businessId, branchId, q, from, to, pageable)
                : purchaseRepo.searchPurchases(businessId, q, from, to, pageable);

        List<Map<String, Object>> content = new ArrayList<>();
        for (Purchase p : resultPage.getContent()) {
            Map<String, Object> m = toHeaderMap(p);
            m.put("itemCount", itemRepo.countByPurchaseId(p.getId()));
            content.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content",       content);
        result.put("totalElements", resultPage.getTotalElements());
        result.put("totalPages",    resultPage.getTotalPages());
        result.put("page",          page);
        result.put("size",          size);
        return result;
    }

    // ── Get single purchase with items ────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getPurchaseById(Long id) {
        Purchase p = purchaseRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase not found: " + id));
        List<PurchaseItem> items = itemRepo.findByPurchaseIdOrderByIdAsc(id);
        return toResponse(p, items);
    }

    // ── Delete purchase — reverses inventory ──────────────────
    @Transactional
    public void deletePurchase(Long id) {
        Purchase p = purchaseRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase not found: " + id));
        List<PurchaseItem> items = itemRepo.findByPurchaseIdOrderByIdAsc(id);

        for (PurchaseItem item : items) {
            if (item.getProductId() == null) continue;
            // Subtract the quantity that was added on purchase
            double qty = "GRAM".equals(item.getUnitType())
                    ? (item.getWeightGrams() != null ? item.getWeightGrams() : item.getQuantity())
                    : item.getQuantity();
            // Reverse the weight that was added (PCS items only)
            Double weightToRemove = null;
            if ("PCS".equals(item.getUnitType())
                    && item.getWeightGrams() != null && item.getWeightGrams() > 0) {
                weightToRemove = -item.getWeightGrams();
            }
            adjustInventory(p.getBranchId(), item.getProductId(), -qty, weightToRemove);
            syncProductTotalQty(p.getBusinessId(), item.getProductId());
            if (weightToRemove != null) syncProductTotalWeight(p.getBusinessId(), item.getProductId());
        }
        purchaseRepo.delete(p); // cascade deletes items
    }

    // ── Summary stats ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(Long businessId, Long branchId,
                                          String dateFrom, String dateTo) {
        LocalDateTime from = dateFrom != null && !dateFrom.isBlank()
                ? LocalDate.parse(dateFrom).atStartOfDay() : LocalDate.now().atStartOfDay();
        LocalDateTime to   = dateTo   != null && !dateTo.isBlank()
                ? LocalDate.parse(dateTo).plusDays(1).atStartOfDay()
                : LocalDate.now().plusDays(1).atStartOfDay();

        BigDecimal total = purchaseRepo.sumTotalByBranchAndDateRange(businessId, branchId, from, to);
        List<Object[]> byMethod = purchaseRepo.groupByPaymentMethod(businessId, branchId, from, to);

        Map<String, Object> pmBreakdown = new LinkedHashMap<>();
        long totalCount = 0;
        for (Object[] row : byMethod) {
            String method = row[0] != null ? row[0].toString() : "UNKNOWN";
            long count    = ((Number) row[1]).longValue();
            BigDecimal amt = (BigDecimal) row[2];
            totalCount += count;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("count",  count);
            entry.put("amount", amt);
            pmBreakdown.put(method, entry);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSpent",      total);
        result.put("totalPurchases",  totalCount);
        result.put("byPaymentMethod", pmBreakdown);
        return result;
    }

    // ── Private helpers ───────────────────────────────────────

    /** Build and save a PurchaseItem, updating/creating inventory */
    private PurchaseItem buildItem(Map<String, Object> iBody, Long purchaseId,
                                   Long businessId, Long branchId) {
        PurchaseItem item = new PurchaseItem();
        item.setPurchaseId(purchaseId);
        item.setProductName(str(iBody.get("productName"), "Unknown Item"));
        item.setPurity(str(iBody.get("purity"), null));
        item.setUnitType(str(iBody.get("unitType"), "PCS"));
        item.setQuantity(toDouble(iBody.get("quantity")));
        if (iBody.get("weightGrams") != null)
            item.setWeightGrams(toDouble(iBody.get("weightGrams")));
        item.setPricingMethod(str(iBody.get("pricingMethod"), "FIXED_PRICE"));
        item.setGoldOzRate(bd(iBody.get("goldOzRate"), null));
        item.setPricePerGram(bd(iBody.get("pricePerGram"), null));
        item.setUnitPrice(bd(iBody.get("unitPrice"), BigDecimal.ZERO));
        item.setTotalPrice(bd(iBody.get("totalPrice"), BigDecimal.ZERO));
        item.setScrapPurity(str(iBody.get("scrapPurity"), null));

        // Actual inventory quantity to add
        double invQty = "GRAM".equals(item.getUnitType())
                ? (item.getWeightGrams() != null ? item.getWeightGrams() : item.getQuantity())
                : item.getQuantity();

        // For PCS items: also track total weight in branch_inventory
        Double invWeightDelta = null;
        if ("PCS".equals(item.getUnitType())
                && item.getWeightGrams() != null && item.getWeightGrams() > 0) {
            invWeightDelta = item.getWeightGrams();
        }

        Long productId = toLong(iBody.get("productId"));
        if (productId != null) {
            // Existing product → add to THIS branch inventory only
            item.setProductId(productId);
            adjustInventory(branchId, productId, invQty, invWeightDelta);
            syncProductTotalQty(businessId, productId);
            if (invWeightDelta != null) syncProductTotalWeight(businessId, productId);
        } else {
            // New product → create it, then add inventory to all branches
            Product newProduct = createNewProduct(iBody, businessId, branchId, invQty, invWeightDelta);
            item.setProductId(newProduct.getId());
        }

        return item;
    }

    /** Create a new product and seed branch_inventory rows */
    private Product createNewProduct(Map<String, Object> iBody, Long businessId,
                                     Long branchId, double initialQty, Double initialWeight) {
        Product prod = new Product();
        prod.setBusinessId(businessId);
        prod.setName(str(iBody.get("productName"), "New Item"));
        prod.setCategory(str(iBody.get("category"), "Scrap").isBlank() ? "General" : str(iBody.get("category"), "General"));
        prod.setUnitType(str(iBody.get("unitType"), "PCS"));
        prod.setPurity(str(iBody.get("purity"), null));
        prod.setCostPrice(bd(iBody.get("unitPrice"), BigDecimal.ZERO));
        prod.setPrice(bd(iBody.get("unitPrice"), BigDecimal.ZERO));
        prod.setQuantity(initialQty);
        prod.setActive(true);

        boolean isScrap = Boolean.TRUE.equals(iBody.get("isScrap"))
                || "true".equalsIgnoreCase(str(iBody.get("isScrap"), "false"));
        prod.setScrap(isScrap);
        if (isScrap) {
            prod.setCategory("Scrap");
            prod.setUnitType("GRAM");
        }

        // Seed total weight on the product record
        if (initialWeight != null && initialWeight > 0 && !prod.getUnitType().equals("GRAM")) {
            prod.setTotalWeightGrams(initialWeight);
        }

        Product savedProd = productRepo.save(prod);

        // Seed branch_inventory for ALL branches of this business (0 quantity)
        List<Branch> allBranches = branchRepo.findByBusinessIdOrderByIsMainBranchDescNameAsc(businessId);
        for (Branch b : allBranches) {
            double qty = b.getId().equals(branchId) ? initialQty : 0.0;
            boolean isThisBranch = b.getId().equals(branchId);
            invRepo.findByBranchIdAndProductId(b.getId(), savedProd.getId())
                   .ifPresentOrElse(
                       existing -> {
                           double newQty2 = BigDecimal.valueOf(existing.getQuantity())
                                   .add(BigDecimal.valueOf(qty))
                                   .setScale(4, RoundingMode.HALF_UP).doubleValue();
                           existing.setQuantity(newQty2);
                           if (isThisBranch && initialWeight != null && initialWeight > 0) {
                               double cw = existing.getTotalWeightGrams() != null ? existing.getTotalWeightGrams() : 0.0;
                               double newWt = BigDecimal.valueOf(cw)
                                       .add(BigDecimal.valueOf(initialWeight))
                                       .setScale(4, RoundingMode.HALF_UP).doubleValue();
                               existing.setTotalWeightGrams(newWt);
                           }
                           invRepo.save(existing);
                       },
                       () -> {
                           BranchInventory newInv = new BranchInventory(b.getId(), savedProd.getId(), qty);
                           if (isThisBranch && initialWeight != null && initialWeight > 0) {
                               newInv.setTotalWeightGrams(initialWeight);
                           }
                           invRepo.save(newInv);
                       }
                   );
        }

        return savedProd;
    }

    /** Add (or subtract) inventory quantity — and optionally weight — for one branch+product */
    private void adjustInventory(Long branchId, Long productId, double delta, Double weightDelta) {
        BranchInventory inv = invRepo.findByBranchIdAndProductId(branchId, productId)
                .orElse(new BranchInventory(branchId, productId, 0));
        double newQty = BigDecimal.valueOf(inv.getQuantity())
                .add(BigDecimal.valueOf(delta))
                .setScale(4, RoundingMode.HALF_UP).doubleValue();
        inv.setQuantity(Math.max(0, newQty));
        if (weightDelta != null && weightDelta != 0) {
            double cw = inv.getTotalWeightGrams() != null ? inv.getTotalWeightGrams() : 0.0;
            double newWeight = BigDecimal.valueOf(cw)
                    .add(BigDecimal.valueOf(weightDelta))
                    .setScale(4, RoundingMode.HALF_UP).doubleValue();
            inv.setTotalWeightGrams(Math.max(0, newWeight));
        }
        invRepo.save(inv);
    }

    /** Convenience overload — quantity only, no weight change */
    private void adjustInventory(Long branchId, Long productId, double delta) {
        adjustInventory(branchId, productId, delta, null);
    }

    /** Recalculate product.quantity as the sum across all branch_inventory rows */
    private void syncProductTotalQty(Long businessId, Long productId) {
        productRepo.findById(productId).ifPresent(prod -> {
            double total = invRepo.getTotalQuantityAcrossBranches(businessId, productId);
            prod.setQuantity(total);
            productRepo.save(prod);
        });
    }

    /** Recalculate product.totalWeightGrams as the sum across all branch_inventory rows */
    private void syncProductTotalWeight(Long businessId, Long productId) {
        productRepo.findById(productId).ifPresent(prod -> {
            Double total = invRepo.sumWeightAcrossBranches(businessId, productId);
            prod.setTotalWeightGrams(total != null ? total : 0.0);
            productRepo.save(prod);
        });
    }

    /** Generate PUR-{BRANCHCODE}-{YYYYMMDD}-{SEQ:03d} */
    private String generateInvoiceNumber(Long businessId, Long branchId, LocalDate date) {
        // Derive a short branch code (up to 4 uppercase alphanum chars)
        String branchCode = branchRepo.findById(branchId)
                .map(b -> b.getName().toUpperCase().replaceAll("[^A-Z0-9]", ""))
                .map(s -> s.length() > 4 ? s.substring(0, 4) : s)
                .orElse("POS");
        if (branchCode.isBlank()) branchCode = "POS";

        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to   = date.plusDays(1).atStartOfDay();
        long seq = purchaseRepo.countByBranchIdAndDateRange(branchId, from, to) + 1;

        return String.format("PUR-%s-%s-%03d", branchCode, dateStr, seq);
    }

    private static Purchase.Status deriveStatus(BigDecimal total, BigDecimal paid) {
        if (paid == null || paid.compareTo(BigDecimal.ZERO) == 0)
            return Purchase.Status.DUE;
        if (paid.compareTo(total) >= 0)
            return Purchase.Status.FULLY_PAID;
        return Purchase.Status.PARTIALLY_PAID;
    }

    // ── Map converters ────────────────────────────────────────

    public Map<String, Object> toHeaderMap(Purchase p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",            p.getId());
        m.put("businessId",    p.getBusinessId());
        m.put("branchId",      p.getBranchId());
        m.put("supplierId",    p.getSupplierId());
        m.put("supplierName",  p.getSupplierName());
        m.put("invoiceNumber", p.getInvoiceNumber());
        m.put("purchaseDate",  p.getPurchaseDate() != null
                ? p.getPurchaseDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) : null);
        m.put("paymentMethod", p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null);
        m.put("status",        p.getStatus() != null ? p.getStatus().name() : null);
        m.put("totalAmount",   p.getTotalAmount());
        m.put("amountPaid",    p.getAmountPaid());
        m.put("dueAmount",     p.getDueAmount());
        m.put("exchangeRate",  p.getExchangeRate());
        m.put("notes",                  p.getNotes());
        m.put("goldPaymentPurity",      p.getGoldPaymentPurity());
        m.put("goldPaymentWeightGrams", p.getGoldPaymentWeightGrams());
        m.put("goldPaymentOzRate",      p.getGoldPaymentOzRate());
        m.put("goldPaymentValue",       p.getGoldPaymentValue());
        m.put("cashAmountPaid",         p.getCashAmountPaid());
        m.put("createdBy",              p.getCreatedBy());
        m.put("createdAt",     p.getCreatedAt() != null
                ? p.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) : null);
        return m;
    }

    public Map<String, Object> toItemMap(PurchaseItem i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",            i.getId());
        m.put("purchaseId",    i.getPurchaseId());
        m.put("productId",     i.getProductId());
        m.put("productName",   i.getProductName());
        m.put("purity",        i.getPurity());
        m.put("unitType",      i.getUnitType());
        m.put("quantity",      i.getQuantity());
        m.put("weightGrams",   i.getWeightGrams());
        m.put("pricingMethod", i.getPricingMethod());
        m.put("goldOzRate",    i.getGoldOzRate());
        m.put("pricePerGram",  i.getPricePerGram());
        m.put("unitPrice",     i.getUnitPrice());
        m.put("totalPrice",    i.getTotalPrice());
        m.put("scrapPurity",   i.getScrapPurity());
        return m;
    }

    private Map<String, Object> toResponse(Purchase p, List<PurchaseItem> items) {
        Map<String, Object> m = toHeaderMap(p);
        List<Map<String, Object>> itemMaps = new ArrayList<>();
        for (PurchaseItem i : items) itemMaps.add(toItemMap(i));
        m.put("items", itemMaps);
        return m;
    }

    // ── Type helpers ──────────────────────────────────────────
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
