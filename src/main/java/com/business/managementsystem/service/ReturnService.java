package com.business.managementsystem.service;

import com.business.managementsystem.model.Branch;
import com.business.managementsystem.model.BranchInventory;
import com.business.managementsystem.model.Product;
import com.business.managementsystem.model.Return;
import com.business.managementsystem.model.Sale;
import com.business.managementsystem.model.SaleTransaction;
import com.business.managementsystem.repository.BranchInventoryRepository;
import com.business.managementsystem.repository.BranchRepository;
import com.business.managementsystem.repository.ProductRepository;
import com.business.managementsystem.repository.ReturnRepository;
import com.business.managementsystem.repository.SaleRepository;
import com.business.managementsystem.repository.SaleTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReturnService {

    private final ReturnRepository          returnRepository;
    private final SaleRepository            saleRepository;
    private final SaleTransactionRepository txRepository;
    private final ProductRepository         productRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final BranchRepository          branchRepository;

    public ReturnService(ReturnRepository returnRepository,
                         SaleRepository saleRepository,
                         SaleTransactionRepository txRepository,
                         ProductRepository productRepository,
                         BranchInventoryRepository branchInventoryRepository,
                         BranchRepository branchRepository) {
        this.returnRepository          = returnRepository;
        this.saleRepository            = saleRepository;
        this.txRepository              = txRepository;
        this.productRepository         = productRepository;
        this.branchInventoryRepository = branchInventoryRepository;
        this.branchRepository          = branchRepository;
    }

    // ── Look up a transaction by receipt number ───────────────────
    // Global — can look up any receipt from any branch.
    // Returns remaining qty per item so UI can enforce return limits.
    @Transactional(readOnly = true)
    public Map<String, Object> lookupReceipt(String receiptNumber,
                                             Long businessId) {
        SaleTransaction tx = txRepository
                .findByReceiptNumber(receiptNumber)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Receipt not found: " + receiptNumber));

        if (!tx.getBusinessId().equals(businessId))
            throw new RuntimeException("Unauthorized.");

        List<Sale> sales = saleRepository.findByTransactionId(tx.getId());

        List<Map<String, Object>> items = new ArrayList<>();
        for (Sale s : sales) {
            double alreadyReturned = returnRepository
                    .getTotalReturnedQtyBySaleId(s.getId());
            double remaining = s.getQuantitySold() - alreadyReturned;
            if (remaining <= 0) continue; // fully returned — skip

            Map<String, Object> item = new HashMap<>();
            item.put("saleId",           s.getId());
            item.put("productId",        s.getProduct().getId());
            item.put("productName",      s.getProduct().getName());
            item.put("quantitySold",     s.getQuantitySold());
            item.put("remainingQty",     remaining);
            item.put("priceAtSale",      s.getPriceAtSale());
            item.put("totalAmount",      s.getTotalAmount());
            items.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("transactionId",  tx.getId());
        result.put("receiptNumber",  tx.getReceiptNumber());
        result.put("branchId",       tx.getBranchId());
        result.put("cashierName",    tx.getCashierName());
        result.put("customerName",   tx.getCustomerName());
        result.put("paymentMethod",  tx.getPaymentMethod());
        result.put("subtotal",       tx.getSubtotal());
        result.put("discountAmount", tx.getDiscountAmount() != null ? tx.getDiscountAmount() : java.math.BigDecimal.ZERO);
        result.put("vatPercent",     tx.getVatPercent());
        result.put("vatAmount",      tx.getVatAmount());
        result.put("totalAmount",    tx.getTotalAmount());
        result.put("amountReceived", tx.getAmountReceived());
        result.put("createdAt",      tx.getCreatedAt() != null
                ? tx.getCreatedAt().toString() : null);
        result.put("items",          items);
        return result;
    }

    // ── Process a return by saleId — branch-aware ─────────────────
    // branchId = the branch where this return is being processed.
    // Stock is returned to the original sale's branch (sale.branchId),
    // but the return record is tagged with the processing branch.
    @Transactional
    public Map<String, Object> processReturn(Long businessId,
                                             Long branchId,
                                             Long saleId,
                                             double quantityReturned,
                                             Return.Reason reason,
                                             String notes,
                                             String processedBy) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() ->
                        new RuntimeException("Sale not found."));

        if (!sale.getBusinessId().equals(businessId))
            throw new RuntimeException("Unauthorized.");

        return doReturn(businessId, branchId, sale, quantityReturned,
                reason, notes, processedBy);
    }

    // ── Internal: apply the return and record it ──────────────────
    private Map<String, Object> doReturn(Long businessId,
                                         Long branchId,
                                         Sale sale,
                                         double quantityReturned,
                                         Return.Reason reason,
                                         String notes,
                                         String processedBy) {
        if (quantityReturned <= 0)
            throw new RuntimeException("Quantity returned must be at least 1.");

        // Remaining qty validation — prevents over-returning
        double alreadyReturned = returnRepository
                .getTotalReturnedQtyBySaleId(sale.getId());
        double remaining = sale.getQuantitySold() - alreadyReturned;

        if (quantityReturned > remaining)
            throw new RuntimeException(
                    "Cannot return " + quantityReturned +
                            " units — only " + remaining + " remain returnable.");

        // Refund = proportional line amount − proportional discount + VAT
        BigDecimal lineSub = sale.getPriceAtSale()
                .multiply(BigDecimal.valueOf(quantityReturned))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal refundAmount;
        SaleTransaction txCtx = sale.getTransactionId() != null
                ? txRepository.findById(sale.getTransactionId()).orElse(null)
                : null;

        if (txCtx != null && txCtx.getSubtotal() != null
                && txCtx.getSubtotal().compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal txSubtotal = txCtx.getSubtotal();
            BigDecimal txDiscount = txCtx.getDiscountAmount() != null
                    ? txCtx.getDiscountAmount() : BigDecimal.ZERO;

            // Each returned item bears its proportional share of the transaction discount
            BigDecimal propDiscount = txDiscount.compareTo(BigDecimal.ZERO) > 0
                    ? lineSub.multiply(txDiscount)
                             .divide(txSubtotal, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal netBeforeVat = lineSub.subtract(propDiscount);

            // VAT is refunded on the net-after-discount amount
            BigDecimal vatFactor = BigDecimal.valueOf(txCtx.getVatPercent())
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            BigDecimal vatOnReturn = netBeforeVat.multiply(vatFactor);

            refundAmount = netBeforeVat.add(vatOnReturn).setScale(2, RoundingMode.HALF_UP);
        } else {
            // Fallback — no transaction context available (legacy data)
            refundAmount = lineSub;
        }

        Product product = productRepository
                .findById(sale.getProduct().getId())
                .orElseThrow(() ->
                        new RuntimeException("Product not found."));

        // Stock always returns to the branch where the original sale was made
        Long stockBranchId = sale.getBranchId();

        if (stockBranchId != null) {
            BranchInventory inv = branchInventoryRepository
                    .findByBranchIdAndProductId(stockBranchId, product.getId())
                    .orElse(new BranchInventory(stockBranchId, product.getId(), 0));
            inv.setQuantity(inv.getQuantity() + quantityReturned);
            branchInventoryRepository.save(inv);

            double total = branchInventoryRepository
                    .getTotalQuantityAcrossBranches(businessId, product.getId());
            product.setQuantity(total);
            productRepository.save(product);
        } else {
            product.setQuantity(product.getQuantity() + quantityReturned);
            productRepository.save(product);
        }

        // Save return — tagged with the branch that processed it
        Return ret = new Return(
                businessId, branchId, sale.getId(),
                product.getId(), product.getName(),
                quantityReturned, refundAmount,
                reason, notes, processedBy);
        ret.setTransactionId(sale.getTransactionId());
        Return saved = returnRepository.save(ret);

        // Generate invoice number using saved ID as unique sequence
        int year = LocalDateTime.now().getYear();
        String branchCode = branchId != null
                ? branchRepository.findById(branchId)
                    .map(b -> deriveBranchCode(b.getName())).orElse("BRN")
                : "GEN";
        saved.setInvoiceNumber(
                String.format("RET-%s-%d-%05d", branchCode, year, saved.getId()));
        saved = returnRepository.save(saved);

        // Update the parent transaction's status to reflect returned items
        if (txCtx != null) {
            List<Sale> allSales = saleRepository.findByTransactionId(sale.getTransactionId());
            boolean allFullyReturned = allSales.stream().allMatch(s ->
                    returnRepository.getTotalReturnedQtyBySaleId(s.getId()) >= s.getQuantitySold());
            txCtx.setStatus(allFullyReturned ? "RETURNED" : "PARTIALLY_RETURNED");
            txRepository.save(txCtx);
        }

        return toMap(saved);
    }

    // ── Get all returns — branch-aware ────────────────────────────
    // branchId present → only returns processed at that branch
    // branchId null    → all returns (owner global view)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllReturns(Long businessId,
                                                   Long branchId) {
        if (branchId != null) {
            return returnRepository
                    .findByBusinessIdAndBranchIdOrderByReturnedAtDesc(
                            businessId, branchId)
                    .stream()
                    .map(this::toMap)
                    .toList();
        }
        return returnRepository
                .findByBusinessIdOrderByReturnedAtDesc(businessId)
                .stream()
                .map(this::toMap)
                .toList();
    }

    // ── Get returns for a specific sale ───────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getReturnsBySale(Long saleId) {
        return returnRepository.findBySaleId(saleId)
                .stream()
                .map(this::toMap)
                .toList();
    }

    // ── Summary stats — branch-aware ──────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(Long businessId, Long branchId) {
        Map<String, Object> result = new HashMap<>();

        if (branchId != null) {
            long count = returnRepository
                    .countByBusinessIdAndBranchId(businessId, branchId);
            BigDecimal totalRefunds = returnRepository
                    .getTotalRefundsByBusinessAndBranch(businessId, branchId);
            Double totalUnits = returnRepository
                    .getTotalUnitsReturnedByBusinessAndBranch(businessId, branchId);

            result.put("totalReturns",      count);
            result.put("totalRefunds",      totalRefunds != null ? totalRefunds : BigDecimal.ZERO);
            result.put("totalUnitsReturned", totalUnits != null ? totalUnits : 0.0);
        } else {
            long count = returnRepository.countByBusinessId(businessId);
            BigDecimal totalRefunds = returnRepository
                    .getTotalRefundsByBusiness(businessId);
            Double totalUnits = returnRepository
                    .getTotalUnitsReturnedByBusiness(businessId);

            result.put("totalReturns",      count);
            result.put("totalRefunds",      totalRefunds != null ? totalRefunds : BigDecimal.ZERO);
            result.put("totalUnitsReturned", totalUnits != null ? totalUnits : 0.0);
        }

        return result;
    }

    // ── Paginated return list with optional search + date range ──
    @Transactional(readOnly = true)
    public Map<String, Object> getReturnList(Long businessId, Long branchId,
                                              int page, int size,
                                              String search,
                                              String dateFrom, String dateTo) {
        LocalDateTime from = null;
        LocalDateTime to   = null;
        if (dateFrom != null && !dateFrom.isBlank())
            from = LocalDate.parse(dateFrom).atStartOfDay();
        if (dateTo != null && !dateTo.isBlank())
            to = LocalDate.parse(dateTo).plusDays(1).atStartOfDay();

        String q = (search != null && !search.isBlank()) ? search : null;

        Page<Return> retPage = branchId != null
                ? returnRepository.searchReturnsByBranch(businessId, branchId, q, from, to, PageRequest.of(page, size))
                : returnRepository.searchReturns(businessId, q, from, to, PageRequest.of(page, size));

        List<Map<String, Object>> content = retPage.getContent()
                .stream().map(this::toMap).collect(java.util.stream.Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content",       content);
        result.put("totalElements", retPage.getTotalElements());
        result.put("totalPages",    retPage.getTotalPages());
        result.put("page",          page);
        result.put("size",          size);
        return result;
    }

    // ── Convert to safe map ───────────────────────────────────────
    private Map<String, Object> toMap(Return r) {
        String receiptNumber = saleRepository.findById(r.getSaleId())
                .map(Sale::getReceiptNumber)
                .orElse(null);

        Map<String, Object> m = new HashMap<>();
        m.put("id",               r.getId());
        m.put("invoiceNumber",    r.getInvoiceNumber());
        m.put("transactionId",    r.getTransactionId());
        m.put("branchId",         r.getBranchId());
        m.put("saleId",           r.getSaleId());
        m.put("receiptNumber",    receiptNumber);
        m.put("productId",        r.getProductId());
        m.put("productName",      r.getProductName());
        m.put("quantityReturned", r.getQuantityReturned());
        m.put("refundAmount",     r.getRefundAmount());
        m.put("reason",           r.getReason().name());
        m.put("notes",            r.getNotes());
        m.put("processedBy",      r.getProcessedBy());
        m.put("returnedAt",       r.getReturnedAt() != null
                ? r.getReturnedAt().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : null);
        return m;
    }

    private String deriveBranchCode(String branchName) {
        if (branchName == null || branchName.isBlank()) return "BRN";
        String clean = branchName.replaceAll("[^a-zA-Z ]", "").trim();
        String[] words = clean.split("\\s+");
        if (words.length >= 3) {
            return (words[0].charAt(0) + "" + words[1].charAt(0) + "" + words[2].charAt(0)).toUpperCase();
        } else if (words.length == 2) {
            String w1 = words[0].length() >= 2 ? words[0].substring(0, 2) : words[0];
            return (w1 + words[1].charAt(0)).toUpperCase();
        } else {
            String w = words[0];
            return w.length() >= 3 ? w.substring(0, 3).toUpperCase() : w.toUpperCase();
        }
    }
}