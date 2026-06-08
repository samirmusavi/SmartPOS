package com.business.managementsystem.service;

import com.business.managementsystem.model.CashDaySheet;
import com.business.managementsystem.model.CashEntry;
import com.business.managementsystem.model.Expense;
import com.business.managementsystem.model.Purchase;
import com.business.managementsystem.model.Return;
import com.business.managementsystem.model.SaleTransaction;
import com.business.managementsystem.repository.CashDaySheetRepository;
import com.business.managementsystem.repository.CashEntryRepository;
import com.business.managementsystem.repository.ExpenseRepository;
import com.business.managementsystem.repository.PurchaseRepository;
import com.business.managementsystem.repository.ReturnRepository;
import com.business.managementsystem.repository.SaleTransactionRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
public class CashSheetService {

    private final CashDaySheetRepository    daySheetRepo;
    private final CashEntryRepository       entryRepo;
    private final SaleTransactionRepository txRepo;
    private final ExpenseRepository         expenseRepo;
    private final ReturnRepository          returnRepo;
    private final PurchaseRepository        purchaseRepo;

    public CashSheetService(CashDaySheetRepository daySheetRepo,
                            CashEntryRepository entryRepo,
                            SaleTransactionRepository txRepo,
                            ExpenseRepository expenseRepo,
                            ReturnRepository returnRepo,
                            PurchaseRepository purchaseRepo) {
        this.daySheetRepo = daySheetRepo;
        this.entryRepo    = entryRepo;
        this.txRepo       = txRepo;
        this.expenseRepo  = expenseRepo;
        this.returnRepo   = returnRepo;
        this.purchaseRepo = purchaseRepo;
    }

    // ── Get or create the day sheet for a given date ──────────────
    @Transactional
    public CashDaySheet getOrCreateDaySheet(Long businessId, Long branchId, LocalDate date) {
        return daySheetRepo
                .findByBusinessIdAndBranchIdAndSheetDate(businessId, branchId, date)
                .orElseGet(() -> {
                    // Auto-fill opening balance from yesterday's closing
                    BigDecimal opening = BigDecimal.ZERO;
                    List<CashDaySheet> prev = daySheetRepo.findLatestSheetBefore(businessId, branchId, date);
                    if (!prev.isEmpty()) {
                        opening = computeClosingBalance(businessId, branchId, prev.get(0).getSheetDate());
                    }
                    CashDaySheet sheet = new CashDaySheet(businessId, branchId, date, opening);
                    return daySheetRepo.save(sheet);
                });
    }

    // ── Compute closing balance for a date (no DB write) ─────────
    private BigDecimal computeClosingBalance(Long businessId, Long branchId, LocalDate date) {
        CashDaySheet sheet = daySheetRepo
                .findByBusinessIdAndBranchIdAndSheetDate(businessId, branchId, date)
                .orElse(null);
        if (sheet == null) return BigDecimal.ZERO;

        BigDecimal balance = sheet.getOpeningBalance();
        List<CashEntry> entries = entryRepo
                .findByBusinessIdAndBranchIdAndSheetDateOrderBySortOrderAscCreatedAtAsc(
                        businessId, branchId, date);
        for (CashEntry e : entries) {
            balance = balance.add(e.getAmountIn()).subtract(e.getAmountOut());
        }
        return balance.setScale(2, RoundingMode.HALF_UP);
    }

    // ── Get all entries with running balance ──────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDayEntries(Long businessId, Long branchId, LocalDate date) {
        CashDaySheet sheet = daySheetRepo
                .findByBusinessIdAndBranchIdAndSheetDate(businessId, branchId, date)
                .orElse(null);
        BigDecimal opening = sheet != null ? sheet.getOpeningBalance() : BigDecimal.ZERO;

        List<CashEntry> entries = entryRepo
                .findByBusinessIdAndBranchIdAndSheetDateOrderBySortOrderAscCreatedAtAsc(
                        businessId, branchId, date);

        return addRunningBalance(entries, opening);
    }

    // ── Sync POS data into cash entries for the given date ───────
    @Transactional
    public void syncFromPOS(Long businessId, Long branchId, LocalDate date) {
        // Ensure day sheet exists
        getOrCreateDaySheet(businessId, branchId, date);

        // Delete previous synced rows so we start fresh
        entryRepo.deleteSyncedEntriesByDate(businessId, branchId, date,
                List.of(CashEntry.EntryType.SALE,
                        CashEntry.EntryType.EXPENSE,
                        CashEntry.EntryType.RETURN,
                        CashEntry.EntryType.PURCHASE));

        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to   = date.plusDays(1).atStartOfDay();
        int seq = 0;

        // ── Cash sales ────────────────────────────────────────────
        List<SaleTransaction> sales = txRepo
                .findCashTransactionsByBranchAndDateRange(businessId, branchId, from, to);
        for (SaleTransaction tx : sales) {
            String party = (tx.getCustomerName() != null && !tx.getCustomerName().isBlank())
                    ? tx.getCustomerName() : "Walk-in Customer";
            String desc  = tx.getReceiptNumber() != null ? tx.getReceiptNumber() : "";
            if (tx.getCashierName() != null && !tx.getCashierName().isBlank())
                desc += " | " + tx.getCashierName();

            entryRepo.save(new CashEntry(
                    businessId, branchId, date,
                    party, desc,
                    tx.getTotalAmount(), BigDecimal.ZERO,
                    CashEntry.EntryType.SALE, tx.getId(),
                    seq++, null));
        }

        // ── One-time expenses ─────────────────────────────────────
        List<Expense> expenses = expenseRepo
                .findByBusinessIdAndBranchIdAndExpenseDateAndRecurringFalse(businessId, branchId, date);
        for (Expense e : expenses) {
            String desc = e.getTitle();
            if (e.getDescription() != null && !e.getDescription().isBlank())
                desc += " — " + e.getDescription();

            entryRepo.save(new CashEntry(
                    businessId, branchId, date,
                    e.getCategory(), desc,
                    BigDecimal.ZERO, e.getAmount(),
                    CashEntry.EntryType.EXPENSE, e.getId(),
                    seq++, null));
        }

        // ── Cash refunds (returns) ────────────────────────────────
        List<Return> returns = returnRepo
                .findByBranchAndDateRange(businessId, branchId, from, to);
        for (Return r : returns) {
            String desc = r.getInvoiceNumber() != null
                    ? "Return: " + r.getInvoiceNumber() : "Sales Return";

            entryRepo.save(new CashEntry(
                    businessId, branchId, date,
                    r.getProductName(), desc,
                    BigDecimal.ZERO, r.getRefundAmount(),
                    CashEntry.EntryType.RETURN, r.getId(),
                    seq++, null));
        }

        // ── Cash purchases (payment out to supplier) ──────────────
        List<Purchase> purchases = purchaseRepo
                .findCashPurchasesByBranchAndDateRange(businessId, branchId, from, to);
        for (Purchase pur : purchases) {
            String party = pur.getSupplierName() != null ? pur.getSupplierName() : "Supplier";
            String desc  = pur.getInvoiceNumber() != null ? pur.getInvoiceNumber() : "Purchase";

            entryRepo.save(new CashEntry(
                    businessId, branchId, date,
                    party, desc,
                    BigDecimal.ZERO, pur.getTotalAmount(),
                    CashEntry.EntryType.PURCHASE, pur.getId(),
                    seq++, null));
        }
    }

    // ── Add a manual entry (auto-detect type) ────────────────────
    @Transactional
    public CashEntry addManualEntry(Long businessId, Long branchId, LocalDate date,
                                    String partyName, String description,
                                    BigDecimal amountIn, BigDecimal amountOut,
                                    String createdBy) {
        return addManualEntry(businessId, branchId, date,
                partyName, description, amountIn, amountOut, createdBy, null);
    }

    // ── Add a manual entry (explicit type override) ───────────────
    @Transactional
    public CashEntry addManualEntry(Long businessId, Long branchId, LocalDate date,
                                    String partyName, String description,
                                    BigDecimal amountIn, BigDecimal amountOut,
                                    String createdBy, CashEntry.EntryType explicitType) {
        getOrCreateDaySheet(businessId, branchId, date);
        int nextOrder = entryRepo.findMaxSortOrderByDate(businessId, branchId, date) + 1;

        CashEntry.EntryType type;
        if (explicitType != null) {
            type = explicitType;
        } else {
            type = (amountOut != null && amountOut.compareTo(BigDecimal.ZERO) > 0
                    && (amountIn == null || amountIn.compareTo(BigDecimal.ZERO) == 0))
                    ? CashEntry.EntryType.MANUAL_OUT
                    : CashEntry.EntryType.MANUAL_IN;
        }

        return entryRepo.save(new CashEntry(
                businessId, branchId, date,
                partyName, description,
                amountIn  != null ? amountIn  : BigDecimal.ZERO,
                amountOut != null ? amountOut : BigDecimal.ZERO,
                type, null, nextOrder, createdBy));
    }

    // ── Edit a manual entry ───────────────────────────────────────
    @Transactional
    public CashEntry updateManualEntry(Long id, String partyName, String description,
                                       BigDecimal amountIn, BigDecimal amountOut) {
        CashEntry entry = entryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry not found: " + id));

        if (entry.getEntryType() != CashEntry.EntryType.MANUAL_IN
                && entry.getEntryType() != CashEntry.EntryType.MANUAL_OUT
                && entry.getEntryType() != CashEntry.EntryType.HAND_LOAN_IN
                && entry.getEntryType() != CashEntry.EntryType.HAND_LOAN_OUT) {
            throw new RuntimeException("Only manual and hand loan entries can be edited.");
        }

        entry.setPartyName(partyName);
        entry.setDescription(description);
        entry.setAmountIn(amountIn  != null ? amountIn  : BigDecimal.ZERO);
        entry.setAmountOut(amountOut != null ? amountOut : BigDecimal.ZERO);

        // Re-classify type based on updated amounts
        boolean isOut = entry.getAmountOut().compareTo(BigDecimal.ZERO) > 0
                && entry.getAmountIn().compareTo(BigDecimal.ZERO) == 0;
        // (keep existing type if both or neither are > 0; it's an edge case)
        if (entry.getAmountOut().compareTo(BigDecimal.ZERO) > 0
                && entry.getAmountIn().compareTo(BigDecimal.ZERO) == 0) {
            // pure outflow
        } else if (entry.getAmountIn().compareTo(BigDecimal.ZERO) > 0
                && entry.getAmountOut().compareTo(BigDecimal.ZERO) == 0) {
            // pure inflow
        }

        return entryRepo.save(entry);
    }

    // ── Delete a manual entry ─────────────────────────────────────
    @Transactional
    public void deleteManualEntry(Long id) {
        CashEntry entry = entryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry not found: " + id));

        if (entry.getEntryType() != CashEntry.EntryType.MANUAL_IN
                && entry.getEntryType() != CashEntry.EntryType.MANUAL_OUT
                && entry.getEntryType() != CashEntry.EntryType.HAND_LOAN_IN
                && entry.getEntryType() != CashEntry.EntryType.HAND_LOAN_OUT) {
            throw new RuntimeException(
                    "Only manual and hand loan entries can be deleted. Synced entries are read-only.");
        }
        entryRepo.delete(entry);
    }

    // ── Update denomination counts / opening balance ───────────────
    @Transactional
    public CashDaySheet updateDaySheet(Long id,
                                       BigDecimal openingBalance,
                                       Boolean openingOverridden,
                                       Integer bundles1000, Integer bundles500,
                                       Integer bundles200, Integer bundles100,
                                       Integer bundles50,  Integer bundles10,
                                       Integer bundles5,
                                       BigDecimal mixNotes, BigDecimal coins,
                                       BigDecimal handLoanTotal) {
        CashDaySheet sheet = daySheetRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Day sheet not found: " + id));

        if (openingBalance   != null) sheet.setOpeningBalance(openingBalance);
        if (openingOverridden != null) sheet.setOpeningOverridden(openingOverridden);
        if (bundles1000 != null) sheet.setBundles1000(bundles1000);
        if (bundles500  != null) sheet.setBundles500(bundles500);
        if (bundles200  != null) sheet.setBundles200(bundles200);
        if (bundles100  != null) sheet.setBundles100(bundles100);
        if (bundles50   != null) sheet.setBundles50(bundles50);
        if (bundles10   != null) sheet.setBundles10(bundles10);
        if (bundles5    != null) sheet.setBundles5(bundles5);
        if (mixNotes    != null) sheet.setMixNotes(mixNotes);
        if (coins       != null) sheet.setCoins(coins);
        if (handLoanTotal != null) sheet.setHandLoanTotal(handLoanTotal);

        return daySheetRepo.save(sheet);
    }

    // ── Full page response ────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getFullDayResponse(Long businessId, Long branchId, LocalDate date) {
        CashDaySheet sheet = getOrCreateDaySheetReadOnly(businessId, branchId, date);
        List<Map<String, Object>> entriesWithBalance = getDayEntries(businessId, branchId, date);

        BigDecimal opening   = sheet.getOpeningBalance();
        BigDecimal totalIn   = BigDecimal.ZERO;
        BigDecimal totalOut  = BigDecimal.ZERO;

        for (Map<String, Object> e : entriesWithBalance) {
            totalIn  = totalIn.add((BigDecimal) e.get("amountIn"));
            totalOut = totalOut.add((BigDecimal) e.get("amountOut"));
        }

        BigDecimal systemBalance = opening.add(totalIn).subtract(totalOut)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal manualCashTotal = computeManualCashTotal(sheet);
        BigDecimal handLoan  = sheet.getHandLoanTotal();
        BigDecimal excessShort = manualCashTotal.subtract(systemBalance.subtract(handLoan))
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> summary = new HashMap<>();
        summary.put("openingBalance",   opening);
        summary.put("totalIn",          totalIn.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalOut",         totalOut.setScale(2, RoundingMode.HALF_UP));
        summary.put("systemBalance",    systemBalance);
        summary.put("handLoanTotal",    handLoan);
        summary.put("manualCashTotal",  manualCashTotal);
        summary.put("excessShort",      excessShort);

        Map<String, Object> result = new HashMap<>();
        result.put("daySheet", toSheetMap(sheet));
        result.put("entries",  entriesWithBalance);
        result.put("summary",  summary);
        return result;
    }

    // Read-only variant — will NOT auto-create if missing
    @Transactional(readOnly = true)
    public CashDaySheet getOrCreateDaySheetReadOnly(Long businessId, Long branchId, LocalDate date) {
        return daySheetRepo
                .findByBusinessIdAndBranchIdAndSheetDate(businessId, branchId, date)
                .orElseGet(() -> new CashDaySheet(businessId, branchId, date, BigDecimal.ZERO));
    }

    // ── Manual cash count total from denomination fields ─────────
    public BigDecimal computeManualCashTotal(CashDaySheet sheet) {
        BigDecimal total = BigDecimal.ZERO;
        total = total.add(BigDecimal.valueOf((long) sheet.getBundles1000() * 1000));
        total = total.add(BigDecimal.valueOf((long) sheet.getBundles500()  * 500));
        total = total.add(BigDecimal.valueOf((long) sheet.getBundles200()  * 200));
        total = total.add(BigDecimal.valueOf((long) sheet.getBundles100()  * 100));
        total = total.add(BigDecimal.valueOf((long) sheet.getBundles50()   * 50));
        total = total.add(BigDecimal.valueOf((long) sheet.getBundles10()   * 10));
        total = total.add(BigDecimal.valueOf((long) sheet.getBundles5()    * 5));
        total = total.add(sheet.getMixNotes());
        total = total.add(sheet.getCoins());
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    // ── Attach running balance to each entry ──────────────────────
    private List<Map<String, Object>> addRunningBalance(List<CashEntry> entries,
                                                         BigDecimal opening) {
        List<Map<String, Object>> result = new ArrayList<>();
        BigDecimal running = opening;

        for (CashEntry e : entries) {
            running = running.add(e.getAmountIn()).subtract(e.getAmountOut())
                    .setScale(2, RoundingMode.HALF_UP);
            Map<String, Object> m = toEntryMap(e);
            m.put("runningBalance", running);
            result.add(m);
        }
        return result;
    }

    // ── Excel export ──────────────────────────────────────────────
    public byte[] generateExcel(Long businessId, Long branchId, LocalDate date) throws IOException {
        Map<String, Object> data = getFullDayResponse(businessId, branchId, date);
        CashDaySheet sheet = daySheetRepo
                .findByBusinessIdAndBranchIdAndSheetDate(businessId, branchId, date)
                .orElseGet(() -> new CashDaySheet(businessId, branchId, date, BigDecimal.ZERO));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries =
                (List<Map<String, Object>>) data.get("entries");
        @SuppressWarnings("unchecked")
        Map<String, Object> summary =
                (Map<String, Object>) data.get("summary");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // ── Styles ────────────────────────────────────────────
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle boldStyle = wb.createCellStyle();
            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            CellStyle amountStyle = wb.createCellStyle();
            DataFormat fmt = wb.createDataFormat();
            amountStyle.setDataFormat(fmt.getFormat("#,##0.00"));

            CellStyle amountBoldStyle = wb.createCellStyle();
            Font amtBoldFont = wb.createFont();
            amtBoldFont.setBold(true);
            amountBoldStyle.setFont(amtBoldFont);
            amountBoldStyle.setDataFormat(fmt.getFormat("#,##0.00"));

            // ── Sheet 1: Transaction Log ──────────────────────────
            Sheet txSheet = wb.createSheet("Transaction Log");
            txSheet.setColumnWidth(0, 800);
            txSheet.setColumnWidth(1, 6000);
            txSheet.setColumnWidth(2, 8000);
            txSheet.setColumnWidth(3, 4000);
            txSheet.setColumnWidth(4, 4000);
            txSheet.setColumnWidth(5, 5000);
            txSheet.setColumnWidth(6, 3000);

            int rowNum = 0;

            // Title row
            Row titleRow = txSheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Cash Sheet — " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            titleCell.setCellStyle(boldStyle);
            txSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
            rowNum++; // blank row

            // Opening balance row
            Row obRow = txSheet.createRow(rowNum++);
            obRow.createCell(0).setCellValue("—");
            obRow.createCell(1).setCellValue("Opening Balance");
            obRow.createCell(2).setCellValue("");
            obRow.createCell(3).setCellValue("");
            obRow.createCell(4).setCellValue("");

            Cell obBalCell = obRow.createCell(5);
            obBalCell.setCellValue(sheet.getOpeningBalance().doubleValue());
            obBalCell.setCellStyle(amountBoldStyle);
            rowNum++;

            // Header row
            Row hRow = txSheet.createRow(rowNum++);
            String[] headers = { "#", "Party / Description", "Details", "In (AED)", "Out (AED)", "Balance (AED)", "Type" };
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // Data rows
            int seq = 1;
            for (Map<String, Object> e : entries) {
                Row row = txSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(seq++);
                row.createCell(1).setCellValue(str(e.get("partyName")));
                row.createCell(2).setCellValue(str(e.get("description")));

                Cell inCell  = row.createCell(3);
                Cell outCell = row.createCell(4);
                Cell balCell = row.createCell(5);
                inCell.setCellStyle(amountStyle);
                outCell.setCellStyle(amountStyle);
                balCell.setCellStyle(amountStyle);

                BigDecimal in  = (BigDecimal) e.getOrDefault("amountIn",  BigDecimal.ZERO);
                BigDecimal out = (BigDecimal) e.getOrDefault("amountOut", BigDecimal.ZERO);
                BigDecimal bal = (BigDecimal) e.getOrDefault("runningBalance", BigDecimal.ZERO);
                if (in.compareTo(BigDecimal.ZERO)  > 0) inCell.setCellValue(in.doubleValue());
                if (out.compareTo(BigDecimal.ZERO) > 0) outCell.setCellValue(out.doubleValue());
                balCell.setCellValue(bal.doubleValue());
                row.createCell(6).setCellValue(str(e.get("entryType")));
            }

            rowNum++; // blank
            // Closing balance row
            Row closingRow = txSheet.createRow(rowNum++);
            Cell closingLabel = closingRow.createCell(1);
            closingLabel.setCellValue("CLOSING BALANCE");
            closingLabel.setCellStyle(boldStyle);
            Cell closingBal = closingRow.createCell(5);
            closingBal.setCellValue(((BigDecimal) summary.get("systemBalance")).doubleValue());
            closingBal.setCellStyle(amountBoldStyle);

            // ── Sheet 2: Summary ──────────────────────────────────
            Sheet sumSheet = wb.createSheet("Summary");
            sumSheet.setColumnWidth(0, 7000);
            sumSheet.setColumnWidth(1, 5000);

            int sr = 0;
            addSummaryRow(sumSheet, sr++, wb, boldStyle, amountStyle, "Opening Balance",   summary.get("openingBalance"));
            addSummaryRow(sumSheet, sr++, wb, boldStyle, amountStyle, "Total Cash In",      summary.get("totalIn"));
            addSummaryRow(sumSheet, sr++, wb, boldStyle, amountStyle, "Total Cash Out",     summary.get("totalOut"));
            addSummaryRow(sumSheet, sr++, wb, boldStyle, amountStyle, "System Balance",     summary.get("systemBalance"));
            addSummaryRow(sumSheet, sr++, wb, boldStyle, amountStyle, "Hand Loan Total",    summary.get("handLoanTotal"));
            addSummaryRow(sumSheet, sr++, wb, boldStyle, amountStyle, "Manual Cash Count",  summary.get("manualCashTotal"));
            addSummaryRow(sumSheet, sr++, wb, boldStyle, amountStyle, "Excess / Short",     summary.get("excessShort"));
            sr++; // blank
            // Denominations
            Row denomHeader = sumSheet.createRow(sr++);
            denomHeader.createCell(0).setCellValue("DENOMINATION COUNT");
            denomHeader.getCell(0).setCellStyle(boldStyle);
            addDenomRow(sumSheet, sr++, "1000 × " + sheet.getBundles1000(), sheet.getBundles1000() * 1000L);
            addDenomRow(sumSheet, sr++, "500  × " + sheet.getBundles500(),  sheet.getBundles500()  * 500L);
            addDenomRow(sumSheet, sr++, "200  × " + sheet.getBundles200(),  sheet.getBundles200()  * 200L);
            addDenomRow(sumSheet, sr++, "100  × " + sheet.getBundles100(),  sheet.getBundles100()  * 100L);
            addDenomRow(sumSheet, sr++, "50   × " + sheet.getBundles50(),   sheet.getBundles50()   * 50L);
            addDenomRow(sumSheet, sr++, "10   × " + sheet.getBundles10(),   sheet.getBundles10()   * 10L);
            addDenomRow(sumSheet, sr++, "5    × " + sheet.getBundles5(),    sheet.getBundles5()    * 5L);
            addDenomRow(sumSheet, sr++, "Mix Notes", sheet.getMixNotes().longValue());
            addDenomRow(sumSheet, sr++, "Coins",     sheet.getCoins().longValue());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    private void addSummaryRow(Sheet s, int rowNum, Workbook wb,
                                CellStyle labelStyle, CellStyle amtStyle,
                                String label, Object value) {
        Row row = s.createRow(rowNum);
        Cell lc = row.createCell(0);
        lc.setCellValue(label);
        lc.setCellStyle(labelStyle);
        if (value instanceof BigDecimal bd) {
            Cell vc = row.createCell(1);
            vc.setCellValue(bd.doubleValue());
            vc.setCellStyle(amtStyle);
        }
    }

    private void addDenomRow(Sheet s, int rowNum, String label, long amount) {
        Row row = s.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(amount);
    }

    private String str(Object v) { return v != null ? v.toString() : ""; }

    // ── Map converters ────────────────────────────────────────────
    public Map<String, Object> toSheetMap(CashDaySheet s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",               s.getId());
        m.put("businessId",       s.getBusinessId());
        m.put("branchId",         s.getBranchId());
        m.put("sheetDate",        s.getSheetDate() != null ? s.getSheetDate().toString() : null);
        m.put("openingBalance",   s.getOpeningBalance());
        m.put("openingOverridden",s.isOpeningOverridden());
        m.put("bundles1000",      s.getBundles1000());
        m.put("bundles500",       s.getBundles500());
        m.put("bundles200",       s.getBundles200());
        m.put("bundles100",       s.getBundles100());
        m.put("bundles50",        s.getBundles50());
        m.put("bundles10",        s.getBundles10());
        m.put("bundles5",         s.getBundles5());
        m.put("mixNotes",         s.getMixNotes());
        m.put("coins",            s.getCoins());
        m.put("handLoanTotal",    s.getHandLoanTotal());
        m.put("updatedAt",        s.getUpdatedAt() != null
                ? s.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) : null);
        return m;
    }

    public Map<String, Object> toEntryMap(CashEntry e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",          e.getId());
        m.put("sheetDate",   e.getSheetDate() != null ? e.getSheetDate().toString() : null);
        m.put("partyName",   e.getPartyName());
        m.put("description", e.getDescription());
        m.put("amountIn",    e.getAmountIn());
        m.put("amountOut",   e.getAmountOut());
        m.put("entryType",   e.getEntryType() != null ? e.getEntryType().name() : null);
        m.put("referenceId", e.getReferenceId());
        m.put("sortOrder",   e.getSortOrder());
        m.put("createdBy",   e.getCreatedBy());
        m.put("createdAt",   e.getCreatedAt() != null
                ? e.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) : null);
        return m;
    }
}
