package com.business.managementsystem.service;

import com.business.managementsystem.dto.SaleDTO;
import com.business.managementsystem.model.Branch;
import com.business.managementsystem.model.BranchInventory;
import com.business.managementsystem.model.Product;
import com.business.managementsystem.model.ReceiptSequence;
import com.business.managementsystem.model.Sale;
import com.business.managementsystem.model.SaleTransaction;
import com.business.managementsystem.repository.BranchInventoryRepository;
import com.business.managementsystem.repository.BranchRepository;
import com.business.managementsystem.repository.ProductRepository;
import com.business.managementsystem.repository.ReceiptSequenceRepository;
import com.business.managementsystem.model.Customer;
import com.business.managementsystem.model.Supplier;
import com.business.managementsystem.repository.CustomerRepository;
import com.business.managementsystem.repository.ReturnRepository;
import com.business.managementsystem.repository.SaleRepository;
import com.business.managementsystem.repository.SaleTransactionRepository;
import com.business.managementsystem.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Service
public class SaleService {

    private final SaleRepository            saleRepository;
    private final SaleTransactionRepository txRepository;
    private final ProductRepository         productRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final BranchRepository          branchRepository;
    private final ReceiptSequenceRepository receiptSequenceRepository;
    private final ReturnRepository          returnRepository;

    @Lazy
    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private LoyaltyService loyaltyService;

    @Lazy
    @Autowired
    private PartyLedgerService partyLedgerService;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CustomerRepository customerRepository;

    public SaleService(SaleRepository saleRepository,
                       SaleTransactionRepository txRepository,
                       ProductRepository productRepository,
                       BranchInventoryRepository branchInventoryRepository,
                       BranchRepository branchRepository,
                       ReceiptSequenceRepository receiptSequenceRepository,
                       ReturnRepository returnRepository) {
        this.saleRepository            = saleRepository;
        this.txRepository              = txRepository;
        this.productRepository         = productRepository;
        this.branchInventoryRepository = branchInventoryRepository;
        this.branchRepository          = branchRepository;
        this.receiptSequenceRepository = receiptSequenceRepository;
        this.returnRepository          = returnRepository;
    }

    // ── Paginated transaction list with search + date filter ───
    @Transactional(readOnly = true)
    public Map<String, Object> getTransactionList(Long businessId, Long branchId,
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

        Page<SaleTransaction> txPage = branchId != null
                ? txRepository.searchTransactionsByBranch(businessId, branchId, q, from, to, PageRequest.of(page, size))
                : txRepository.searchTransactions(businessId, q, from, to, PageRequest.of(page, size));

        List<Map<String, Object>> content = txPage.getContent()
                .stream().map(this::txToReceiptMap).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content",       content);
        result.put("totalElements", txPage.getTotalElements());
        result.put("totalPages",    txPage.getTotalPages());
        result.put("page",          page);
        result.put("size",          size);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllTransactions(Long businessId, Long branchId) {
        List<SaleTransaction> txList = branchId != null
                ? txRepository.findByBusinessIdAndBranchIdOrderByCreatedAtDesc(businessId, branchId)
                : txRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);
        return txList.stream().map(this::txToReceiptMap).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> recordTransaction(Long businessId, Long branchId,
                                                 List<Map<String, Object>> cartItems,
                                                 String paymentMethod,
                                                 BigDecimal amountReceived,
                                                 String cashierName,
                                                 Long customerId,
                                                 String customerName,
                                                 BigDecimal discountAmount,
                                                 double vatPercent,
                                                 BigDecimal exchangeRate,
                                                 String pricingMethod,
                                                 BigDecimal goldOzRate) {
        if (cartItems == null || cartItems.isEmpty())
            throw new RuntimeException("Cart is empty.");

        BigDecimal subtotal = BigDecimal.ZERO;
        List<Sale> sales = new ArrayList<>();

        for (Map<String, Object> item : cartItems) {
            Long productId = Long.parseLong(item.get("productId").toString());
            double quantity = Double.parseDouble(item.get("quantity").toString());

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

            if (branchId != null) {
                BranchInventory inv = branchInventoryRepository
                        .findByBranchIdAndProductId(branchId, productId)
                        .orElseThrow(() -> new RuntimeException(
                                product.getName() + " is not available at this branch."));
                if (inv.getQuantity() < quantity)
                    throw new RuntimeException("Insufficient stock for " + product.getName() +
                            ". Available: " + inv.getQuantity());
                inv.setQuantity(BigDecimal.valueOf(inv.getQuantity())
                        .subtract(BigDecimal.valueOf(quantity))
                        .setScale(4, RoundingMode.HALF_UP).doubleValue());
                branchInventoryRepository.save(inv);
                double total = branchInventoryRepository.getTotalQuantityAcrossBranches(businessId, productId);
                product.setQuantity(total);
                productRepository.save(product);
            } else {
                if (product.getQuantity() < quantity)
                    throw new RuntimeException("Insufficient stock for " + product.getName() +
                            ". Available: " + product.getQuantity());
                product.setQuantity(BigDecimal.valueOf(product.getQuantity())
                        .subtract(BigDecimal.valueOf(quantity))
                        .setScale(4, RoundingMode.HALF_UP).doubleValue());
                productRepository.save(product);
            }

            BigDecimal unitPrice = product.getPrice();
            if (item.containsKey("price") && item.get("price") != null) {
                unitPrice = new BigDecimal(item.get("price").toString());
            }

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            subtotal = subtotal.add(lineTotal);

            Sale sale = new Sale(businessId, product, quantity, unitPrice);
            sale.setBranchId(branchId);
            if (item.containsKey("scrapPurity") && item.get("scrapPurity") != null) {
                sale.setScrapPurity(item.get("scrapPurity").toString());
            }
            sales.add(sale);
        }

        if (discountAmount == null) discountAmount = BigDecimal.ZERO;
        BigDecimal discountedSubtotal = subtotal.subtract(discountAmount);
        if (discountedSubtotal.compareTo(BigDecimal.ZERO) < 0) discountedSubtotal = BigDecimal.ZERO;

        BigDecimal vatRate = BigDecimal.valueOf(vatPercent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal vatAmount = discountedSubtotal.multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = discountedSubtotal.add(vatAmount);

        BigDecimal change = BigDecimal.ZERO;
        BigDecimal dueAmount = totalAmount;
        String status = "PARTIALLY_PAID";

        if (amountReceived != null) {
            if (amountReceived.compareTo(totalAmount) >= 0) {
                change = amountReceived.subtract(totalAmount);
                dueAmount = BigDecimal.ZERO;
                status = "FULLY_PAID";
            } else {
                dueAmount = totalAmount.subtract(amountReceived);
            }
        } else {
            // No payment received yet
            dueAmount = totalAmount;
        }

        String receiptNumber = branchId != null ? generateReceiptNumber(branchId) : null;

        SaleTransaction tx = new SaleTransaction();
        tx.setBusinessId(businessId);
        tx.setBranchId(branchId);
        tx.setReceiptNumber(receiptNumber);
        tx.setCashierName(cashierName);
        tx.setPaymentMethod(paymentMethod);
        tx.setSubtotal(subtotal);
        tx.setDiscountAmount(discountAmount);
        tx.setVatPercent(vatPercent);
        tx.setVatAmount(vatAmount);
        tx.setTotalAmount(totalAmount);
        tx.setAmountReceived(amountReceived);
        tx.setChangeGiven(change);
        tx.setDueAmount(dueAmount);
        tx.setStatus(status);
        tx.setExchangeRate(exchangeRate);
        tx.setPricingMethod(pricingMethod);
        tx.setGoldOzRate(goldOzRate);
        tx.setCustomerId(customerId);
        tx.setCustomerName(customerName);
        SaleTransaction savedTx = txRepository.save(tx);

        if (customerId != null) {
            loyaltyService.processLoyaltyForTransaction(savedTx);
        }

        List<Map<String, Object>> saleItems = new ArrayList<>();
        for (Sale sale : sales) {
            sale.setTransactionId(savedTx.getId());
            sale.setReceiptNumber(receiptNumber);
            sale.setCashierName(cashierName);
            sale.setPaymentMethod(paymentMethod);
            Sale saved = saleRepository.save(sale);
            saleItems.add(saleItemToMap(saved));
        }

        // ── Party ledger: post entry when sale is linked to a party ──
        if (customerId != null) {
            resolvePartyForSale(customerId, businessId).ifPresent(party ->
                    partyLedgerService.postSaleEntry(savedTx, party));
        }

        String branchName = branchId != null
                ? branchRepository.findById(branchId).map(Branch::getName).orElse(null) : null;

        Map<String, Object> response = new HashMap<>();
        response.put("transactionId", savedTx.getId());
        response.put("receiptNumber", receiptNumber);
        response.put("branchId", branchId);
        response.put("branchName", branchName);
        response.put("cashierName", cashierName);
        response.put("paymentMethod", paymentMethod);
        response.put("subtotal", subtotal);
        response.put("discountAmount", discountAmount);
        response.put("vatPercent", vatPercent);
        response.put("vatAmount", vatAmount);
        response.put("totalAmount", totalAmount);
        response.put("amountReceived", amountReceived);
        response.put("changeGiven", change);
        response.put("dueAmount", dueAmount);
        response.put("status", status);
        response.put("customerId", customerId);
        response.put("customerName", customerName);
        response.put("items", saleItems);
        response.put("createdAt", savedTx.getCreatedAt() != null ? savedTx.getCreatedAt().toString() : null);
        return response;
    }

    @Transactional
    public SaleDTO recordSale(Long businessId, Long productId, double quantity, Long branchId) {
        List<Map<String, Object>> items = List.of(Map.of("productId", productId, "quantity", quantity));
        Map<String, Object> result = recordTransaction(businessId, branchId, items, null, null, null, null, null, BigDecimal.ZERO, 5.0, null, null, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> saleItems = (List<Map<String, Object>>) result.get("items");
        Map<String, Object> first = saleItems.get(0);

        return new SaleDTO(
                ((Number) first.get("id")).longValue(),
                ((Number) first.get("productId")).longValue(),
                first.get("productName").toString(),
                ((Number) first.get("quantitySold")).doubleValue(),
                (BigDecimal) first.get("priceAtSale"),
                (BigDecimal) first.get("totalAmount"),
                null,
                result.get("receiptNumber") != null ? result.get("receiptNumber").toString() : null,
                branchId, null, null
        );
    }

    @Transactional
    public SaleDTO recordSale(Long businessId, Long productId, int quantity) {
        return recordSale(businessId, productId, quantity, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getByReceiptNumber(String receiptNumber, Long businessId) {
        SaleTransaction tx = txRepository.findByReceiptNumber(receiptNumber)
                .orElseThrow(() -> new RuntimeException("Receipt not found: " + receiptNumber));
        if (!tx.getBusinessId().equals(businessId)) throw new RuntimeException("Unauthorized.");
        return txToReceiptMap(tx);
    }

    @Transactional(readOnly = true)
    public List<SaleDTO> getAllSales(Long businessId) {
        return saleRepository.findByBusinessIdOrderBySaleDateDesc(businessId)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleDTO> getRecentSales(Long businessId) {
        return saleRepository.findTop10ByBusinessIdOrderBySaleDateDesc(businessId)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // ── Full report — branch-filtered when branchId is present ────
    @Transactional(readOnly = true)
    public Map<String, Object> getFullReport(Long businessId, Long branchId) {
        Map<String, Object> report = new HashMap<>();

        BigDecimal revenue = branchId != null
                ? saleRepository.getTotalRevenueByBranch(businessId, branchId)
                : saleRepository.getTotalRevenueByBusiness(businessId);
        Double cogsRaw = branchId != null
                ? saleRepository.getTotalCOGSByBranch(businessId, branchId)
                : saleRepository.getTotalCOGSByBusiness(businessId);
        BigDecimal cogs = cogsRaw != null ? new BigDecimal(cogsRaw.toString()) : BigDecimal.ZERO;
        if (revenue == null) revenue = BigDecimal.ZERO;

        BigDecimal totalRefunds = returnRepository.getTotalRefundsByBusiness(businessId);
        if (totalRefunds == null) totalRefunds = BigDecimal.ZERO;
        Double totalUnitsReturned = returnRepository.getTotalUnitsReturnedByBusiness(businessId);
        if (totalUnitsReturned == null) totalUnitsReturned = 0.0;
        long totalReturnsCount = returnRepository.countByBusinessId(businessId);

        BigDecimal netRevenue = revenue.subtract(totalRefunds);
        BigDecimal grossProfit = netRevenue.subtract(cogs);

        double profitMargin = netRevenue.compareTo(BigDecimal.ZERO) > 0
                ? grossProfit.divide(netRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;

        // ── FIXED: Count actual checkout transactions (receipts), not sale items ──
        long count = branchId != null
                ? txRepository.countByBusinessIdAndBranchId(businessId, branchId)
                : txRepository.countByBusinessId(businessId);

        BigDecimal avgTransaction = count > 0
                ? revenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Expenses — branch-filtered
        Map<String, Object> expenseSummary = expenseService.getSummary(businessId, branchId);
        BigDecimal totalOperatingExpenses = toBigDecimal(
                expenseSummary.getOrDefault("totalMonthlyBurden", BigDecimal.ZERO));

        // Pass through expense detail for the report's expense breakdown section
        report.put("expenseByCategory", expenseSummary.get("expenseByCategory"));
        report.put("recurringExpenses", expenseSummary.get("recurringExpenses"));
        report.put("totalOneTimeExpenses", expenseSummary.get("totalOneTimeExpenses"));
        report.put("monthlyRecurringBurden", expenseSummary.get("totalMonthlyBurden"));

        BigDecimal netProfit = grossProfit.subtract(totalOperatingExpenses);
        double netMargin = netRevenue.compareTo(BigDecimal.ZERO) > 0
                ? netProfit.divide(netRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;

        report.put("totalRevenue", revenue);
        report.put("totalRefunds", totalRefunds);
        report.put("totalReturnsCount", totalReturnsCount);
        report.put("totalUnitsReturned", totalUnitsReturned);
        report.put("netRevenue", netRevenue);
        report.put("totalCOGS", cogs);
        report.put("grossProfit", grossProfit);
        report.put("profitMargin", Math.round(profitMargin * 100.0) / 100.0);
        report.put("totalSalesCount", count);
        report.put("avgTransactionValue", avgTransaction);
        report.put("totalOperatingExpenses", totalOperatingExpenses);
        report.put("netProfit", netProfit);
        report.put("netProfitMargin", Math.round(netMargin * 100.0) / 100.0);

        Double units = branchId != null
                ? saleRepository.getTotalUnitsSoldByBranch(businessId, branchId)
                : saleRepository.getTotalUnitsSoldByBusiness(businessId);
        report.put("totalUnitsSold", units != null ? units.doubleValue() : 0.0);

        List<Object[]> topRaw = branchId != null
                ? saleRepository.getTopSellingProductsByBranch(businessId, branchId)
                : saleRepository.getTopSellingProductsByBusiness(businessId);
        List<Map<String, Object>> topProducts = new ArrayList<>();
        for (Object[] row : topRaw) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("productName", row[0] != null ? row[0].toString() : "");
            entry.put("unitsSold", row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
            entry.put("totalRevenue", row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO);
            topProducts.add(entry);
        }
        report.put("topSellingProducts", topProducts);

        List<Object[]> catRaw = branchId != null
                ? saleRepository.getProfitByCategoryByBranch(businessId, branchId)
                : saleRepository.getProfitByCategoryByBusiness(businessId);
        List<Map<String, Object>> byCategory = new ArrayList<>();
        for (Object[] row : catRaw) {
            Map<String, Object> entry = new HashMap<>();
            BigDecimal catRevenue = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            BigDecimal catCOGS = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
            BigDecimal catProfit = catRevenue.subtract(catCOGS);
            double catMargin = catRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? catProfit.divide(catRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;
            entry.put("category", row[0] != null ? row[0].toString() : "");
            entry.put("unitsSold", row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
            entry.put("totalRevenue", catRevenue);
            entry.put("totalCOGS", catCOGS);
            entry.put("grossProfit", catProfit);
            entry.put("profitMargin", Math.round(catMargin * 100.0) / 100.0);
            byCategory.add(entry);
        }
        report.put("salesByCategory", byCategory);
        report.put("profitByCategory", byCategory);
        report.put("recentSales", getRecentSales(businessId));

        if (branchId == null) {
            List<Object[]> branchRevRaw = saleRepository.getRevenueByBranch(businessId);
            List<Map<String, Object>> revenueByBranch = new ArrayList<>();
            for (Object[] row : branchRevRaw) {
                Map<String, Object> entry = new HashMap<>();
                Long bId = row[0] != null ? ((Number) row[0]).longValue() : null;
                entry.put("branchId", bId);
                entry.put("totalRevenue", row[1] != null ? row[1] : BigDecimal.ZERO);
                if (bId != null) {
                    String bName = branchRepository.findById(bId).map(Branch::getName).orElse("Unknown");
                    entry.put("branchName", bName);
                }
                revenueByBranch.add(entry);
            }
            report.put("revenueByBranch", revenueByBranch);
        }

        return report;
    }

    @Transactional
    public String generateReceiptNumber(Long branchId) {
        int year = LocalDateTime.now().getYear();
        String branchCode = branchRepository.findById(branchId)
                .map(b -> deriveBranchCode(b.getName())).orElse("BRN");

        ReceiptSequence seq = receiptSequenceRepository
                .findByBranchIdAndYearForUpdate(branchId, year).orElse(null);
        if (seq == null) seq = new ReceiptSequence(branchId, year);

        long next = seq.getLastSequence() + 1;
        seq.setLastSequence(next);
        receiptSequenceRepository.save(seq);
        return String.format("%s-%d-%05d", branchCode, year, next);
    }

    public Map<String, Object> txToReceiptMap(SaleTransaction tx) {
        String branchName = tx.getBranchId() != null
                ? branchRepository.findById(tx.getBranchId()).map(Branch::getName).orElse(null) : null;

        List<Map<String, Object>> items = saleRepository.findByTransactionId(tx.getId())
                .stream().map(this::saleItemToMap).collect(Collectors.toList());

        Map<String, Object> m = new HashMap<>();
        m.put("transactionId",  tx.getId());
        m.put("receiptNumber",  tx.getReceiptNumber());
        m.put("branchId",       tx.getBranchId());
        m.put("branchName",     branchName);
        m.put("cashierName",    tx.getCashierName());
        m.put("paymentMethod",  tx.getPaymentMethod());
        m.put("subtotal",       tx.getSubtotal());
        m.put("discountAmount", tx.getDiscountAmount());
        m.put("vatPercent",     tx.getVatPercent());
        m.put("vatAmount",      tx.getVatAmount());
        m.put("totalAmount",    tx.getTotalAmount());
        m.put("amountReceived", tx.getAmountReceived());
        m.put("changeGiven",    tx.getChangeGiven());
        m.put("dueAmount",      tx.getDueAmount());
        m.put("status",         tx.getStatus());
        m.put("exchangeRate",   tx.getExchangeRate());
        m.put("pricingMethod",  tx.getPricingMethod());
        m.put("goldOzRate",     tx.getGoldOzRate());
        m.put("customerId",     tx.getCustomerId());
        m.put("customerName",   tx.getCustomerName());
        m.put("items",          items);
        m.put("createdAt",      tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> saleItemToMap(Sale s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("productId", s.getProduct().getId());
        m.put("productName", s.getProduct().getName());
        m.put("quantitySold", s.getQuantitySold());
        m.put("priceAtSale", s.getPriceAtSale());
        m.put("totalAmount", s.getTotalAmount());
        m.put("receiptNumber", s.getReceiptNumber());
        m.put("transactionId", s.getTransactionId());
        m.put("scrapPurity", s.getScrapPurity());
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

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        return new BigDecimal(value.toString());
    }

    private SaleDTO convertToDTO(Sale sale) {
        return new SaleDTO(
                sale.getId(), sale.getProduct().getId(), sale.getProduct().getName(),
                sale.getQuantitySold(), sale.getPriceAtSale(), sale.getTotalAmount(),
                sale.getSaleDate(), sale.getReceiptNumber(), sale.getBranchId(),
                sale.getCashierName(), sale.getPaymentMethod()
        );
    }

    /**
     * Resolves the Supplier (party) record for a sale's customerId.
     *
     * Convention:
     *   customerId < 0  → the "customer" is stored in the supplier table with id = -customerId
     *   customerId > 0  → standard Customer; if it has a linkedSupplierId, return that Supplier
     */
    private Optional<Supplier> resolvePartyForSale(Long customerId, Long businessId) {
        if (customerId == null) return Optional.empty();
        if (customerId < 0) {
            // Negative ID namespace: supplier-backed customer
            return supplierRepository.findByIdAndBusinessId(-customerId, businessId);
        }
        // Positive ID: look up regular Customer and follow the linkedSupplierId link
        return customerRepository.findByIdAndBusinessId(customerId, businessId)
                .map(Customer::getLinkedSupplierId)
                .filter(sid -> sid != null)
                .flatMap(sid -> supplierRepository.findByIdAndBusinessId(sid, businessId));
    }
}