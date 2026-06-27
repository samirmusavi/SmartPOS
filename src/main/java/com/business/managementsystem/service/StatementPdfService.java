package com.business.managementsystem.service;

import com.business.managementsystem.model.Branch;
import com.business.managementsystem.model.Business;
import com.business.managementsystem.model.Supplier;
import com.business.managementsystem.repository.BranchRepository;
import com.business.managementsystem.repository.BusinessRepository;
import com.business.managementsystem.repository.SupplierRepository;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generates a Statement of Account as a PDF byte array.
 *
 * Layout: A4 landscape.  Two-level column header with PdfPTable.setHeaderRows(2)
 * so headers repeat on every page.  Company/branch info drawn on every page via
 * PdfPageEventHelper.  "Page X of Y" uses a PdfTemplate placeholder filled in
 * onCloseDocument.
 *
 * Data source: calls PartyLedgerService.getPartyStatement() — no ledger logic
 * is duplicated here.  Narrations are printed exactly as stored.
 *
 * Library choice: OpenPDF (com.github.librepdf:openpdf) was chosen over Apache
 * PDFBox because OpenPDF provides PdfPTable with native colspan/rowspan,
 * setHeaderRows(), and PdfPageEventHelper.  PDFBox has no table abstraction and
 * requires manual coordinate-based drawing for every cell.
 *
 * Company profile fields come from:
 *   – Business entity  → businessName, ownerName, phone, email
 *   – Branch entity    → name, address, city, country, phone
 *   – Supplier entity  → name, partyCode, address, phone, email, tradeLicenseNumber
 * (Business has no address field; branch address is used instead.)
 *
 * Full pagination IS implemented: setHeaderRows(2) repeats both header rows on
 * every page, and the PdfPageEventHelper draws the company header + page number
 * on every page.
 */
@Service
public class StatementPdfService {

    // ── Palette ──────────────────────────────────────────────────────────
    private static final Color C_HDR_DARK  = new Color(30,  58,  95);   // #1e3a5f navy
    private static final Color C_HDR_MID   = new Color(45,  74, 122);   // #2d4a7a
    private static final Color C_HDR_STRIPE= new Color(248, 250, 252);  // #f8fafc
    private static final Color C_TOTAL_BG  = new Color(224, 231, 255);  // #e0e7ff
    private static final Color C_OPB_BG    = new Color(241, 245, 249);  // #f1f5f9
    private static final Color C_ALT_ROW   = new Color(249, 250, 251);  // #f9fafb
    private static final Color C_BORDER    = new Color(209, 213, 219);  // gray-300
    private static final Color C_BORDER_HDR= new Color(255, 255, 255);  // white dividers in header
    private static final Color C_GREEN     = new Color(5,  150, 105);   // CR
    private static final Color C_RED       = new Color(220,  38,  38);  // DR
    private static final Color C_MUTED     = new Color(107, 114, 128);  // gray-500
    private static final Color C_DARK      = new Color( 17,  24,  39);  // gray-900
    private static final Color C_MED       = new Color( 75,  85,  99);  // gray-600

    private final PartyLedgerService partyLedgerService;
    private final BusinessRepository  businessRepository;
    private final BranchRepository    branchRepository;
    private final SupplierRepository  supplierRepository;

    public StatementPdfService(PartyLedgerService partyLedgerService,
                                BusinessRepository  businessRepository,
                                BranchRepository    branchRepository,
                                SupplierRepository  supplierRepository) {
        this.partyLedgerService = partyLedgerService;
        this.businessRepository  = businessRepository;
        this.branchRepository    = branchRepository;
        this.supplierRepository  = supplierRepository;
    }

    // ════════════════════════════════════════════════════════════════════
    //  MAIN ENTRY POINT
    // ════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public byte[] generateStatementPdf(Long businessId, Long partyId,
                                        LocalDate dateFrom, LocalDate dateTo,
                                        Long branchId) throws Exception {

        // ── Fetch data (all DB reads) ─────────────────────────────────
        Map<String, Object> stmt = partyLedgerService.getPartyStatement(
                businessId, partyId, dateFrom, dateTo);

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));

        long supplierId = Math.abs(partyId);
        Supplier supplier = supplierRepository.findByIdAndBusinessId(supplierId, businessId)
                .orElseThrow(() -> new IllegalArgumentException("Party not found: " + partyId));

        Optional<Branch> branchOpt = (branchId != null)
                ? branchRepository.findByIdAndBusinessId(branchId, businessId)
                : branchRepository.findByBusinessIdAndIsMainBranchTrue(businessId);
        Branch branch = branchOpt.orElse(null);

        // Branch name lookup for each ledger row
        List<Branch> allBranches =
                branchRepository.findByBusinessIdOrderByIsMainBranchDescNameAsc(businessId);
        Map<Long, String> branchNameMap = new HashMap<>();
        for (Branch b : allBranches) branchNameMap.put(b.getId(), b.getName());

        // ── Fonts ─────────────────────────────────────────────────────
        BaseFont bf     = BaseFont.createFont(BaseFont.HELVETICA,
                BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD,
                BaseFont.CP1252, BaseFont.NOT_EMBEDDED);

        Font fHdrGrp  = new Font(bfBold, 7,  Font.NORMAL, Color.WHITE);
        Font fHdrCol  = new Font(bfBold, 7,  Font.NORMAL, Color.WHITE);
        Font fData    = new Font(bf,     8,  Font.NORMAL, C_DARK);
        Font fBold    = new Font(bfBold, 8,  Font.NORMAL, C_DARK);
        Font fNarr    = new Font(bf,     7.5f, Font.NORMAL, new Color(31, 41, 55));
        Font fNarrBold= new Font(bfBold, 7.5f, Font.NORMAL, new Color(31, 41, 55));
        Font fMuted   = new Font(bf,     8,  Font.NORMAL, C_MUTED);
        Font fCr      = new Font(bfBold, 8,  Font.NORMAL, C_GREEN);
        Font fDr      = new Font(bfBold, 8,  Font.NORMAL, C_RED);
        Font fLbl     = new Font(bfBold, 7.5f, Font.NORMAL, C_MUTED);
        Font fPartyNm = new Font(bfBold, 9,  Font.NORMAL, C_DARK);

        // ── Document (A4 landscape) ───────────────────────────────────
        //   top margin 92pt leaves room for the per-page company header
        //   drawn by the page event handler at y > (pageHeight − 88)
        Rectangle pageSize = PageSize.A4.rotate();          // 841.89 × 595.28
        Document  doc      = new Document(pageSize, 28, 28, 92, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(doc, baos);
        writer.setPageEvent(new CompanyHeaderEvent(business, branch, dateFrom, dateTo, bf, bfBold));
        doc.open();

        // ── Party + contact info block ────────────────────────────────
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1, 1});
        infoTable.setSpacingBefore(2f);
        infoTable.setSpacingAfter(6f);

        // "To" cell
        PdfPCell toCell = new PdfPCell();
        toCell.setPadding(8);
        toCell.setBackgroundColor(new Color(249, 250, 251));
        toCell.setBorderColor(C_BORDER);
        toCell.setBorderWidth(0.5f);
        Paragraph toPara = new Paragraph();
        toPara.add(new Chunk("To:  ", fLbl));
        String partyLabel = (supplier.getPartyCode() != null && !supplier.getPartyCode().isBlank()
                ? supplier.getPartyCode() + " — " : "") + supplier.getName();
        toPara.add(new Chunk(partyLabel, fPartyNm));
        toCell.addElement(toPara);
        if (supplier.getAddress() != null && !supplier.getAddress().isBlank()) {
            Paragraph addr = new Paragraph(supplier.getAddress(), fData);
            addr.setSpacingBefore(3f);
            toCell.addElement(addr);
        }
        if (supplier.getTradeLicenseNumber() != null && !supplier.getTradeLicenseNumber().isBlank()) {
            Paragraph trn = new Paragraph();
            trn.setSpacingBefore(2f);
            trn.add(new Chunk("TRN: ", fLbl));
            trn.add(new Chunk(supplier.getTradeLicenseNumber(), fData));
            toCell.addElement(trn);
        }
        infoTable.addCell(toCell);

        // Contact cell
        PdfPCell contactCell = new PdfPCell();
        contactCell.setPadding(8);
        contactCell.setBackgroundColor(new Color(249, 250, 251));
        contactCell.setBorderColor(C_BORDER);
        contactCell.setBorderWidth(0.5f);
        Paragraph ctTitle = new Paragraph("Contact Details", fLbl);
        ctTitle.setSpacingAfter(3f);
        contactCell.addElement(ctTitle);
        if (supplier.getPhone() != null && !supplier.getPhone().isBlank()) {
            Paragraph ph = new Paragraph();
            ph.add(new Chunk("TEL:   ", fLbl));
            ph.add(new Chunk(supplier.getPhone(), fData));
            ph.setSpacingBefore(2f);
            contactCell.addElement(ph);
        }
        if (supplier.getEmail() != null && !supplier.getEmail().isBlank()) {
            Paragraph em = new Paragraph();
            em.add(new Chunk("Email: ", fLbl));
            em.add(new Chunk(supplier.getEmail(), fData));
            em.setSpacingBefore(2f);
            contactCell.addElement(em);
        }
        infoTable.addCell(contactCell);
        doc.add(infoTable);

        // ── Main 10-column ledger table ───────────────────────────────
        //  Columns: Branch | Voucher | Voc Date | Narration |
        //           AED(Dr/Cr/Bal) | Metal(Dr/Cr/Bal)
        PdfPTable tbl = new PdfPTable(10);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{ 40, 65, 55, 165, 72, 72, 86, 58, 58, 76 });
        tbl.setHeaderRows(2);          // rows 0 and 1 repeat on every page
        tbl.setSpacingBefore(4f);
        tbl.setKeepTogether(false);

        // ── Header row 1: group labels ────────────────────────────────
        //   Branch/Voucher/Voc Date/Narration span 2 rows (rowspan=2)
        addHdrCell(tbl, "Branch",    C_HDR_DARK, fHdrCol, Element.ALIGN_LEFT,  2, 1);
        addHdrCell(tbl, "Voucher",   C_HDR_DARK, fHdrCol, Element.ALIGN_LEFT,  2, 1);
        addHdrCell(tbl, "Voc Date",  C_HDR_DARK, fHdrCol, Element.ALIGN_LEFT,  2, 1);
        addHdrCell(tbl, "Narration", C_HDR_DARK, fHdrCol, Element.ALIGN_LEFT,  2, 1);
        addHdrCell(tbl, "AMOUNT IN (AED)",   C_HDR_MID,  fHdrGrp, Element.ALIGN_CENTER, 1, 3);
        addHdrCell(tbl, "PURE WT IN GRAMS",  C_HDR_MID,  fHdrGrp, Element.ALIGN_CENTER, 1, 3);

        // ── Header row 2: individual column labels ────────────────────
        addHdrCell(tbl, "Debit",   C_HDR_DARK, fHdrCol, Element.ALIGN_RIGHT, 1, 1);
        addHdrCell(tbl, "Credit",  C_HDR_DARK, fHdrCol, Element.ALIGN_RIGHT, 1, 1);
        addHdrCell(tbl, "Balance", C_HDR_DARK, fHdrCol, Element.ALIGN_RIGHT, 1, 1);
        addHdrCell(tbl, "Debit",   C_HDR_DARK, fHdrCol, Element.ALIGN_RIGHT, 1, 1);
        addHdrCell(tbl, "Credit",  C_HDR_DARK, fHdrCol, Element.ALIGN_RIGHT, 1, 1);
        addHdrCell(tbl, "Balance", C_HDR_DARK, fHdrCol, Element.ALIGN_RIGHT, 1, 1);

        // ── Opening balance row ───────────────────────────────────────
        BigDecimal openAed     = bd(stmt.get("openingAedBalance"));
        String     openAedType = str(stmt.get("openingAedType"),  "CR");
        double     openMet     = dbl(stmt.get("openingMetalBalance"));
        String     openMetType = str(stmt.get("openingMetalType"), "DR");

        String openDateStr = dateFrom != null
                ? dateFrom.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "";
        addSpecialRow(tbl, "O/B", openDateStr, "Opening Balance (B/F)",
                BigDecimal.ZERO, BigDecimal.ZERO, openAed, openAedType,
                0.0, 0.0, openMet, openMetType,
                C_OPB_BG, fNarrBold, fCr, fDr, fMuted);

        // ── Data rows ─────────────────────────────────────────────────
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries =
                (List<Map<String, Object>>) stmt.get("entries");

        int rowIdx = 0;
        for (Map<String, Object> row : entries) {
            Color bg = (rowIdx % 2 == 1) ? C_ALT_ROW : Color.WHITE;
            addDataRow(tbl, row, bg, branchNameMap, fData, fBold, fNarr, fMuted, fCr, fDr);
            rowIdx++;
        }

        // ── Sub-Total / Closing row ───────────────────────────────────
        BigDecimal totDr      = bd(stmt.get("totalAedDebit"));
        BigDecimal totCr      = bd(stmt.get("totalAedCredit"));
        BigDecimal closeAed   = bd(stmt.get("closingAedBalance"));
        String     closeAedTp = str(stmt.get("closingAedType"),  "CR");
        double     totMetDr   = dbl(stmt.get("totalMetalDebit"));
        double     totMetCr   = dbl(stmt.get("totalMetalCredit"));
        double     closeMet   = dbl(stmt.get("closingMetalBalance"));
        String     closeMetTp = str(stmt.get("closingMetalType"), "DR");

        String closeDateStr = dateTo != null
                ? dateTo.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "";
        addSpecialRow(tbl, "", closeDateStr, "Sub Total / Closing Balance",
                totDr, totCr, closeAed, closeAedTp,
                totMetDr, totMetCr, closeMet, closeMetTp,
                C_TOTAL_BG, fNarrBold, fCr, fDr, fMuted);

        doc.add(tbl);
        doc.close();
        return baos.toByteArray();
    }

    // ════════════════════════════════════════════════════════════════════
    //  CELL BUILDERS
    // ════════════════════════════════════════════════════════════════════

    /** Adds one header cell with optional rowspan / colspan. */
    private static void addHdrCell(PdfPTable tbl, String text, Color bg,
                                    Font font, int hAlign,
                                    int rowspan, int colspan) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(hAlign);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPaddingTop(5f);
        c.setPaddingBottom(5f);
        c.setPaddingLeft(5f);
        c.setPaddingRight(5f);
        c.setBorderColor(C_BORDER_HDR);
        c.setBorderWidth(0.5f);
        if (rowspan > 1) c.setRowspan(rowspan);
        if (colspan > 1) c.setColspan(colspan);
        tbl.addCell(c);
    }

    /** Adds a full data row (10 cells) from a ledger entry map. */
    private static void addDataRow(PdfPTable tbl, Map<String, Object> row,
                                    Color bg, Map<Long, String> branchNameMap,
                                    Font fData, Font fBold, Font fNarr,
                                    Font fMuted, Font fCr, Font fDr) {

        // Branch name lookup
        String branchName = "";
        Object bid = row.get("branchId");
        if (bid != null) {
            Long bId = bid instanceof Number ? ((Number) bid).longValue()
                                             : Long.parseLong(bid.toString());
            branchName = branchNameMap.getOrDefault(bId, "");
        }

        String vType  = str(row.get("voucherType"),   "");
        String vNo    = str(row.get("voucherNumber"),  "");
        String date   = fmtDate(str(row.get("voucherDate"), ""));
        String narr   = str(row.get("narration"), "");

        BigDecimal aedDr   = bd(row.get("aedDebit"));
        BigDecimal aedCr   = bd(row.get("aedCredit"));
        BigDecimal runAed  = bd(row.get("aedRunningBalance"));
        String     runAedT = str(row.get("aedBalanceType"), "CR");

        double metDr   = dbl(row.get("metalDebit"));
        double metCr   = dbl(row.get("metalCredit"));
        double runMet  = dbl(row.get("metalRunningBalance"));
        String runMetT = str(row.get("metalBalanceType"), "DR");

        // Col 1: Branch
        addTxtCell(tbl, branchName, bg, fData, Element.ALIGN_LEFT);

        // Col 2: Voucher type + number stacked
        PdfPCell vc = new PdfPCell();
        vc.setBackgroundColor(bg);
        vc.setPadding(4f);
        vc.setBorderColor(C_BORDER);
        vc.setBorderWidth(0.3f);
        Paragraph vp = new Paragraph();
        Font fVType = new Font(fBold.getBaseFont(), 6.5f, Font.NORMAL, C_MUTED);
        vp.add(new Chunk(vType + "\n", fVType));
        vp.add(new Chunk(vNo, fData));
        vp.setLeading(0, 1.3f);
        vc.addElement(vp);
        tbl.addCell(vc);

        // Col 3: Date
        addTxtCell(tbl, date, bg, fData, Element.ALIGN_LEFT);

        // Col 4: Narration (wraps)
        PdfPCell nc = new PdfPCell(new Phrase(narr, fNarr));
        nc.setBackgroundColor(bg);
        nc.setPadding(4f);
        nc.setBorderColor(C_BORDER);
        nc.setBorderWidth(0.3f);
        nc.setNoWrap(false);
        nc.setLeading(0, 1.3f);
        tbl.addCell(nc);

        // Cols 5–10: amounts
        addAmtCell(tbl, aedDr,  bg, fData, fMuted);
        addAmtCell(tbl, aedCr,  bg, fData, fMuted);
        addBalCell(tbl, runAed, runAedT, bg, fCr, fDr);
        addMetCell(tbl, metDr,  bg, fData, fMuted);
        addMetCell(tbl, metCr,  bg, fData, fMuted);
        addMetBalCell(tbl, runMet, runMetT, bg, fCr, fDr);
    }

    /** Adds a special (opening / subtotal) row (10 cells). */
    private static void addSpecialRow(PdfPTable tbl, String vType, String date, String label,
                                       BigDecimal aedDr, BigDecimal aedCr,
                                       BigDecimal balAed, String balAedType,
                                       double metDr, double metCr,
                                       double balMet, String balMetType,
                                       Color bg, Font fLabel, Font fCr, Font fDr, Font fMuted) {

        addTxtCell(tbl, "",       bg, fLabel, Element.ALIGN_LEFT);
        addTxtCell(tbl, vType,    bg, fLabel, Element.ALIGN_LEFT);
        addTxtCell(tbl, date,     bg, fLabel, Element.ALIGN_LEFT);

        // Label cell (narration column)
        PdfPCell lc = new PdfPCell(new Phrase(label, fLabel));
        lc.setBackgroundColor(bg);
        lc.setPadding(4f);
        lc.setBorderColor(C_BORDER);
        lc.setBorderWidth(0.3f);
        tbl.addCell(lc);

        addAmtCell(tbl, aedDr,   bg, fLabel, fMuted);
        addAmtCell(tbl, aedCr,   bg, fLabel, fMuted);
        addBalCell(tbl, balAed,  balAedType, bg, fCr, fDr);
        addMetCell(tbl, metDr,   bg, fLabel, fMuted);
        addMetCell(tbl, metCr,   bg, fLabel, fMuted);
        addMetBalCell(tbl, balMet, balMetType, bg, fCr, fDr);
    }

    // ── Atomic cell helpers ──────────────────────────────────────────────

    private static void addTxtCell(PdfPTable tbl, String text, Color bg,
                                    Font font, int hAlign) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "", font));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(hAlign);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(4f);
        c.setBorderColor(C_BORDER);
        c.setBorderWidth(0.3f);
        tbl.addCell(c);
    }

    /** AED debit or credit cell — blank if zero. */
    private static void addAmtCell(PdfPTable tbl, BigDecimal v, Color bg,
                                    Font fNormal, Font fZero) {
        boolean zero = v == null || v.compareTo(BigDecimal.ZERO) == 0;
        PdfPCell c = new PdfPCell(new Phrase(zero ? "" : fmtAed(v), zero ? fZero : fNormal));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(4f);
        c.setBorderColor(C_BORDER);
        c.setBorderWidth(0.3f);
        tbl.addCell(c);
    }

    /** AED running-balance cell — always shows value + CR/DR. */
    private static void addBalCell(PdfPTable tbl, BigDecimal v, String type,
                                    Color bg, Font fCr, Font fDr) {
        boolean cr = "CR".equals(type);
        String text = fmtAed(v) + " " + type;
        PdfPCell c = new PdfPCell(new Phrase(text, cr ? fCr : fDr));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(4f);
        c.setBorderColor(C_BORDER);
        c.setBorderWidth(0.3f);
        tbl.addCell(c);
    }

    /** Metal debit/credit cell — blank if zero. */
    private static void addMetCell(PdfPTable tbl, double v, Color bg,
                                    Font fNormal, Font fZero) {
        boolean zero = v == 0.0;
        PdfPCell c = new PdfPCell(new Phrase(zero ? "" : fmtMet(v), zero ? fZero : fNormal));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(4f);
        c.setBorderColor(C_BORDER);
        c.setBorderWidth(0.3f);
        tbl.addCell(c);
    }

    /** Metal running-balance cell — always shows value + CR/DR. */
    private static void addMetBalCell(PdfPTable tbl, double v, String type,
                                       Color bg, Font fCr, Font fDr) {
        boolean cr = "CR".equals(type);
        String text = fmtMet(v) + " " + type;
        PdfPCell c = new PdfPCell(new Phrase(text, cr ? fCr : fDr));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(4f);
        c.setBorderColor(C_BORDER);
        c.setBorderWidth(0.3f);
        tbl.addCell(c);
    }

    // ════════════════════════════════════════════════════════════════════
    //  PAGE EVENT — company header + "Page X of Y" on every page
    // ════════════════════════════════════════════════════════════════════

    private static class CompanyHeaderEvent extends PdfPageEventHelper {

        private final Business   business;
        private final Branch     branch;
        private final LocalDate  dateFrom;
        private final LocalDate  dateTo;
        private final BaseFont   bf;
        private final BaseFont   bfBold;

        /** Placeholder template filled in with total-page count at close. */
        private PdfTemplate totalPagesTpl;

        CompanyHeaderEvent(Business business, Branch branch,
                            LocalDate dateFrom, LocalDate dateTo,
                            BaseFont bf, BaseFont bfBold) {
            this.business = business;
            this.branch   = branch;
            this.dateFrom = dateFrom;
            this.dateTo   = dateTo;
            this.bf       = bf;
            this.bfBold   = bfBold;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            // Reserve a 30×12 template for the total-pages number
            totalPagesTpl = writer.getDirectContent().createTemplate(30, 12);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float w  = document.getPageSize().getWidth();   // 841.89
            float h  = document.getPageSize().getHeight();  // 595.28
            float ml = 28f;                                 // left margin
            float mr = 28f;                                 // right margin

            // Header strip occupies h-8 down to h-86 (78pt tall)
            float stripTop    = h - 8f;
            float stripBottom = h - 86f;
            float stripH      = stripTop - stripBottom;     // 78pt

            // ── Background strip ──────────────────────────────────────
            cb.saveState();
            cb.setColorFill(new Color(248, 250, 252));
            cb.rectangle(ml, stripBottom, w - ml - mr, stripH);
            cb.fill();
            cb.setColorStroke(new Color(209, 213, 219));
            cb.setLineWidth(0.5f);
            cb.rectangle(ml, stripBottom, w - ml - mr, stripH);
            cb.stroke();
            cb.restoreState();

            // ── Left column text ──────────────────────────────────────
            float xL = ml + 10f;
            float yL = stripTop - 15f;  // first text baseline

            Font fTitle = new Font(bfBold, 10, Font.NORMAL, new Color(30, 58, 95));
            Font fMeta  = new Font(bf,     7.5f, Font.NORMAL, new Color(107, 114, 128));
            Font fSmall = new Font(bf,     7,    Font.NORMAL, new Color(107, 114, 128));

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("STATEMENT OF ACCOUNT", fTitle), xL, yL, 0);

            yL -= 14f;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            String dateRange = "From: "
                    + (dateFrom != null ? dateFrom.format(fmt) : "All")
                    + "   To: "
                    + (dateTo   != null ? dateTo.format(fmt)   : "Today")
                    + "   (LC and Gold)";
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(dateRange, fMeta), xL, yL, 0);

            yL -= 12f;
            String branchLabel = "BRANCH: " + (branch != null ? branch.getName() : "HO");
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(branchLabel, fMeta), xL, yL, 0);

            yL -= 12f;
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("Order By: Trans Date", fSmall), xL, yL, 0);

            // ── Right column text ─────────────────────────────────────
            float xR = w - mr - 10f;
            float yR = stripTop - 15f;

            Font fCoName = new Font(bfBold, 12, Font.NORMAL, new Color(17, 24, 39));
            Font fCoMeta = new Font(bf,     7.5f, Font.NORMAL, new Color(75, 85, 99));

            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase(business.getBusinessName(), fCoName), xR, yR, 0);

            if (business.getOwnerName() != null && !business.getOwnerName().isBlank()) {
                yR -= 14f;
                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                        new Phrase(business.getOwnerName(), fCoMeta), xR, yR, 0);
            }

            if (branch != null) {
                String addrLine = buildAddrLine(branch);
                if (!addrLine.isBlank()) {
                    yR -= 12f;
                    ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                            new Phrase(addrLine, fCoMeta), xR, yR, 0);
                }
                if (branch.getPhone() != null && !branch.getPhone().isBlank()) {
                    yR -= 12f;
                    ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                            new Phrase("Tel: " + branch.getPhone(), fCoMeta), xR, yR, 0);
                }
            }

            if (business.getEmail() != null && !business.getEmail().isBlank()) {
                yR -= 12f;
                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                        new Phrase("Email: " + business.getEmail(), fCoMeta), xR, yR, 0);
            }
            if (business.getPhone() != null && !business.getPhone().isBlank()) {
                yR -= 12f;
                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                        new Phrase("Phone: " + business.getPhone(), fCoMeta), xR, yR, 0);
            }

            // ── Page X of Y ───────────────────────────────────────────
            //   Drawn at bottom-right of the header strip
            float pnY = stripBottom + 6f;
            String pnText = "Page " + writer.getPageNumber() + " of ";
            float pnTextWidth = bf.getWidthPoint(pnText, 7f);
            // Position so the whole block is right-aligned at xR, with 30pt for total
            float pnX = xR - pnTextWidth - 30f;

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(pnText, fSmall), pnX, pnY, 0);
            // Template placeholder for total pages (rendered in onCloseDocument)
            cb.addTemplate(totalPagesTpl, pnX + pnTextWidth, pnY - 1f);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            // writer.getPageNumber() at close = last real page + 1
            int total = writer.getPageNumber() - 1;
            Font fSmall = new Font(bf, 7, Font.NORMAL, new Color(107, 114, 128));
            ColumnText.showTextAligned(totalPagesTpl, Element.ALIGN_LEFT,
                    new Phrase(String.valueOf(total), fSmall), 1, 2, 0);
        }

        // Build "Street, City, Country" omitting blanks
        private static String buildAddrLine(Branch b) {
            StringBuilder sb = new StringBuilder();
            if (b.getAddress() != null && !b.getAddress().isBlank())
                sb.append(b.getAddress());
            if (b.getCity() != null && !b.getCity().isBlank())
                sb.append(sb.length() > 0 ? ", " : "").append(b.getCity());
            if (b.getCountry() != null && !b.getCountry().isBlank())
                sb.append(sb.length() > 0 ? ", " : "").append(b.getCountry());
            return sb.toString();
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  UTILITIES
    // ════════════════════════════════════════════════════════════════════

    private static String fmtAed(BigDecimal v) {
        if (v == null) return "0.00";
        return String.format("%,.2f", v);
    }

    private static String fmtMet(double v) {
        return String.format("%,.3f", v);
    }

    private static String fmtDate(String iso) {
        if (iso == null || iso.isEmpty() || iso.equals("null")) return "";
        try {
            // voucherDate is "yyyy-MM-dd" (first 10 chars)
            return LocalDate.parse(iso.length() >= 10 ? iso.substring(0, 10) : iso)
                            .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (Exception e) {
            return iso;
        }
    }

    private static BigDecimal bd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number)    return new BigDecimal(v.toString());
        try { return new BigDecimal(v.toString().trim()); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private static double dbl(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(v.toString().trim()); } catch (Exception e) { return 0.0; }
    }

    private static String str(Object v, String def) {
        return (v != null && !v.toString().isBlank()) ? v.toString() : def;
    }
}
