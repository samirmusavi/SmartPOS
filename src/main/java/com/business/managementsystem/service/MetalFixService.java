package com.business.managementsystem.service;

import com.business.managementsystem.model.FixSettlement;
import com.business.managementsystem.model.MetalFix;
import com.business.managementsystem.repository.FixSettlementRepository;
import com.business.managementsystem.repository.MetalFixRepository;
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

    private final MetalFixRepository       fixRepo;
    private final FixSettlementRepository  settlementRepo;
    private final PartyLedgerService       partyLedgerService;

    public MetalFixService(MetalFixRepository      fixRepo,
                           FixSettlementRepository settlementRepo,
                           PartyLedgerService      partyLedgerService) {
        this.fixRepo            = fixRepo;
        this.settlementRepo     = settlementRepo;
        this.partyLedgerService = partyLedgerService;
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

        // ── Calculated fields ─────────────────────────────────────────
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

        // ── Generate HPF number ───────────────────────────────────────
        String fixNumber = partyLedgerService.generateVoucherNumber(fix.getBusinessId(), "HPF");
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

        if ("SETTLED".equals(fix.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Fix already settled: " + fix.getFixNumber());
        }

        BigDecimal settlementRate = toBd(req.get("fixedRate"));
        BigDecimal settlementXRate = req.get("exchangeRate") != null
                ? toBd(req.get("exchangeRate")) : fix.getExchangeRate();
        LocalDate settlementDate = req.get("settlementDate") != null
                ? LocalDate.parse(str(req.get("settlementDate"))) : LocalDate.now();

        // Settlement AED at the confirmed rate
        BigDecimal settlementAed = computeTotalAed(
                fix.getWeightGrams(), settlementRate, settlementXRate);

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
        settlement.setFixedRate(settlementRate);
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
        fix.setFixedRate(settlementRate);
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
    //  QUERIES
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Map<String, Object> getFixById(Long fixId) {
        MetalFix fix = fixRepo.findById(fixId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Fix not found: " + fixId));
        Map<String, Object> m = toFixMap(fix);
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
        m.put("linkedSaleId",        f.getLinkedSaleId());
        m.put("linkedPurchaseId",    f.getLinkedPurchaseId());
        m.put("notes",               f.getNotes());
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
        m.put("weightGrams",      s.getWeightGrams());
        m.put("fixedRate",        s.getFixedRate());
        m.put("exchangeRate",     s.getExchangeRate());
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
