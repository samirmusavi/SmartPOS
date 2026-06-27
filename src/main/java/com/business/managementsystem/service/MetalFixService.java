package com.business.managementsystem.service;

import com.business.managementsystem.model.FixSettlement;
import com.business.managementsystem.model.MetalFix;
import com.business.managementsystem.model.Purchase;
import com.business.managementsystem.model.SaleTransaction;
import com.business.managementsystem.repository.FixSettlementRepository;
import com.business.managementsystem.repository.MetalFixRepository;
import com.business.managementsystem.repository.PurchaseRepository;
import com.business.managementsystem.repository.SaleTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business logic for Hold-Price Fix (HPF) lifecycle:
 *   createFix    → status = OPEN,   posts HPF ledger entry
 *   markAsFixed  → status = FIXED,  records fixedRate + fixedDate
 *   settleFix    → status = SETTLED, creates FixSettlement, posts SFX ledger entry
 */
@Service
public class MetalFixService {

    private static final BigDecimal TROY_OZ = new BigDecimal("31.1035");

    private final MetalFixRepository        fixRepo;
    private final FixSettlementRepository   settlementRepo;
    private final PartyLedgerService        partyLedgerService;
    private final SaleTransactionRepository saleTransactionRepository;
    private final PurchaseRepository        purchaseRepository;

    public MetalFixService(MetalFixRepository        fixRepo,
                           FixSettlementRepository   settlementRepo,
                           PartyLedgerService        partyLedgerService,
                           SaleTransactionRepository saleTransactionRepository,
                           PurchaseRepository        purchaseRepository) {
        this.fixRepo                   = fixRepo;
        this.settlementRepo            = settlementRepo;
        this.partyLedgerService        = partyLedgerService;
        this.saleTransactionRepository = saleTransactionRepository;
        this.purchaseRepository        = purchaseRepository;
    }

    // ═══════════════════════════════════════════════════════════════
    //  CREATE FIX
    // ═══════════════════════════════════════════════════════════════

    /**
     * Creates a new metal fix from the request map.
     *
     * Required keys: businessId, branchId, partyId, partyName, fixType (SALE_FIX|PURCHASE_FIX),
     *   metalType (GOLD|SILVER), weightGrams, transactionRate
     * Optional: purity, discountPremium, discountPremiumType (PER_OZ|PERCENTAGE),
     *   exchangeRate, marginPercent, linkedSaleId, linkedPurchaseId, notes
     */
    @Transactional
    public Map<String, Object> createFix(Map<String, Object> req, String createdBy) {
        MetalFix fix = new MetalFix();

        // ── Mandatory fields ──────────────────────────────────────────
        fix.setBusinessId(toLong(req.get("businessId")));
        fix.setBranchId(req.get("branchId") != null ? toLong(req.get("branchId")) : null);
        fix.setPartyId(toLong(req.get("partyId")));
        fix.setPartyName(str(req.get("partyName")));
        fix.setFixType(str(req.get("fixType")));
        fix.setMetalType(str(req.get("metalType")));
        fix.setWeightGrams(toDouble(req.get("weightGrams")));
        fix.setTransactionRate(toBd(req.get("transactionRate")));

        // ── Optional fields ───────────────────────────────────────────
        if (req.get("purity") != null)
            fix.setPurity(str(req.get("purity")));

        BigDecimal dp = req.get("discountPremium") != null
                ? toBd(req.get("discountPremium")) : BigDecimal.ZERO;
        fix.setDiscountPremium(dp);

        String dpType = req.get("discountPremiumType") != null
                ? str(req.get("discountPremiumType")) : "PER_OZ";
        fix.setDiscountPremiumType(dpType);

        BigDecimal xRate = req.get("exchangeRate") != null
                ? toBd(req.get("exchangeRate")) : new BigDecimal("3.6740");
        fix.setExchangeRate(xRate);

        if (req.get("marginPercent") != null)
            fix.setMarginPercent(toBd(req.get("marginPercent")));

        if (req.get("linkedSaleId") != null)
            fix.setLinkedSaleId(toLong(req.get("linkedSaleId")));

        if (req.get("linkedPurchaseId") != null)
            fix.setLinkedPurchaseId(toLong(req.get("linkedPurchaseId")));

        if (req.get("notes") != null)
            fix.setNotes(str(req.get("notes")));

        fix.setCreatedBy(createdBy);

        // ── linkedTransactionType — UNFIXED position fix (Stage 3) ────────────────
        String linkedTxType = req.get("linkedTransactionType") != null
                ? str(req.get("linkedTransactionType")) : null;
        fix.setLinkedTransactionType(linkedTxType);

        if ("UNFIXED_SALE".equals(linkedTxType) || "UNFIXED_PURCHASE".equals(linkedTxType)) {
            return createUnfixedPositionFix(fix, dp, dpType, xRate);
        }

        // ── EXISTING STANDALONE FIX PATH (unchanged) ──────────────────────────────
        BigDecimal effectiveRate = computeEffectiveRate(
                fix.getTransactionRate(), dp, dpType);
        fix.setEffectiveRate(effectiveRate);

        BigDecimal totalAed = computeTotalAed(
                fix.getWeightGrams(), effectiveRate, xRate);
        fix.setTotalAed(totalAed);

        if (fix.getMarginPercent() != null && fix.getMarginPercent().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal marginAmount = totalAed
                    .multiply(fix.getMarginPercent())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            fix.setMarginAmount(marginAmount);
        }

        // ── Generate fix number (PF-#### for Purchase Fix, SF-#### for Sale Fix) ──
        String fixNumber = partyLedgerService.generateVoucherNumber(
                fix.getBusinessId(),
                "PURCHASE_FIX".equals(fix.getFixType()) ? "PF" : "SF");
        fix.setFixNumber(fixNumber);

        fix.setStatus("OPEN");

        // ── Save first to get the ID ──────────────────────────────────
        MetalFix saved = fixRepo.save(fix);

        // ── Post ledger entry ─────────────────────────────────────────
        partyLedgerService.postFixEntry(saved);

        return toFixMap(saved);
    }

    // ═══════════════════════════════════════════════════════════════
    //  MARK AS FIXED
    // ═══════════════════════════════════════════════════════════════

    /**
     * Records the actual market rate at which the fix was confirmed.
     * Transitions OPEN → FIXED (FIXED fixes can also be marked fixed again
     * to update the rate before settlement, which is legitimate in practice).
     *
     * Required keys: fixedRate
     * Optional: fixedDate (defaults to today), notes
     */
    @Transactional
    public Map<String, Object> markAsFixed(Long fixId, Map<String, Object> req) {
        MetalFix fix = fixRepo.findById(fixId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Fix not found: " + fixId));

        // UNFIXED-position fixes are immutable once posted
        if (isUnfixedPositionFix(fix)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Fix " + fix.getFixNumber() + " is an unfixed-position fix and is immutable — " +
                    "it cannot be modified or settled.");
        }

        if ("SETTLED".equals(fix.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cannot mark a settled fix: " + fix.getFixNumber());
        }

        fix.setFixedRate(toBd(req.get("fixedRate")));
        fix.setFixedDate(req.get("fixedDate") != null
                ? LocalDate.parse(str(req.get("fixedDate")))
                : LocalDate.now());
        if (req.get("notes") != null)
            fix.setNotes(str(req.get("notes")));

        fix.setStatus("FIXED");
        return toFixMap(fixRepo.save(fix));
    }

    // ═══════════════════════════════════════════════════════════════
    //  SETTLE FIX
    // ═══════════════════════════════════════════════════════════════

    /**
     * Settles a fix at the confirmed rate, creating a FixSettlement record
     * and posting an SFX ledger entry that closes out the HPF position.
     *
     * The fix must be in OPEN or FIXED status.
     *
     * Required keys: fixedRate (the settlement rate), settlementDate (or defaults to today)
     * Optional: exchangeRate, notes
     */
    @Transactional
    public Map<String, Object> settleFix(Long fixId, Map<String, Object> req, String settledBy) {
        MetalFix fix = fixRepo.findById(fixId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Fix not found: " + fixId));

        // UNFIXED-position fixes are immutable — they have no settlement step
        if (isUnfixedPositionFix(fix)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Fix " + fix.getFixNumber() + " is an unfixed-position fix — " +
                    "it cannot be settled. The price was locked at the time of posting.");
        }

        if ("SETTLED".equals(fix.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Fix already settled: " + fix.getFixNumber());
        }

        // Raw market rate entered by cashier at settlement time
        BigDecimal settlementMarketRate = toBd(req.get("fixedRate"));
        BigDecimal settlementXRate = req.get("exchangeRate") != null
                ? toBd(req.get("exchangeRate")) : fix.getExchangeRate();
        LocalDate settlementDate = req.get("settlementDate") != null
                ? LocalDate.parse(str(req.get("settlementDate"))) : LocalDate.now();

        // Re-apply the SAME discount/premium as the original deal to get the effective rate.
        // This is critical: the party agreed to deal rate ± dp, and settlement re-prices
        // using the new market rate but the same spread.
        BigDecimal settlementEffectiveRate = computeEffectiveRate(
                settlementMarketRate, fix.getDiscountPremium(), fix.getDiscountPremiumType());

        // Settlement AED at the effective rate (mirrors the creation formula exactly)
        BigDecimal settlementAed = computeTotalAed(
                fix.getWeightGrams(), settlementEffectiveRate, settlementXRate);

        BigDecimal differenceCrDr = fix.getTotalAed().subtract(settlementAed);
        // positive → CR (original estimate was higher → party overpaid, they have credit)
        // negative → DR (original estimate was lower  → party underpaid, they owe more)

        // ── Build FixSettlement ───────────────────────────────────────
        FixSettlement settlement = new FixSettlement();
        settlement.setMetalFixId(fix.getId());
        settlement.setBusinessId(fix.getBusinessId());
        settlement.setBranchId(fix.getBranchId());
        settlement.setPartyId(fix.getPartyId());
        settlement.setPartyName(fix.getPartyName());
        settlement.setWeightGrams(fix.getWeightGrams());
        settlement.setSettlementMarketRate(settlementMarketRate); // raw rate entered by cashier
        settlement.setFixedRate(settlementEffectiveRate);          // effective rate after dp
        settlement.setExchangeRate(settlementXRate);
        settlement.setSettlementAed(settlementAed);
        settlement.setOriginalAed(fix.getTotalAed());
        settlement.setDifferenceCrDr(differenceCrDr);
        settlement.setSettledBy(settledBy);
        settlement.setSettlementDate(settlementDate);
        if (req.get("notes") != null)
            settlement.setNotes(str(req.get("notes")));

        // Auto-generate SFX number
        String sfxNumber = partyLedgerService.generateVoucherNumber(fix.getBusinessId(), "SFX");
        settlement.setSettlementNumber(sfxNumber);

        FixSettlement savedSettlement = settlementRepo.save(settlement);

        // ── Update the fix record ─────────────────────────────────────
        fix.setStatus("SETTLED");
        fix.setFixedRate(settlementEffectiveRate); // store effective rate on the fix
        fix.setFixedDate(settlementDate);
        fix.setSettlementAmount(settlementAed);
        fix.setSettlementDate(settlementDate);
        fixRepo.save(fix);

        // ── Post SFX ledger entry ─────────────────────────────────────
        partyLedgerService.postSettlementEntry(savedSettlement, fix);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fix",        toFixMap(fix));
        result.put("settlement", toSettlementMap(savedSettlement));
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  UNFIXED POSITION FIX  (Stage 3)
    //  Consumes open weight from an UNFIXED_AT_TRADE sale or purchase.
    //  Posts AED-only ledger entry; metal was already posted at trade time.
    //  The fix is IMMUTABLE once posted — no OPEN→FIXED→SETTLED lifecycle.
    // ═══════════════════════════════════════════════════════════════

    private Map<String, Object> createUnfixedPositionFix(MetalFix fix,
                                                          BigDecimal userDp,
                                                          String dpType,
                                                          BigDecimal xRate) {
        boolean isSale = "UNFIXED_SALE".equals(fix.getLinkedTransactionType());

        // ── Auto-derive fixType from linkedTransactionType ────────────
        fix.setFixType(isSale ? "SALE_FIX" : "PURCHASE_FIX");

        String parentRef;
        String lockedPurity;

        if (isSale) {
            // ── Validate linked SaleTransaction ───────────────────────
            if (fix.getLinkedSaleId() == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "linkedSaleId is required when linkedTransactionType = UNFIXED_SALE");

            SaleTransaction sale = saleTransactionRepository.findById(fix.getLinkedSaleId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Sale not found: " + fix.getLinkedSaleId()));

            if (!sale.getBusinessId().equals(fix.getBusinessId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sale belongs to a different business.");

            if (!"UNFIXED_AT_TRADE".equals(sale.getOriginalPricingMethod()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Sale " + sale.getReceiptNumber() + " was not entered as Unfixed.");

            if (!"OPEN".equals(sale.getFixingCompletionStatus()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Sale " + sale.getReceiptNumber() + " has no remaining open weight " +
                        "(status: " + sale.getFixingCompletionStatus() + ").");

            double remaining = sale.getRemainingOpenWeightGrams() != null
                    ? sale.getRemainingOpenWeightGrams() : 0.0;
            if (fix.getWeightGrams() > remaining + 0.0001)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(
                        "Fix weight %.4fg exceeds remaining open weight %.4fg for sale %s.",
                        fix.getWeightGrams(), remaining, sale.getReceiptNumber()));

            // Override dp with the locked agreedPremiumDiscount from the original deal
            BigDecimal lockedDp = sale.getAgreedPremiumDiscount() != null
                    ? sale.getAgreedPremiumDiscount() : BigDecimal.ZERO;
            fix.setDiscountPremium(lockedDp);
            fix.setDiscountPremiumType("PER_OZ");

            parentRef   = sale.getReceiptNumber();
            lockedPurity = fix.getPurity();

            // ── Recompute with locked premium ─────────────────────────
            BigDecimal effRate = computeEffectiveRate(fix.getTransactionRate(), lockedDp, "PER_OZ");
            fix.setEffectiveRate(effRate);
            fix.setTotalAed(computeTotalAed(fix.getWeightGrams(), effRate, xRate));

        } else {
            // ── Validate linked Purchase ──────────────────────────────
            if (fix.getLinkedPurchaseId() == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "linkedPurchaseId is required when linkedTransactionType = UNFIXED_PURCHASE");

            Purchase purchase = purchaseRepository.findById(fix.getLinkedPurchaseId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Purchase not found: " + fix.getLinkedPurchaseId()));

            if (!purchase.getBusinessId().equals(fix.getBusinessId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Purchase belongs to a different business.");

            if (!"UNFIXED_AT_TRADE".equals(purchase.getOriginalPricingMethod()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Purchase " + purchase.getInvoiceNumber() + " was not entered as Unfixed.");

            if (!"OPEN".equals(purchase.getFixingCompletionStatus()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Purchase " + purchase.getInvoiceNumber() + " has no remaining open weight " +
                        "(status: " + purchase.getFixingCompletionStatus() + ").");

            double remaining = purchase.getRemainingOpenWeightGrams() != null
                    ? purchase.getRemainingOpenWeightGrams() : 0.0;
            if (fix.getWeightGrams() > remaining + 0.0001)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(
                        "Fix weight %.4fg exceeds remaining open weight %.4fg for purchase %s.",
                        fix.getWeightGrams(), remaining, purchase.getInvoiceNumber()));

            BigDecimal lockedDp = purchase.getAgreedPremiumDiscount() != null
                    ? purchase.getAgreedPremiumDiscount() : BigDecimal.ZERO;
            fix.setDiscountPremium(lockedDp);
            fix.setDiscountPremiumType("PER_OZ");

            parentRef   = purchase.getInvoiceNumber();
            lockedPurity = fix.getPurity();

            BigDecimal effRate = computeEffectiveRate(fix.getTransactionRate(), lockedDp, "PER_OZ");
            fix.setEffectiveRate(effRate);
            fix.setTotalAed(computeTotalAed(fix.getWeightGrams(), effRate, xRate));
        }

        // ── Append parent reference to notes ──────────────────────────
        String notePrefix = "REF:" + parentRef;
        fix.setNotes(fix.getNotes() != null && !fix.getNotes().isBlank()
                ? notePrefix + " | " + fix.getNotes() : notePrefix);

        // ── Generate fix number (SF-#### or PF-####) ──────────────────
        String fixNumber = partyLedgerService.generateVoucherNumber(
                fix.getBusinessId(), isSale ? "SF" : "PF");
        fix.setFixNumber(fixNumber);

        // IMMUTABLE: posted directly as FIXED (no OPEN phase)
        fix.setStatus("FIXED");
        fix.setFixedDate(LocalDate.now());
        fix.setFixedRate(fix.getEffectiveRate());

        MetalFix saved = fixRepo.save(fix);

        // ── Consume open weight on the parent transaction ──────────────
        if (isSale) {
            SaleTransaction sale = saleTransactionRepository.findById(fix.getLinkedSaleId()).get();
            double remaining = sale.getRemainingOpenWeightGrams() != null
                    ? sale.getRemainingOpenWeightGrams() : 0.0;
            double newRemaining = Math.max(0.0, remaining - fix.getWeightGrams());
            sale.setRemainingOpenWeightGrams(newRemaining);
            if (newRemaining < 0.001) {
                sale.setFixingCompletionStatus("FULLY_FIXED");
            }
            saleTransactionRepository.save(sale);
            // Post SF ledger: AED credit only (metal already posted at UNFIXED_AT_TRADE SAL time)
            partyLedgerService.postUnfixedSaleFixEntry(saved, parentRef);
        } else {
            Purchase purchase = purchaseRepository.findById(fix.getLinkedPurchaseId()).get();
            double remaining = purchase.getRemainingOpenWeightGrams() != null
                    ? purchase.getRemainingOpenWeightGrams() : 0.0;
            double newRemaining = Math.max(0.0, remaining - fix.getWeightGrams());
            purchase.setRemainingOpenWeightGrams(newRemaining);
            if (newRemaining < 0.001) {
                purchase.setFixingCompletionStatus("FULLY_FIXED");
            }
            purchaseRepository.save(purchase);
            // Post PF ledger: AED debit only (metal already posted at UNFIXED_AT_TRADE PUR time)
            partyLedgerService.postUnfixedPurchaseFixEntry(saved, parentRef);
        }

        return toFixMap(saved);
    }

    // ═══════════════════════════════════════════════════════════════
    //  OPEN UNFIXED POSITIONS  (used by Stage 5 metal-fix.html UI)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns UNFIXED_AT_TRADE sales that still have open weight to price.
     * Each entry shows receiptNumber, customer, remaining weight, locked premium.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOpenUnfixedSales(Long businessId, Long branchId) {
        List<SaleTransaction> txns = branchId != null
                ? saleTransactionRepository.findOpenUnfixedSalesByBranch(businessId, branchId)
                : saleTransactionRepository.findOpenUnfixedSales(businessId);
        return txns.stream().map(this::txToUnfixedSaleMap).collect(Collectors.toList());
    }

    /**
     * Returns UNFIXED_AT_TRADE purchases that still have open weight to price.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOpenUnfixedPurchases(Long businessId, Long branchId) {
        List<Purchase> purchases = branchId != null
                ? purchaseRepository.findOpenUnfixedPurchasesByBranch(businessId, branchId)
                : purchaseRepository.findOpenUnfixedPurchases(businessId);
        return purchases.stream().map(this::purchaseToUnfixedMap).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    //  EXPOSURE DASHBOARD  (Stage 4)
    //  Derived on-the-fly from existing UNFIXED_AT_TRADE records.
    //  NO new persisted table — pure aggregation over Sale/Purchase.
    // ═══════════════════════════════════════════════════════════════

    /**
     * Aggregates all open unfixed-position weight and produces an exposure snapshot.
     *
     * <p>Convention:
     * <ul>
     *   <li>Sale positions  — metal has left vault, AED flows IN later  → net CR claim</li>
     *   <li>Purchase positions — metal has entered vault, AED flows OUT later → net DR obligation</li>
     *   <li>netOpenWeightGrams = saleGrams − purchaseGrams
     *       (positive = net we are owed; negative = net we owe)</li>
     * </ul>
     *
     * @param currentOzRate optional live $/oz spot rate; when supplied, estimated AED
     *                      values are included in the response
     * @param xRateParam    optional USD→AED exchange rate (defaults to 3.6740)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getExposureDashboard(Long businessId, Long branchId,
                                                     BigDecimal currentOzRate,
                                                     BigDecimal xRateParam) {
        // Reuse Stage-3 repository queries
        List<SaleTransaction> openSales = branchId != null
                ? saleTransactionRepository.findOpenUnfixedSalesByBranch(businessId, branchId)
                : saleTransactionRepository.findOpenUnfixedSales(businessId);

        List<Purchase> openPurchases = branchId != null
                ? purchaseRepository.findOpenUnfixedPurchasesByBranch(businessId, branchId)
                : purchaseRepository.findOpenUnfixedPurchases(businessId);

        // Aggregate remaining open weight across all OPEN positions
        double totalSaleGrams = openSales.stream()
                .mapToDouble(t -> t.getRemainingOpenWeightGrams() != null
                        ? t.getRemainingOpenWeightGrams() : 0.0)
                .sum();

        double totalPurchaseGrams = openPurchases.stream()
                .mapToDouble(p -> p.getRemainingOpenWeightGrams() != null
                        ? p.getRemainingOpenWeightGrams() : 0.0)
                .sum();

        // Positive = net inflow (more AED owed to us); negative = net outflow (we owe more)
        double netGrams = totalSaleGrams - totalPurchaseGrams;

        BigDecimal xRate = (xRateParam != null && xRateParam.compareTo(BigDecimal.ZERO) > 0)
                ? xRateParam : new BigDecimal("3.6740");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("openSaleCount",                openSales.size());
        result.put("openPurchaseCount",            openPurchases.size());
        result.put("totalOpenSaleWeightGrams",     totalSaleGrams);
        result.put("totalOpenPurchaseWeightGrams", totalPurchaseGrams);
        result.put("netOpenWeightGrams",           netGrams);

        // AED estimation is optional — only included when a live rate is supplied
        if (currentOzRate != null && currentOzRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal saleAed     = computeTotalAed(totalSaleGrams,     currentOzRate, xRate);
            BigDecimal purchaseAed = computeTotalAed(totalPurchaseGrams, currentOzRate, xRate);
            // positive = net AED inflow; negative = net AED outflow
            BigDecimal netAed = saleAed.subtract(purchaseAed);
            result.put("estimatedSaleAed",     saleAed);
            result.put("estimatedPurchaseAed", purchaseAed);
            result.put("estimatedNetAed",      netAed);
            result.put("rateUsed",             currentOzRate);
            result.put("exchangeRateUsed",     xRate);
        }

        // Detailed position lists (same maps used by the pick-list endpoints)
        result.put("openSalePositions",
                openSales.stream().map(this::txToUnfixedSaleMap).collect(Collectors.toList()));
        result.put("openPurchasePositions",
                openPurchases.stream().map(this::purchaseToUnfixedMap).collect(Collectors.toList()));

        return result;
    }

    private Map<String, Object> txToUnfixedSaleMap(SaleTransaction tx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                      tx.getId());
        m.put("receiptNumber",           tx.getReceiptNumber());
        m.put("customerId",              tx.getCustomerId());
        m.put("customerName",            tx.getCustomerName());
        m.put("grossWeightGrams",        tx.getGrossWeightGrams());
        m.put("pureWeightGrams",         tx.getPureWeightGrams());
        m.put("remainingOpenWeightGrams",tx.getRemainingOpenWeightGrams());
        m.put("agreedPremiumDiscount",   tx.getAgreedPremiumDiscount());
        m.put("fixingCompletionStatus",  tx.getFixingCompletionStatus());
        m.put("exchangeRate",            tx.getExchangeRate());
        m.put("createdAt", tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> purchaseToUnfixedMap(Purchase p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                      p.getId());
        m.put("invoiceNumber",           p.getInvoiceNumber());
        m.put("supplierId",              p.getSupplierId());
        m.put("supplierName",            p.getSupplierName());
        m.put("grossWeightGrams",        p.getGrossWeightGrams());
        m.put("pureWeightGrams",         p.getPureWeightGrams());
        m.put("remainingOpenWeightGrams",p.getRemainingOpenWeightGrams());
        m.put("agreedPremiumDiscount",   p.getAgreedPremiumDiscount());
        m.put("fixingCompletionStatus",  p.getFixingCompletionStatus());
        m.put("exchangeRate",            p.getExchangeRate());
        m.put("purchaseDate", p.getPurchaseDate() != null
                ? p.getPurchaseDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) : null);
        return m;
    }

    /** Returns true when this fix was created to consume an UNFIXED_AT_TRADE position */
    private boolean isUnfixedPositionFix(MetalFix fix) {
        return "UNFIXED_SALE".equals(fix.getLinkedTransactionType())
                || "UNFIXED_PURCHASE".equals(fix.getLinkedTransactionType());
    }

    // ═══════════════════════════════════════════════════════════════
    //  QUERIES
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Map<String, Object> getFixById(Long fixId) {
        MetalFix fix = fixRepo.findById(fixId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Fix not found: " + fixId));
        Map<String, Object> m = toFixMap(fix);

        // Fix 6 — enrich with linked sale receipt number and purchase invoice number
        if (fix.getLinkedSaleId() != null) {
            saleTransactionRepository.findById(fix.getLinkedSaleId()).ifPresent(sale ->
                    m.put("linkedSaleReceiptNumber", sale.getReceiptNumber()));
        }
        if (fix.getLinkedPurchaseId() != null) {
            purchaseRepository.findById(fix.getLinkedPurchaseId()).ifPresent(purchase ->
                    m.put("linkedPurchaseInvoiceNumber", purchase.getInvoiceNumber()));
        }

        // Include settlements for detail view
        List<Map<String, Object>> settlements = settlementRepo
                .findByMetalFixIdOrderByCreatedAtDesc(fixId)
                .stream().map(this::toSettlementMap)
                .collect(Collectors.toList());
        m.put("settlements", settlements);
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFixes(Long businessId, Long branchId,
                                        String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MetalFix> fixPage;

        if (branchId != null) {
            fixPage = fixRepo.findByBusinessIdAndBranchIdOrderByCreatedAtDesc(
                    businessId, branchId, pageable);
        } else {
            fixPage = fixRepo.findByBusinessIdOrderByCreatedAtDesc(businessId, pageable);
        }

        // Filter by status in-memory if requested (paging is approximate when filtering)
        List<Map<String, Object>> content = fixPage.getContent()
                .stream()
                .filter(f -> status == null || status.isBlank() || status.equalsIgnoreCase(f.getStatus()))
                .map(this::toFixMap)
                .collect(Collectors.toList());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("content",       content);
        m.put("totalElements", fixPage.getTotalElements());
        m.put("totalPages",    fixPage.getTotalPages());
        m.put("currentPage",   page);
        m.put("pageSize",      size);
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFixesByParty(Long businessId, Long partyId) {
        return fixRepo.findByBusinessIdAndPartyIdOrderByCreatedAtDesc(businessId, partyId)
                .stream().map(this::toFixMap)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOpenFixes(Long businessId, Long branchId) {
        List<MetalFix> fixes;
        if (branchId != null) {
            fixes = fixRepo.findByBusinessIdAndBranchIdAndStatusOrderByCreatedAtDesc(
                    businessId, branchId, "OPEN");
        } else {
            fixes = fixRepo.findByBusinessIdAndStatusOrderByCreatedAtDesc(businessId, "OPEN");
        }
        return fixes.stream().map(this::toFixMap).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardCounts(Long businessId, Long branchId) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (branchId != null) {
            m.put("openCount",     fixRepo.countByBusinessIdAndBranchIdAndStatus(businessId, branchId, "OPEN"));
            m.put("fixedCount",    fixRepo.countByBusinessIdAndBranchIdAndStatus(businessId, branchId, "FIXED"));
            m.put("settledCount",  fixRepo.countByBusinessIdAndBranchIdAndStatus(businessId, branchId, "SETTLED"));
        } else {
            m.put("openCount",     fixRepo.countByBusinessIdAndStatus(businessId, "OPEN"));
            m.put("fixedCount",    fixRepo.countByBusinessIdAndStatus(businessId, "FIXED"));
            m.put("settledCount",  fixRepo.countByBusinessIdAndStatus(businessId, "SETTLED"));
        }
        BigDecimal openAed = fixRepo.sumTotalAedByBusinessIdAndStatus(businessId, "OPEN");
        m.put("openAed", openAed != null ? openAed : BigDecimal.ZERO);
        return m;
    }

    // ═══════════════════════════════════════════════════════════════
    //  CALCULATION HELPERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Computes the effective rate from transactionRate, discountPremium, and type.
     *
     * PER_OZ:      effectiveRate = transactionRate + discountPremium  (may be negative)
     * PERCENTAGE:  effectiveRate = transactionRate × (1 + discountPremium/100)
     */
    private BigDecimal computeEffectiveRate(BigDecimal transactionRate,
                                             BigDecimal discountPremium,
                                             String type) {
        if (discountPremium == null || discountPremium.compareTo(BigDecimal.ZERO) == 0) {
            return transactionRate;
        }
        if ("PERCENTAGE".equalsIgnoreCase(type)) {
            // e.g. rate=3000, dp=2%  → effectiveRate = 3000 * 1.02 = 3060
            BigDecimal factor = BigDecimal.ONE.add(
                    discountPremium.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
            return transactionRate.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        }
        // Default: PER_OZ  (dp can be negative for discount)
        return transactionRate.add(discountPremium).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Computes AED value from weight in grams, USD/oz rate, and USD→AED exchange rate.
     * Formula: (weightGrams / 31.1035) × usdRate × exchangeRate
     */
    private BigDecimal computeTotalAed(Double weightGrams, BigDecimal usdRate,
                                        BigDecimal exchangeRate) {
        BigDecimal oz  = BigDecimal.valueOf(weightGrams)
                .divide(TROY_OZ, 8, RoundingMode.HALF_UP);
        return oz.multiply(usdRate)
                 .multiply(exchangeRate)
                 .setScale(2, RoundingMode.HALF_UP);
    }

    // ═══════════════════════════════════════════════════════════════
    //  SERIALISATION
    // ═══════════════════════════════════════════════════════════════

    public Map<String, Object> toFixMap(MetalFix f) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                  f.getId());
        m.put("businessId",          f.getBusinessId());
        m.put("branchId",            f.getBranchId());
        m.put("fixNumber",           f.getFixNumber());
        m.put("partyId",             f.getPartyId());
        m.put("partyName",           f.getPartyName());
        m.put("fixType",             f.getFixType());
        m.put("metalType",           f.getMetalType());
        m.put("purity",              f.getPurity());
        m.put("weightGrams",         f.getWeightGrams());
        m.put("transactionRate",     f.getTransactionRate());
        m.put("discountPremium",     f.getDiscountPremium());
        m.put("discountPremiumType", f.getDiscountPremiumType());
        m.put("effectiveRate",       f.getEffectiveRate());
        m.put("exchangeRate",        f.getExchangeRate());
        m.put("totalAed",            f.getTotalAed());
        m.put("marginPercent",       f.getMarginPercent());
        m.put("marginAmount",        f.getMarginAmount());
        m.put("status",              f.getStatus());
        m.put("fixedRate",           f.getFixedRate());
        m.put("fixedDate",           f.getFixedDate() != null ? f.getFixedDate().toString() : null);
        m.put("settlementAmount",    f.getSettlementAmount());
        m.put("settlementDate",      f.getSettlementDate() != null ? f.getSettlementDate().toString() : null);
        m.put("linkedSaleId",           f.getLinkedSaleId());
        m.put("linkedPurchaseId",       f.getLinkedPurchaseId());
        m.put("linkedTransactionType",  f.getLinkedTransactionType());
        m.put("notes",                  f.getNotes());
        m.put("createdAt",           f.getCreatedAt() != null ? f.getCreatedAt().format(fmt) : null);
        m.put("updatedAt",           f.getUpdatedAt() != null ? f.getUpdatedAt().format(fmt) : null);
        m.put("createdBy",           f.getCreatedBy());
        return m;
    }

    public Map<String, Object> toSettlementMap(FixSettlement s) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",               s.getId());
        m.put("metalFixId",       s.getMetalFixId());
        m.put("settlementNumber", s.getSettlementNumber());
        m.put("businessId",       s.getBusinessId());
        m.put("branchId",         s.getBranchId());
        m.put("partyId",          s.getPartyId());
        m.put("partyName",        s.getPartyName());
        m.put("weightGrams",           s.getWeightGrams());
        m.put("settlementMarketRate",  s.getSettlementMarketRate()); // raw market rate entered
        m.put("fixedRate",             s.getFixedRate());             // effective rate (mkt ± dp)
        m.put("exchangeRate",          s.getExchangeRate());
        m.put("settlementAed",    s.getSettlementAed());
        m.put("originalAed",      s.getOriginalAed());
        m.put("differenceCrDr",   s.getDifferenceCrDr());
        m.put("settledBy",        s.getSettledBy());
        m.put("settlementDate",   s.getSettlementDate() != null ? s.getSettlementDate().toString() : null);
        m.put("notes",            s.getNotes());
        m.put("createdAt",        s.getCreatedAt() != null ? s.getCreatedAt().format(fmt) : null);
        return m;
    }

    // ═══════════════════════════════════════════════════════════════
    //  TYPE COERCION UTILITIES
    // ═══════════════════════════════════════════════════════════════

    private Long toLong(Object v) {
        if (v == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Required numeric field is null");
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(v.toString().trim());
    }

    private Double toDouble(Object v) {
        if (v == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Required numeric field is null");
        if (v instanceof Number) return ((Number) v).doubleValue();
        return Double.parseDouble(v.toString().trim());
    }

    private BigDecimal toBd(Object v) {
        if (v == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Required numeric field is null");
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(v.toString());
        return new BigDecimal(v.toString().trim());
    }

    private String str(Object v) {
        return v != null ? v.toString().trim() : null;
    }
}
