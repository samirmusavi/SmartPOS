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

/**
 * Core party ledger engine.
 *
 * Running balance convention:  Σ(aedCredit) − Σ(aedDebit)
 *   positive value → CR (party owes you, or has a credit with you)
 *   negative value → DR (you owe the party)
 *
 * Entry directions:
 *   SAL  → aedCredit  (party owes you for gold sold)    + metalDebit (metal given out)
 *   PUR  → aedCredit  (party delivered metal; you owe them AED → they have credit) + metalCredit (metal received)
 *   REC  → aedDebit   (party paid you → reduces their credit balance toward 0)
 *   PAY  → aedDebit   (you paid party → reduces their credit balance toward 0)
 *   HPF  → same direction as SAL/PUR depending on fixType
 *   SFX  → metal close (always) + NET AED difference only (avoids double-posting the HPF amount)
 */
@Service
public class PartyLedgerService {

    private static final BigDecimal TROY_OZ = new BigDecimal("31.1035");

    private final PartyLedgerEntryRepository ledgerRepo;
    private final VoucherSequenceRepository  seqRepo;
    private final SaleRepository             saleRepo;
    private final PurchaseItemRepository     purchaseItemRepo;

    public PartyLedgerService(PartyLedgerEntryRepository ledgerRepo,
                               VoucherSequenceRepository  seqRepo,
                               SaleRepository             saleRepo,
                               PurchaseItemRepository     purchaseItemRepo) {
        this.ledgerRepo      = ledgerRepo;
        this.seqRepo         = seqRepo;
        this.saleRepo        = saleRepo;
        this.purchaseItemRepo = purchaseItemRepo;
    }

    // ═══════════════════════════════════════════════════════════════
    //  SEQUENCE GENERATOR
    // ═══════════════════════════════════════════════════════════════

    /**
     * Generates the next sequential voucher number for a given type.
     * Uses a pessimistic lock to prevent duplicate numbers under concurrency.
     * Format: {TYPE}-{number}  e.g. REC-42, PAY-7, HPF-15
     */
    @Transactional
    public String generateVoucherNumber(Long businessId, String type) {
        VoucherSequence seq = seqRepo.findForUpdate(businessId, type)
                .orElse(new VoucherSequence(businessId, type));
        int next = seq.getLastNumber() + 1;
        seq.setLastNumber(next);
        seqRepo.save(seq);
        return type + "-" + next;
    }

    // ═══════════════════════════════════════════════════════════════
    //  SALE ENTRY
    // ═══════════════════════════════════════════════════════════════

    /**
     * Posts one or two ledger entries for a sale transaction linked to a party.
     *
     * Entry 1 — METAL:  aedCredit = base metal value, metalDebit = grams
     * Entry 2 — PREMIUM (if non-zero): aedCredit (premium) or aedDebit (discount)
     *
     * If the sale is not a bullion OZ_RATE sale (no goldOzRate), posts a single
     * AED-only entry for the full totalAmount.
     */
    @Transactional
    public void postSaleEntry(SaleTransaction sale, Supplier party) {
        List<Sale> items = saleRepo.findByTransactionId(sale.getId());

        // Aggregate metal weight and purity from GRAM-unit items
        double totalGrams = 0.0;
        String purity     = null;
        String metalType  = "GOLD";

        for (Sale item : items) {
            if ("GRAM".equalsIgnoreCase(item.getUnitType())) {
                totalGrams += item.getQuantitySold();
                if (purity == null && item.getPurity() != null && !item.getPurity().isBlank())
                    purity = item.getPurity();
            }
        }
        if (purity == null) purity = "995";

        BigDecimal exchangeRate = sale.getExchangeRate() != null
                ? sale.getExchangeRate() : new BigDecimal("3.6740");
        LocalDate  date         = sale.getCreatedAt() != null
                ? sale.getCreatedAt().toLocalDate() : LocalDate.now();
        String     voucherNo    = sale.getReceiptNumber() != null
                ? sale.getReceiptNumber() : generateVoucherNumber(sale.getBusinessId(), "SAL");

        // ── Determine if this is an OZ-rate or PER-GRAM bullion sale ──
        // Fix 3: must also confirm pricingMethod is OZ_RATE or PER_GRAM
        //        (not just any sale that happens to have a goldOzRate value)
        boolean isBullionSale = totalGrams > 0
                && sale.getGoldOzRate() != null
                && sale.getGoldOzRate().compareTo(BigDecimal.ZERO) > 0
                && ("OZ_RATE".equals(sale.getPricingMethod())
                        || "PER_GRAM".equals(sale.getPricingMethod()));

        if (isBullionSale) {
            BigDecimal ozWeight   = BigDecimal.valueOf(totalGrams)
                    .divide(TROY_OZ, 8, RoundingMode.HALF_UP);
            BigDecimal metalValue = ozWeight.multiply(sale.getGoldOzRate())
                    .multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
            // Fix 2: use the premium stored by the calculator for accuracy;
            //        fall back to recalculation only if not stored.
            BigDecimal premium = (sale.getPremiumAmount() != null
                    && sale.getPremiumAmount().compareTo(BigDecimal.ZERO) != 0)
                    ? sale.getPremiumAmount()
                    : sale.getTotalAmount().subtract(metalValue);

            // Per-oz USD premium/discount for narration.
            // premiumAmount is stored as total AED:
            //   premiumAmount = ozWeight × purityFactor × premiumPerOzUSD × exchangeRate
            // Recover premiumPerOzUSD = premiumAmount / (ozWeight × purityFactor × exchangeRate)
            BigDecimal perOz = BigDecimal.ZERO;
            if (ozWeight.compareTo(BigDecimal.ZERO) > 0 && exchangeRate.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal purityFactor;
                try {
                    purityFactor = new BigDecimal(purity).divide(new BigDecimal("1000"), 8, RoundingMode.HALF_UP);
                    if (purityFactor.compareTo(BigDecimal.ZERO) <= 0) purityFactor = BigDecimal.ONE;
                } catch (NumberFormatException ignored) {
                    purityFactor = BigDecimal.ONE;
                }
                perOz = premium.divide(
                        ozWeight.multiply(purityFactor).multiply(exchangeRate), 2, RoundingMode.HALF_UP);
            }

            String discPremLabel = premium.compareTo(BigDecimal.ZERO) >= 0 ? "PREM" : "DISC";
            String weightStr     = formatWeight(totalGrams);

            // Entry 1 — METAL
            PartyLedgerEntry e1 = newEntry(party, "SAL", voucherNo, date,
                    sale.getBusinessId(), sale.getBranchId());
            e1.setNarration(String.format("SALE %s %s @%s$ %s METAL",
                    weightStr, purity, perOz.toPlainString(), discPremLabel));
            e1.setAedCredit(metalValue);
            e1.setMetalDebit(totalGrams);
            e1.setMetalType(metalType);
            e1.setPurity(purity);
            e1.setReferenceId(sale.getId());
            e1.setReferenceType("SALE");
            ledgerRepo.save(e1);

            // Entry 2 — PREMIUM/DISCOUNT (only if non-zero)
            if (premium.abs().compareTo(new BigDecimal("0.01")) >= 0) {
                PartyLedgerEntry e2 = newEntry(party, "SAL", voucherNo, date,
                        sale.getBusinessId(), sale.getBranchId());
                e2.setNarration(String.format("SALE %s %s @%s$ %s PREMIUM",
                        weightStr, purity, perOz.toPlainString(), discPremLabel));
                if (premium.compareTo(BigDecimal.ZERO) > 0) {
                    e2.setAedCredit(premium);         // premium → additional receivable
                } else {
                    e2.setAedDebit(premium.negate()); // discount → reduces receivable
                }
                e2.setReferenceId(sale.getId());
                e2.setReferenceType("SALE");
                ledgerRepo.save(e2);
            }

        } else {
            // Non-bullion or missing rate — simple AED-only entry
            PartyLedgerEntry e = newEntry(party, "SAL", voucherNo, date,
                    sale.getBusinessId(), sale.getBranchId());
            e.setNarration("SALE " + (sale.getReceiptNumber() != null ? sale.getReceiptNumber() : ""));
            e.setAedCredit(sale.getTotalAmount());
            if (totalGrams > 0) {
                e.setMetalDebit(totalGrams);
                e.setMetalType(metalType);
                e.setPurity(purity);
            }
            e.setReferenceId(sale.getId());
            e.setReferenceType("SALE");
            ledgerRepo.save(e);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  UNFIXED SALE ENTRY  (UNFIXED_AT_TRADE — metal-only, AED = ZERO)
    //
    //  Metal physically leaves the vault at sale time.
    //  AED value is unknown until a later Fixing event settles the price.
    //  Rule: post metalDebit = pureWeightGrams, aedCredit = ZERO.
    //  The subsequent Fixing event will post the full AED credit.
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void postUnfixedSaleEntry(SaleTransaction sale, Supplier party) {
        if (sale.getPureWeightGrams() == null || sale.getPureWeightGrams() <= 0) return;

        List<Sale> items = saleRepo.findByTransactionId(sale.getId());
        String purity    = null;
        for (Sale item : items) {
            if ("GRAM".equalsIgnoreCase(item.getUnitType())
                    && item.getPurity() != null && !item.getPurity().isBlank()) {
                purity = item.getPurity();
                break;
            }
        }
        if (purity == null) purity = "995";

        LocalDate  date      = sale.getCreatedAt() != null
                ? sale.getCreatedAt().toLocalDate() : LocalDate.now();
        String     voucherNo = sale.getReceiptNumber() != null
                ? sale.getReceiptNumber() : generateVoucherNumber(sale.getBusinessId(), "SAL");

        BigDecimal perOz = sale.getAgreedPremiumDiscount() != null
                ? sale.getAgreedPremiumDiscount() : BigDecimal.ZERO;
        String discPremLabel = perOz.compareTo(BigDecimal.ZERO) >= 0 ? "PREM" : "DISC";
        String weightStr     = formatWeight(sale.getPureWeightGrams());

        PartyLedgerEntry e = newEntry(party, "SAL", voucherNo, date,
                sale.getBusinessId(), sale.getBranchId());
        e.setNarration(String.format("UNFIXED SALE %s %s @%s$ %s — AED DEFERRED",
                weightStr, purity, perOz.abs().toPlainString(), discPremLabel));
        // AED price is deferred — post ZERO AED, metal debit only
        e.setAedCredit(BigDecimal.ZERO);
        e.setMetalDebit(sale.getPureWeightGrams());
        e.setMetalType("GOLD");
        e.setPurity(purity);
        e.setReferenceId(sale.getId());
        e.setReferenceType("SALE");
        ledgerRepo.save(e);
    }

    // ═══════════════════════════════════════════════════════════════
    //  PURCHASE ENTRY
    // ═══════════════════════════════════════════════════════════════

    /**
     * Posts one or two ledger entries for a purchase transaction.
     *
     * Entry 1 — METAL:  aedCredit = base metal value, metalCredit = grams received
     * Entry 2 — PREMIUM (if non-zero)
     */
    @Transactional
    public void postPurchaseEntry(Purchase purchase, Supplier party) {
        List<PurchaseItem> items = purchaseItemRepo.findByPurchaseIdOrderByIdAsc(purchase.getId());

        double totalGrams = 0.0;
        String purity     = null;
        String metalType  = "GOLD";

        for (PurchaseItem item : items) {
            double grams = 0.0;
            if ("GRAM".equalsIgnoreCase(item.getUnitType())) {
                grams = item.getWeightGrams() != null ? item.getWeightGrams() : item.getQuantity();
            } else if (item.getWeightGrams() != null && item.getWeightGrams() > 0) {
                grams = item.getWeightGrams();
            }
            totalGrams += grams;
            if (purity == null && item.getPurity() != null && !item.getPurity().isBlank())
                purity = item.getPurity();
        }
        if (purity == null) purity = "995";

        BigDecimal exchangeRate = purchase.getExchangeRate() != null
                ? purchase.getExchangeRate() : new BigDecimal("3.6740");
        LocalDate  date         = purchase.getPurchaseDate() != null
                ? purchase.getPurchaseDate().toLocalDate() : LocalDate.now();
        String     voucherNo    = purchase.getInvoiceNumber();

        // Determine the gold oz rate from items
        BigDecimal goldOzRate = null;
        for (PurchaseItem item : items) {
            if (item.getGoldOzRate() != null) { goldOzRate = item.getGoldOzRate(); break; }
        }

        boolean isBullionPurchase = totalGrams > 0 && goldOzRate != null;

        if (isBullionPurchase) {
            BigDecimal ozWeight   = BigDecimal.valueOf(totalGrams)
                    .divide(TROY_OZ, 8, RoundingMode.HALF_UP);
            BigDecimal metalValue = ozWeight.multiply(goldOzRate)
                    .multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
            // Fix 4: use the premium stored on the purchase for accuracy;
            //        fall back to recalculation only if not stored.
            BigDecimal premium = (purchase.getPremiumAmount() != null
                    && purchase.getPremiumAmount().compareTo(BigDecimal.ZERO) != 0)
                    ? purchase.getPremiumAmount()
                    : purchase.getTotalAmount().subtract(metalValue);

            // Bug-fix: recover USD/oz premium from total AED premium.
            // premiumAED = ozWeight × purityFactor × premiumPerOzUSD × exchangeRate
            // → premiumPerOzUSD = premiumAED / (ozWeight × purityFactor × exchangeRate)
            BigDecimal perOz = BigDecimal.ZERO;
            if (ozWeight.compareTo(BigDecimal.ZERO) > 0 && exchangeRate.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal purityFactor;
                try {
                    purityFactor = new BigDecimal(purity).divide(new BigDecimal("1000"), 8, RoundingMode.HALF_UP);
                    if (purityFactor.compareTo(BigDecimal.ZERO) <= 0) purityFactor = BigDecimal.ONE;
                } catch (NumberFormatException ignored) {
                    purityFactor = BigDecimal.ONE;
                }
                perOz = premium.divide(
                        ozWeight.multiply(purityFactor).multiply(exchangeRate), 2, RoundingMode.HALF_UP);
            }

            String discPremLabel = premium.compareTo(BigDecimal.ZERO) >= 0 ? "PREM" : "DISC";
            String weightStr     = formatWeight(totalGrams);

            // Entry 1 — METAL
            PartyLedgerEntry e1 = newEntry(party, "PUR", voucherNo, date,
                    purchase.getBusinessId(), purchase.getBranchId());
            e1.setNarration(String.format("PURCHASE %s %s @%s$ METAL",
                    weightStr, purity, perOz.toPlainString()));
            e1.setAedCredit(metalValue);
            e1.setMetalCredit(totalGrams);
            e1.setMetalType(metalType);
            e1.setPurity(purity);
            e1.setReferenceId(purchase.getId());
            e1.setReferenceType("PURCHASE");
            ledgerRepo.save(e1);

            // Entry 2 — PREMIUM/DISCOUNT
            if (premium.abs().compareTo(new BigDecimal("0.01")) >= 0) {
                PartyLedgerEntry e2 = newEntry(party, "PUR", voucherNo, date,
                        purchase.getBusinessId(), purchase.getBranchId());
                e2.setNarration(String.format("PURCHASE %s %s @%s$ %s PREMIUM",
                        weightStr, purity, perOz.toPlainString(), discPremLabel));
                if (premium.compareTo(BigDecimal.ZERO) > 0) {
                    e2.setAedCredit(premium);          // premium → party has more credit
                } else {
                    e2.setAedDebit(premium.negate());  // discount → reduces their credit
                }
                e2.setReferenceId(purchase.getId());
                e2.setReferenceType("PURCHASE");
                ledgerRepo.save(e2);
            }

        } else {
            // Simple purchase entry — whole amount as credit (party has credit; we owe them)
            PartyLedgerEntry e = newEntry(party, "PUR", voucherNo, date,
                    purchase.getBusinessId(), purchase.getBranchId());
            e.setNarration("PURCHASE " + purchase.getInvoiceNumber());
            e.setAedCredit(purchase.getTotalAmount());
            if (totalGrams > 0) {
                e.setMetalCredit(totalGrams);
                e.setMetalType(metalType);
                e.setPurity(purity);
            }
            e.setReferenceId(purchase.getId());
            e.setReferenceType("PURCHASE");
            ledgerRepo.save(e);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  UNFIXED PURCHASE ENTRY  (UNFIXED_AT_TRADE — metal-only, AED = ZERO)
    //
    //  Metal physically arrives at the vault at purchase time.
    //  AED price is unknown until a later Fixing event settles the price.
    //  Rule: post metalCredit = pureWeightGrams, aedDebit = ZERO.
    //  The subsequent Fixing event will post the full AED debit.
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void postUnfixedPurchaseEntry(Purchase purchase, Supplier party) {
        if (purchase.getPureWeightGrams() == null || purchase.getPureWeightGrams() <= 0) return;

        List<PurchaseItem> items = purchaseItemRepo.findByPurchaseIdOrderByIdAsc(purchase.getId());
        String purity = null;
        for (PurchaseItem item : items) {
            if (item.getPurity() != null && !item.getPurity().isBlank()) {
                purity = item.getPurity();
                break;
            }
        }
        if (purity == null) purity = "995";

        LocalDate  date      = purchase.getPurchaseDate() != null
                ? purchase.getPurchaseDate().toLocalDate() : LocalDate.now();
        String     voucherNo = purchase.getInvoiceNumber();

        BigDecimal perOz = purchase.getAgreedPremiumDiscount() != null
                ? purchase.getAgreedPremiumDiscount() : BigDecimal.ZERO;
        String discPremLabel = perOz.compareTo(BigDecimal.ZERO) >= 0 ? "PREM" : "DISC";
        String weightStr     = formatWeight(purchase.getPureWeightGrams());

        PartyLedgerEntry e = newEntry(party, "PUR", voucherNo, date,
                purchase.getBusinessId(), purchase.getBranchId());
        e.setNarration(String.format("UNFIXED PURCHASE %s %s @%s$ %s — AED DEFERRED",
                weightStr, purity, perOz.abs().toPlainString(), discPremLabel));
        // AED price is deferred — post ZERO AED, metal credit only
        e.setAedDebit(BigDecimal.ZERO);
        e.setMetalCredit(purchase.getPureWeightGrams());
        e.setMetalType("GOLD");
        e.setPurity(purity);
        e.setReferenceId(purchase.getId());
        e.setReferenceType("PURCHASE");
        ledgerRepo.save(e);
    }

    // ═══════════════════════════════════════════════════════════════
    //  RECEIPT ENTRY  (party pays you → reduces their credit balance)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public PartyLedgerEntry postReceiptEntry(Long partyId, String partyName,
                                              BigDecimal amount,
                                              String receivedFrom,
                                              LocalDate date,
                                              String createdBy,
                                              Long businessId, Long branchId) {
        String voucherNo = generateVoucherNumber(businessId, "REC");
        PartyLedgerEntry e = new PartyLedgerEntry();
        e.setBusinessId(businessId);
        e.setBranchId(branchId);
        e.setPartyId(partyId);
        e.setPartyName(partyName);
        e.setVoucherType("REC");
        e.setVoucherNumber(voucherNo);
        e.setVoucherDate(date != null ? date : LocalDate.now());
        e.setNarration("RECVD " + formatAed(amount) + " AED FROM " + partyName.toUpperCase());
        // Receipt = party paid you → reduces their outstanding (aedDebit reduces Σcredit−Σdebit)
        e.setAedDebit(amount);
        e.setMetalDebit(0.0);
        e.setMetalCredit(0.0);
        e.setReferenceType("RECEIPT");
        e.setCreatedBy(createdBy);
        return ledgerRepo.save(e);
    }

    // ═══════════════════════════════════════════════════════════════
    //  PAYMENT ENTRY  (you pay party → reduces your debit balance)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public PartyLedgerEntry postPaymentEntry(Long partyId, String partyName,
                                              BigDecimal amount,
                                              String paidTo,
                                              LocalDate date,
                                              String createdBy,
                                              Long businessId, Long branchId) {
        String voucherNo = generateVoucherNumber(businessId, "PAY");
        PartyLedgerEntry e = new PartyLedgerEntry();
        e.setBusinessId(businessId);
        e.setBranchId(branchId);
        e.setPartyId(partyId);
        e.setPartyName(partyName);
        e.setVoucherType("PAY");
        e.setVoucherNumber(voucherNo);
        e.setVoucherDate(date != null ? date : LocalDate.now());
        e.setNarration("PAID " + formatAed(amount) + " AED TO " + partyName.toUpperCase());
        // Payment = you paid party → reduces their credit balance toward 0 (aedDebit reduces Σcredit−Σdebit)
        e.setAedDebit(amount);
        e.setMetalDebit(0.0);
        e.setMetalCredit(0.0);
        e.setReferenceType("PAYMENT");
        e.setCreatedBy(createdBy);
        return ledgerRepo.save(e);
    }

    // ═══════════════════════════════════════════════════════════════
    //  METAL RECEIPT ENTRY  (weight-only, no AED — metal received from party)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public PartyLedgerEntry postMetalReceiptEntry(Long partyId, String partyName,
                                                  Double weightGrams, String metalType,
                                                  String purity, String notes,
                                                  LocalDate date, String createdBy,
                                                  Long businessId, Long branchId) {
        String voucherNo = generateVoucherNumber(businessId, "MRC");
        PartyLedgerEntry e = new PartyLedgerEntry();
        e.setBusinessId(businessId);
        e.setBranchId(branchId);
        e.setPartyId(partyId);
        e.setPartyName(partyName);
        e.setVoucherType("MRC");
        e.setVoucherNumber(voucherNo);
        e.setVoucherDate(date != null ? date : LocalDate.now());
        e.setNarration("METAL RCVD " + formatWeight(weightGrams) + " "
                + (purity != null ? purity + " " : "")
                + (metalType != null ? metalType : "GOLD")
                + " FROM " + partyName.toUpperCase()
                + (notes != null && !notes.isBlank() ? " | " + notes : ""));
        // MRC — weight-only: NEVER touch AED columns
        // Metal Receipt = we deliver metal TO the party (e.g. to settle a purchase obligation).
        // Metal physically leaves our vault → metalDebit.  This reduces the outstanding metalCredit
        // balance that the original Purchase posted, driving the running balance back toward zero.
        e.setAedDebit(BigDecimal.ZERO);
        e.setAedCredit(BigDecimal.ZERO);
        e.setMetalDebit(weightGrams);            // metal leaves our vault → DR
        e.setMetalCredit(0.0);
        e.setMetalType(metalType != null ? metalType : "GOLD");
        e.setPurity(purity);
        e.setReferenceType("METAL_RECEIPT");
        e.setCreatedBy(createdBy);
        return ledgerRepo.save(e);
    }

    // ═══════════════════════════════════════════════════════════════
    //  METAL PAYMENT ENTRY  (weight-only, no AED — metal given to party)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public PartyLedgerEntry postMetalPaymentEntry(Long partyId, String partyName,
                                                   Double weightGrams, String metalType,
                                                   String purity, String notes,
                                                   LocalDate date, String createdBy,
                                                   Long businessId, Long branchId) {
        String voucherNo = generateVoucherNumber(businessId, "MPY");
        PartyLedgerEntry e = new PartyLedgerEntry();
        e.setBusinessId(businessId);
        e.setBranchId(branchId);
        e.setPartyId(partyId);
        e.setPartyName(partyName);
        e.setVoucherType("MPY");
        e.setVoucherNumber(voucherNo);
        e.setVoucherDate(date != null ? date : LocalDate.now());
        e.setNarration("METAL PAID " + formatWeight(weightGrams) + " "
                + (purity != null ? purity + " " : "")
                + (metalType != null ? metalType : "GOLD")
                + " TO " + partyName.toUpperCase()
                + (notes != null && !notes.isBlank() ? " | " + notes : ""));
        // MPY — weight-only: NEVER touch AED columns
        // Metal Payment = party delivers metal TO us (e.g. to settle a sale obligation).
        // Metal physically enters our vault → metalCredit.  This reduces the outstanding metalDebit
        // balance that the original Sale posted, driving the running balance back toward zero.
        e.setAedDebit(BigDecimal.ZERO);
        e.setAedCredit(BigDecimal.ZERO);
        e.setMetalDebit(0.0);
        e.setMetalCredit(weightGrams);            // metal enters our vault → CR
        e.setMetalType(metalType != null ? metalType : "GOLD");
        e.setPurity(purity);
        e.setReferenceType("METAL_PAYMENT");
        e.setCreatedBy(createdBy);
        return ledgerRepo.save(e);
    }

    // ═══════════════════════════════════════════════════════════════
    //  GENERAL JOURNAL ENTRY  (GJV — manual correction/adjustment)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public PartyLedgerEntry postGeneralJournalEntry(Long partyId, String partyName,
                                                    BigDecimal aedDebit, BigDecimal aedCredit,
                                                    Double metalDebit, Double metalCredit,
                                                    String metalType, String purity,
                                                    String reason,
                                                    LocalDate date, String createdBy,
                                                    Long businessId, Long branchId) {
        // Normalise nulls to zero
        if (aedDebit    == null) aedDebit    = BigDecimal.ZERO;
        if (aedCredit   == null) aedCredit   = BigDecimal.ZERO;
        if (metalDebit  == null) metalDebit  = 0.0;
        if (metalCredit == null) metalCredit = 0.0;

        // Validate: reason is mandatory for GJV
        if (reason == null || reason.isBlank())
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Reason/narration is required for a Journal entry.");

        // Validate: at least one non-zero value
        boolean hasValue = aedDebit.compareTo(BigDecimal.ZERO) > 0
                || aedCredit.compareTo(BigDecimal.ZERO) > 0
                || metalDebit > 0 || metalCredit > 0;
        if (!hasValue)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Journal entry must have at least one non-zero value.");

        // Validate: cannot have both AED debit and AED credit
        if (aedDebit.compareTo(BigDecimal.ZERO) > 0 && aedCredit.compareTo(BigDecimal.ZERO) > 0)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Cannot have both AED debit and AED credit in the same journal entry.");

        // Validate: cannot have both metal debit and metal credit
        if (metalDebit > 0 && metalCredit > 0)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Cannot have both metal debit and metal credit in the same journal entry.");

        String voucherNo = generateVoucherNumber(businessId, "GJV");
        PartyLedgerEntry e = new PartyLedgerEntry();
        e.setBusinessId(businessId);
        e.setBranchId(branchId);
        e.setPartyId(partyId);
        e.setPartyName(partyName);
        e.setVoucherType("GJV");
        e.setVoucherNumber(voucherNo);
        e.setVoucherDate(date != null ? date : LocalDate.now());
        e.setNarration("JOURNAL: " + reason.trim());
        e.setAedDebit(aedDebit);
        e.setAedCredit(aedCredit);
        e.setMetalDebit(metalDebit);
        e.setMetalCredit(metalCredit);
        if (metalType != null && !metalType.isBlank()) e.setMetalType(metalType);
        if (purity    != null && !purity.isBlank())    e.setPurity(purity);
        e.setReferenceType("JOURNAL");
        e.setCreatedBy(createdBy);
        return ledgerRepo.save(e);
    }

    // ═══════════════════════════════════════════════════════════════
    //  FIX ENTRY  (PF for Purchase Fix, SF for Sale Fix)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void postFixEntry(MetalFix fix) {
        PartyLedgerEntry e = new PartyLedgerEntry();
        e.setBusinessId(fix.getBusinessId());
        e.setBranchId(fix.getBranchId());
        e.setPartyId(fix.getPartyId());
        e.setPartyName(fix.getPartyName());
        e.setVoucherType("PURCHASE_FIX".equals(fix.getFixType()) ? "PF" : "SF");
        e.setVoucherNumber(fix.getFixNumber());
        e.setVoucherDate(fix.getCreatedAt() != null
                ? fix.getCreatedAt().toLocalDate() : LocalDate.now());

        BigDecimal dp = fix.getDiscountPremium() != null ? fix.getDiscountPremium() : BigDecimal.ZERO;
        String     discPremLabel = dp.compareTo(BigDecimal.ZERO) >= 0 ? "PREM" : "DISC";
        String     weightStr     = formatWeight(fix.getWeightGrams());
        e.setNarration(String.format("%s %s %s @%s$ %s",
                "SALE_FIX".equals(fix.getFixType()) ? "SALE" : "PURCHASE",
                weightStr,
                fix.getPurity() != null ? fix.getPurity() : "",
                dp.toPlainString(),
                discPremLabel));

        boolean isSaleFix = "SALE_FIX".equals(fix.getFixType());
        if (isSaleFix) {
            e.setAedCredit(fix.getTotalAed());     // party will owe you
            e.setMetalDebit(fix.getWeightGrams()); // you're committing metal
        } else {
            e.setAedDebit(fix.getTotalAed());        // you will owe party
            e.setMetalCredit(fix.getWeightGrams());  // party is committing metal
        }

        e.setMetalType(fix.getMetalType());
        e.setPurity(fix.getPurity());
        e.setReferenceId(fix.getId());
        e.setReferenceType("FIX");
        e.setCreatedBy(fix.getCreatedBy());
        ledgerRepo.save(e);
    }

    // ═══════════════════════════════════════════════════════════════
    //  UNFIXED SALE FIX ENTRY  (SF — AED credit only; metal was already posted at SAL time)
    //
    //  Lifecycle:  UNFIXED_AT_TRADE SAL entry posted metalDebit.
    //              This SF entry posts the now-known AED credit.
    //              No metal movement at all — would double-count if posted again.
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void postUnfixedSaleFixEntry(MetalFix fix, String parentRef) {
        PartyLedgerEntry e = new PartyLedgerEntry();
        e.setBusinessId(fix.getBusinessId());
        e.setBranchId(fix.getBranchId());
        e.setPartyId(fix.getPartyId());
        e.setPartyName(fix.getPartyName());
        e.setVoucherType("SF");
        e.setVoucherNumber(fix.getFixNumber());
        e.setVoucherDate(fix.getFixedDate() != null ? fix.getFixedDate() : LocalDate.now());

        BigDecimal dp = fix.getDiscountPremium() != null ? fix.getDiscountPremium() : BigDecimal.ZERO;
        String discPremLabel  = dp.compareTo(BigDecimal.ZERO) >= 0 ? "PREM" : "DISC";
        String weightStr      = formatWeight(fix.getWeightGrams());
        BigDecimal effRate    = fix.getEffectiveRate() != null ? fix.getEffectiveRate() : fix.getTransactionRate();

        e.setNarration(String.format("FIX SALE %s %s @%s$ %s | REF:%s",
                weightStr,
                fix.getPurity() != null ? fix.getPurity() : "",
                effRate.toPlainString(),
                discPremLabel,
                parentRef != null ? parentRef : ""));

        // AED credit — party now owes the fixed price
        // Metal = ZERO: metal was already debited in the UNFIXED_AT_TRADE SAL entry
        e.setAedCredit(fix.getTotalAed());
        e.setAedDebit(BigDecimal.ZERO);
        e.setMetalDebit(0.0);
        e.setMetalCredit(0.0);
        e.setMetalType(fix.getMetalType());
        e.setPurity(fix.getPurity());
        e.setReferenceId(fix.getId());
        e.setReferenceType("FIX");
        e.setCreatedBy(fix.getCreatedBy());
        ledgerRepo.save(e);
    }

    // ═══════════════════════════════════════════════════════════════
    //  UNFIXED PURCHASE FIX ENTRY  (PF — AED debit only; metal was already posted at PUR time)
    //
    //  Lifecycle:  UNFIXED_AT_TRADE PUR entry posted metalCredit.
    //              This PF entry posts the now-known AED debit.
    //              No metal movement at all — would double-count if posted again.
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void postUnfixedPurchaseFixEntry(MetalFix fix, String parentRef) {
        PartyLedgerEntry e = new PartyLedgerEntry();
        e.setBusinessId(fix.getBusinessId());
        e.setBranchId(fix.getBranchId());
        e.setPartyId(fix.getPartyId());
        e.setPartyName(fix.getPartyName());
        e.setVoucherType("PF");
        e.setVoucherNumber(fix.getFixNumber());
        e.setVoucherDate(fix.getFixedDate() != null ? fix.getFixedDate() : LocalDate.now());

        BigDecimal dp = fix.getDiscountPremium() != null ? fix.getDiscountPremium() : BigDecimal.ZERO;
        String discPremLabel  = dp.compareTo(BigDecimal.ZERO) >= 0 ? "PREM" : "DISC";
        String weightStr      = formatWeight(fix.getWeightGrams());
        BigDecimal effRate    = fix.getEffectiveRate() != null ? fix.getEffectiveRate() : fix.getTransactionRate();

        e.setNarration(String.format("FIX PURCHASE %s %s @%s$ %s | REF:%s",
                weightStr,
                fix.getPurity() != null ? fix.getPurity() : "",
                effRate.toPlainString(),
                discPremLabel,
                parentRef != null ? parentRef : ""));

        // AED debit — we now owe the party the fixed price
        // Metal = ZERO: metal was already credited in the UNFIXED_AT_TRADE PUR entry
        e.setAedDebit(fix.getTotalAed());
        e.setAedCredit(BigDecimal.ZERO);
        e.setMetalDebit(0.0);
        e.setMetalCredit(0.0);
        e.setMetalType(fix.getMetalType());
        e.setPurity(fix.getPurity());
        e.setReferenceId(fix.getId());
        e.setReferenceType("FIX");
        e.setCreatedBy(fix.getCreatedBy());
        ledgerRepo.save(e);
    }

    // ═══════════════════════════════════════════════════════════════
    //  SETTLEMENT ENTRY  (SFX — closes the fix position)
    //  TODO: If settlement vouchers ever need to be split by fixType
    //        (e.g. "PSX" for Purchase settlements, "SSX" for Sale settlements),
    //        replace the "SFX" literal below with the same conditional pattern
    //        used in postFixEntry(). Until then, all settlements share "SFX".
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void postSettlementEntry(FixSettlement settlement, MetalFix fix) {
        PartyLedgerEntry e = new PartyLedgerEntry();
        e.setBusinessId(settlement.getBusinessId());
        e.setBranchId(settlement.getBranchId());
        e.setPartyId(settlement.getPartyId());
        e.setPartyName(settlement.getPartyName());
        e.setVoucherType("SFX"); // intentionally not split by fixType — see TODO above
        e.setVoucherNumber(settlement.getSettlementNumber());
        e.setVoucherDate(settlement.getSettlementDate());

        // Net AED difference = settlementAed − originalAed
        //   > 0  → rate went UP   (settlement is higher than the HPF estimate)
        //   < 0  → rate went DOWN (settlement is lower than the HPF estimate)
        //   = 0  → no AED adjustment needed (only metal position closes)
        BigDecimal netAedDiff = settlement.getSettlementAed()
                .subtract(fix.getTotalAed())
                .setScale(2, RoundingMode.HALF_UP);
        int sign = netAedDiff.compareTo(BigDecimal.ZERO); // +1, 0, -1

        String rateInfo = settlement.getSettlementMarketRate() != null
                ? String.format("mkt %.2f → eff %.2f",
                        settlement.getSettlementMarketRate().doubleValue(),
                        settlement.getFixedRate().doubleValue())
                : String.format("eff %.2f", settlement.getFixedRate().doubleValue());

        e.setNarration(String.format("SFX %s %s %.3fg @ %s; net %s AED %.2f",
                "SALE_FIX".equals(fix.getFixType()) ? "Sale" : "Purchase",
                fix.getMetalType(),
                settlement.getWeightGrams(),
                rateInfo,
                sign >= 0 ? "CR+" : "DR-",
                netAedDiff.abs().doubleValue()));

        boolean isSaleFix = "SALE_FIX".equals(fix.getFixType());
        if (isSaleFix) {
            // Close metal position: HPF debited metal (you committed to deliver), SFX credits it
            e.setMetalCredit(settlement.getWeightGrams());
            // AED adjustment — only the NET difference vs what HPF already posted
            if (sign > 0) {
                // Rate went up → party owes you MORE → additional AED credit
                e.setAedCredit(netAedDiff);
            } else if (sign < 0) {
                // Rate went down → party owes you LESS → AED debit to offset HPF excess credit
                e.setAedDebit(netAedDiff.abs());
            }
            // sign == 0: no AED entry, metal close only
        } else {
            // PURCHASE_FIX
            // Close metal position: HPF credited metal (party committed to deliver), SFX debits it
            e.setMetalDebit(settlement.getWeightGrams());
            // AED adjustment — only the NET difference vs what HPF already posted
            if (sign > 0) {
                // Rate went up → you owe party MORE → additional AED debit
                e.setAedDebit(netAedDiff);
            } else if (sign < 0) {
                // Rate went down → you owe party LESS → AED credit to offset HPF excess debit
                e.setAedCredit(netAedDiff.abs());
            }
            // sign == 0: no AED entry, metal close only
        }

        e.setMetalType(fix.getMetalType());
        e.setPurity(fix.getPurity());
        e.setReferenceId(settlement.getId());
        e.setReferenceType("SETTLEMENT");
        e.setCreatedBy(settlement.getSettledBy());
        ledgerRepo.save(e);
    }

    // ═══════════════════════════════════════════════════════════════
    //  STATEMENT  (with running balance)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns all ledger entries for a party in the given date range,
     * with per-row running balances computed in Java (not stored in DB).
     *
     * Running balance = Σ(aedCredit) − Σ(aedDebit)  (all entries from the
     * beginning of time, not just the date range).  The opening balance is
     * shown as a synthetic first row so the statement balances correctly.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPartyStatement(Long businessId, Long partyId,
                                                   LocalDate from, LocalDate to) {
        // Opening balance: sum of ALL entries before the requested date range
        List<PartyLedgerEntry> allEntries =
                ledgerRepo.findByBusinessIdAndPartyIdOrderByVoucherDateAscCreatedAtAsc(
                        businessId, partyId);

        BigDecimal openingAedBalance   = BigDecimal.ZERO;
        double     openingMetalBalance = 0.0;

        List<PartyLedgerEntry> rangeEntries = new ArrayList<>();

        for (PartyLedgerEntry e : allEntries) {
            if (from != null && e.getVoucherDate().isBefore(from)) {
                // Before range → contributes to opening balance only
                openingAedBalance   = openingAedBalance.add(e.getAedCredit()).subtract(e.getAedDebit());
                openingMetalBalance = openingMetalBalance + e.getMetalDebit() - e.getMetalCredit();
            } else if (to == null || !e.getVoucherDate().isAfter(to)) {
                rangeEntries.add(e);
            }
        }

        // Build rows with running balance
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal runningAed   = openingAedBalance;
        double     runningMetal = openingMetalBalance;
        BigDecimal totalDebit   = BigDecimal.ZERO;
        BigDecimal totalCredit  = BigDecimal.ZERO;
        double     totalMetalDr = 0.0;
        double     totalMetalCr = 0.0;

        for (PartyLedgerEntry e : rangeEntries) {
            runningAed   = runningAed.add(e.getAedCredit()).subtract(e.getAedDebit());
            runningMetal = runningMetal + e.getMetalDebit() - e.getMetalCredit();
            totalDebit   = totalDebit.add(e.getAedDebit());
            totalCredit  = totalCredit.add(e.getAedCredit());
            totalMetalDr += e.getMetalDebit();
            totalMetalCr += e.getMetalCredit();

            Map<String, Object> row = toEntryMap(e);
            row.put("aedRunningBalance",   runningAed.abs());
            row.put("aedBalanceType",      runningAed.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DR");
            row.put("metalRunningBalance", Math.abs(runningMetal));
            row.put("metalBalanceType",    runningMetal >= 0 ? "DR" : "CR");
            rows.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("partyId",              partyId);
        result.put("dateFrom",             from != null ? from.toString() : null);
        result.put("dateTo",               to   != null ? to.toString()   : null);
        result.put("openingAedBalance",    openingAedBalance.abs());
        result.put("openingAedType",       openingAedBalance.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DR");
        result.put("openingMetalBalance",  Math.abs(openingMetalBalance));
        result.put("openingMetalType",     openingMetalBalance >= 0 ? "DR" : "CR");
        result.put("entries",              rows);
        result.put("totalAedDebit",        totalDebit);
        result.put("totalAedCredit",       totalCredit);
        result.put("totalMetalDebit",      totalMetalDr);
        result.put("totalMetalCredit",     totalMetalCr);

        BigDecimal closingAed = openingAedBalance.add(totalCredit).subtract(totalDebit);
        result.put("closingAedBalance",    closingAed.abs());
        result.put("closingAedType",       closingAed.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DR");
        double closingMetal = openingMetalBalance + totalMetalDr - totalMetalCr;
        result.put("closingMetalBalance",  Math.abs(closingMetal));
        result.put("closingMetalType",     closingMetal >= 0 ? "DR" : "CR");
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  BALANCE  (current totals)
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Map<String, Object> getPartyBalance(Long businessId, Long partyId) {
        BigDecimal totalCredit = ledgerRepo.sumAedCreditByBusinessIdAndPartyId(businessId, partyId);
        BigDecimal totalDebit  = ledgerRepo.sumAedDebitByBusinessIdAndPartyId(businessId, partyId);
        Double     metalDr     = ledgerRepo.sumMetalDebitByBusinessIdAndPartyId(businessId, partyId);
        Double     metalCr     = ledgerRepo.sumMetalCreditByBusinessIdAndPartyId(businessId, partyId);

        if (totalCredit == null) totalCredit = BigDecimal.ZERO;
        if (totalDebit  == null) totalDebit  = BigDecimal.ZERO;
        if (metalDr     == null) metalDr     = 0.0;
        if (metalCr     == null) metalCr     = 0.0;

        BigDecimal aedNet   = totalCredit.subtract(totalDebit);
        double     metalNet = metalDr - metalCr;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("partyId",       partyId);
        m.put("aedBalance",    aedNet.abs());
        m.put("aedType",       aedNet.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DR");
        m.put("metalBalance",  Math.abs(metalNet));
        m.put("metalType",     metalNet >= 0 ? "DR" : "CR");
        return m;
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private PartyLedgerEntry newEntry(Supplier party, String type, String voucherNo,
                                      LocalDate date, Long businessId, Long branchId) {
        PartyLedgerEntry e = new PartyLedgerEntry();
        e.setBusinessId(businessId);
        e.setBranchId(branchId);
        e.setPartyId(party.getId());
        e.setPartyName(party.getName());
        e.setVoucherType(type);
        e.setVoucherNumber(voucherNo);
        e.setVoucherDate(date);
        e.setAedDebit(BigDecimal.ZERO);
        e.setAedCredit(BigDecimal.ZERO);
        e.setMetalDebit(0.0);
        e.setMetalCredit(0.0);
        return e;
    }

    private String formatWeight(double grams) {
        if (grams >= 1000) {
            return String.format("%.2fKG", grams / 1000.0);
        }
        return String.format("%.2fG", grams);
    }

    private String formatAed(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }

    public Map<String, Object> toEntryMap(PartyLedgerEntry e) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",            e.getId());
        m.put("partyId",       e.getPartyId());
        m.put("partyName",     e.getPartyName());
        m.put("voucherType",   e.getVoucherType());
        m.put("voucherNumber", e.getVoucherNumber());
        m.put("voucherDate",   e.getVoucherDate() != null ? e.getVoucherDate().toString() : null);
        m.put("narration",     e.getNarration());
        m.put("aedDebit",      e.getAedDebit());
        m.put("aedCredit",     e.getAedCredit());
        m.put("metalDebit",    e.getMetalDebit());
        m.put("metalCredit",   e.getMetalCredit());
        m.put("metalType",     e.getMetalType());
        m.put("purity",        e.getPurity());
        m.put("referenceId",   e.getReferenceId());
        m.put("referenceType", e.getReferenceType());
        m.put("branchId",      e.getBranchId());
        m.put("createdBy",     e.getCreatedBy());
        m.put("createdAt",     e.getCreatedAt() != null ? e.getCreatedAt().format(fmt) : null);
        return m;
    }
}
