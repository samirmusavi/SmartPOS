package com.business.managementsystem.service;

import com.business.managementsystem.model.Branch;
import com.business.managementsystem.model.Business;
import com.business.managementsystem.model.Customer;
import com.business.managementsystem.model.PartyLedgerEntry;
import com.business.managementsystem.model.Purchase;
import com.business.managementsystem.model.PurchaseItem;
import com.business.managementsystem.model.Sale;
import com.business.managementsystem.model.SaleTransaction;
import com.business.managementsystem.model.Supplier;
import com.business.managementsystem.repository.BranchRepository;
import com.business.managementsystem.repository.BusinessRepository;
import com.business.managementsystem.repository.CustomerRepository;
import com.business.managementsystem.repository.PartyLedgerEntryRepository;
import com.business.managementsystem.repository.PurchaseItemRepository;
import com.business.managementsystem.repository.PurchaseRepository;
import com.business.managementsystem.repository.SaleRepository;
import com.business.managementsystem.repository.SaleTransactionRepository;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Generates a Tax Invoice (Fixed) as an A4 portrait PDF.
 *
 * Used for both sales and purchases.  Reuses the same OpenPDF library
 * (com.github.librepdf:openpdf 1.3.30) and page-event pattern as
 * StatementPdfService — no second PDF dependency is introduced.
 *
 * Data sources:
 *  – Sale:     SaleTransaction + Sale items via SaleRepository
 *  – Purchase: Purchase header + PurchaseItem list
 *  – Party info: Supplier or Customer entity
 *  – Narration: PartyLedgerEntry (verbatim, never regenerated)
 *  – Company:  Business + Branch entities
 *
 * The "roundOffAmount" and "includeReverseChargeDeclaration" fields
 * ALREADY EXIST on SaleTransaction and Purchase — they are only read here,
 * never added.
 *
 * Declaration text is the exact legal wording from Cabinet Decision No. 127/2024
 * and is printed only when the flag on the sale record is true.
 */
@Service
public class TaxInvoicePdfService {

    // ── Colour palette ───────────────────────────────────────────────────
    private static final Color C_HDR_DARK   = new Color(30,  58,  95);   // #1e3a5f navy
    private static final Color C_HDR_MID    = new Color(45,  74, 122);   // #2d4a7a
    private static final Color C_TOTAL_BG   = new Color(224, 231, 255);  // #e0e7ff
    private static final Color C_STRIPE     = new Color(248, 250, 252);  // #f8fafc
    private static final Color C_ALT_ROW    = new Color(249, 250, 251);  // #f9fafb
    private static final Color C_BORDER     = new Color(209, 213, 219);  // gray-300
    private static final Color C_DARK       = new Color( 17,  24,  39);  // gray-900
    private static final Color C_MED        = new Color( 75,  85,  99);  // gray-600
    private static final Color C_MUTED      = new Color(107, 114, 128);  // gray-500
    private static final Color C_DECL_BG    = new Color(255, 251, 235);  // amber-50
    private static final Color C_DECL_BDR   = new Color(217, 119,   6);  // amber-600
    private static final Color C_DEBIT      = new Color(220,  38,  38);  // red-600
    private static final Color C_CREDIT     = new Color(  5, 150, 105);  // emerald-600

    private static final String DECLARATION_TEXT =
            "UAE Reverse-Charge VAT Declaration (Cabinet Decision No. 127/2024): " +
            "The supplier named herein is not registered for VAT in the UAE. " +
            "Pursuant to Cabinet Decision No. 127/2024, the recipient (buyer) is responsible " +
            "for self-accounting for VAT on this supply under the reverse-charge mechanism. " +
            "The applicable VAT must be declared and paid directly to the Federal Tax Authority " +
            "by the recipient.";

    // ── Repositories ─────────────────────────────────────────────────────
    private final SaleTransactionRepository   txRepo;
    private final SaleRepository              saleRepo;
    private final PurchaseRepository          purchaseRepo;
    private final PurchaseItemRepository      itemRepo;
    private final BusinessRepository          businessRepo;
    private final BranchRepository            branchRepo;
    private final SupplierRepository          supplierRepo;
    private final CustomerRepository          customerRepo;
    private final PartyLedgerEntryRepository  ledgerRepo;

    public TaxInvoicePdfService(
            SaleTransactionRepository  txRepo,
            SaleRepository             saleRepo,
            PurchaseRepository         purchaseRepo,
            PurchaseItemRepository     itemRepo,
            BusinessRepository         businessRepo,
            BranchRepository           branchRepo,
            SupplierRepository         supplierRepo,
            CustomerRepository         customerRepo,
            PartyLedgerEntryRepository ledgerRepo) {
        this.txRepo       = txRepo;
        this.saleRepo     = saleRepo;
        this.purchaseRepo = purchaseRepo;
        this.itemRepo     = itemRepo;
        this.businessRepo = businessRepo;
        this.branchRepo   = branchRepo;
        this.supplierRepo = supplierRepo;
        this.customerRepo = customerRepo;
        this.ledgerRepo   = ledgerRepo;
    }

    // ════════════════════════════════════════════════════════════════════
    //  PUBLIC ENTRY POINTS
    // ════════════════════════════════════════════════════════════════════

    /**
     * Generate a Tax Invoice PDF for a sale transaction.
     *
     * @param saleTransactionId  SaleTransaction.id (the numeric transaction ID, not receiptNumber)
     * @param businessId         must match tx.businessId (security check)
     * @return PDF as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateSaleInvoicePdf(Long saleTransactionId, Long businessId) throws Exception {

        SaleTransaction tx = txRepo.findById(saleTransactionId)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + saleTransactionId));
        if (!tx.getBusinessId().equals(businessId))
            throw new IllegalArgumentException("Unauthorized");

        Business business = businessRepo.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));

        Branch branch = tx.getBranchId() != null
                ? branchRepo.findById(tx.getBranchId()).orElse(null) : null;
        if (branch == null)
            branch = branchRepo.findByBusinessIdAndIsMainBranchTrue(businessId).orElse(null);

        // Party info — resolve supplier or customer
        PartyInfo party = resolvePartyForSale(tx.getCustomerId(), tx.getCustomerName(), businessId);

        // Narration from ledger (verbatim — never regenerated)
        String narration = fetchNarration(saleTransactionId, "SALE");

        // Items
        List<Sale> sales = saleRepo.findByTransactionId(saleTransactionId);

        // Build invoice rows
        List<ItemRow> rows = new ArrayList<>();
        int sn = 1;
        for (Sale s : sales) {
            rows.add(new ItemRow(
                    sn++,
                    s.getProduct() != null ? s.getProduct().getName() : "—",
                    s.getScrapPurity() != null ? s.getScrapPurity() : "",
                    "PCS",
                    String.valueOf(s.getQuantitySold()),
                    fmtAed(s.getPriceAtSale()),
                    fmtAed(s.getTotalAmount())
            ));
        }

        InvoiceData data = new InvoiceData();
        data.docType         = "SALE";
        data.voucherNumber   = safe(tx.getReceiptNumber(), "—");
        data.voucherDate     = fmtDateTime(tx.getCreatedAt());
        data.paymentMethod   = safe(tx.getPaymentMethod(), "—");
        data.cashierName     = safe(tx.getCashierName(), "—");
        data.business        = business;
        data.branch          = branch;
        data.party           = party;
        data.subtotal        = nvl(tx.getSubtotal());
        data.discountAmount  = nvl(tx.getDiscountAmount());
        data.vatPercent      = tx.getVatPercent();
        data.vatAmount       = nvl(tx.getVatAmount());
        data.roundOffAmount  = nvl(tx.getRoundOffAmount());
        data.totalAmount     = nvl(tx.getTotalAmount());
        data.exchangeRate    = tx.getExchangeRate();
        data.rows            = rows;
        data.narration       = narration;
        data.includeDeclaration = Boolean.TRUE.equals(tx.getIncludeReverseChargeDeclaration());

        return buildPdf(data);
    }

    /**
     * Generate a Tax Invoice PDF for a purchase.
     *
     * @param purchaseId  Purchase.id
     * @param businessId  must match purchase.businessId (security check)
     * @return PDF as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generatePurchaseInvoicePdf(Long purchaseId, Long businessId) throws Exception {

        Purchase p = purchaseRepo.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + purchaseId));
        if (!p.getBusinessId().equals(businessId))
            throw new IllegalArgumentException("Unauthorized");

        Business business = businessRepo.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));

        Branch branch = p.getBranchId() != null
                ? branchRepo.findById(p.getBranchId()).orElse(null) : null;
        if (branch == null)
            branch = branchRepo.findByBusinessIdAndIsMainBranchTrue(businessId).orElse(null);

        // Party info from supplier record
        PartyInfo party = new PartyInfo();
        if (p.getSupplierId() != null) {
            Optional<Supplier> sup = supplierRepo.findByIdAndBusinessId(p.getSupplierId(), businessId);
            if (sup.isPresent()) {
                Supplier s = sup.get();
                party.code    = safe(s.getPartyCode(), "");
                party.name    = safe(s.getName(), safe(p.getSupplierName(), "—"));
                party.address = safe(s.getAddress(), "");
                party.trn     = safe(s.getTradeLicenseNumber(), "");
                party.phone   = safe(s.getPhone(), "");
                party.email   = safe(s.getEmail(), "");
            } else {
                party.name = safe(p.getSupplierName(), "—");
            }
        } else {
            party.name = safe(p.getSupplierName(), "—");
        }

        // Narration from ledger (verbatim — never regenerated)
        String narration = fetchNarration(purchaseId, "PURCHASE");

        // Items
        List<PurchaseItem> items = itemRepo.findByPurchaseIdOrderByIdAsc(purchaseId);
        List<ItemRow> rows = new ArrayList<>();
        int sn = 1;
        for (PurchaseItem i : items) {
            // Determine display unit and quantity
            String unit = safe(i.getUnitType(), "PCS");
            String qty;
            if ("GRAM".equals(unit) && i.getWeightGrams() != null) {
                qty = String.format("%.3f g", i.getWeightGrams());
            } else if ("PCS".equals(unit) && i.getWeightGrams() != null && i.getWeightGrams() > 0) {
                qty = String.format("%.0f PCS / %.3f g", i.getQuantity(), i.getWeightGrams());
            } else {
                qty = String.valueOf(i.getQuantity());
            }
            rows.add(new ItemRow(
                    sn++,
                    safe(i.getProductName(), "—"),
                    safe(i.getPurity(), ""),
                    unit,
                    qty,
                    i.getUnitPrice() != null ? fmtAed(i.getUnitPrice()) : "—",
                    fmtAed(i.getTotalPrice())
            ));
        }

        // Reconstruct subtotal = total - discount - round off  (Purchase has no explicit subtotal)
        BigDecimal disc  = nvl(p.getDiscountAmount());
        BigDecimal rnd   = nvl(p.getRoundOffAmount());
        BigDecimal total = nvl(p.getTotalAmount());
        BigDecimal sub   = total.subtract(disc).subtract(rnd).max(BigDecimal.ZERO);

        InvoiceData data = new InvoiceData();
        data.docType        = "PURCHASE";
        data.voucherNumber  = safe(p.getInvoiceNumber(), "—");
        data.voucherDate    = p.getPurchaseDate() != null
                ? p.getPurchaseDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";
        data.paymentMethod  = p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "—";
        data.cashierName    = safe(p.getCreatedBy(), "—");
        data.business       = business;
        data.branch         = branch;
        data.party          = party;
        data.subtotal       = sub;
        data.discountAmount = disc;
        data.vatPercent     = 0.0;    // Purchase has no VAT stored
        data.vatAmount      = BigDecimal.ZERO;
        data.roundOffAmount = rnd;
        data.totalAmount    = total;
        data.exchangeRate   = p.getExchangeRate();
        data.rows           = rows;
        data.narration      = narration;
        data.includeDeclaration = false;  // Declaration is Sales-only

        return buildPdf(data);
    }

    // ════════════════════════════════════════════════════════════════════
    //  CORE PDF BUILDER
    // ════════════════════════════════════════════════════════════════════

    private byte[] buildPdf(InvoiceData d) throws Exception {

        // ── Fonts ─────────────────────────────────────────────────────
        BaseFont bf     = BaseFont.createFont(BaseFont.HELVETICA,
                BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD,
                BaseFont.CP1252, BaseFont.NOT_EMBEDDED);

        Font fBody    = new Font(bf,     8.5f, Font.NORMAL, C_DARK);
        Font fBold    = new Font(bfBold, 8.5f, Font.NORMAL, C_DARK);
        Font fLbl     = new Font(bfBold, 7.5f, Font.NORMAL, C_MUTED);
        Font fSmall   = new Font(bf,     7.5f, Font.NORMAL, C_MED);
        Font fHdrCol  = new Font(bfBold, 8f,   Font.NORMAL, Color.WHITE);
        Font fTotal   = new Font(bfBold, 9f,   Font.NORMAL, C_DARK);
        Font fDebit   = new Font(bfBold, 9f,   Font.NORMAL, C_DEBIT);
        Font fCredit  = new Font(bfBold, 9f,   Font.NORMAL, C_CREDIT);
        Font fDecl    = new Font(bf,     7.5f, Font.NORMAL, new Color(92, 45, 4));

        // ── Document — A4 portrait, top=108pt for per-page header ────
        Document doc  = new Document(PageSize.A4, 36, 36, 108, 40);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(doc, baos);
        writer.setPageEvent(new InvoiceHeaderEvent(d.business, d.branch,
                d.docType, d.voucherNumber, bf, bfBold));
        doc.open();

        // ════════════════════════════════════════════════════════════
        //  1.  PARTY INFO  +  VOUCHER INFO  (2-column table)
        // ════════════════════════════════════════════════════════════
        PdfPTable infoTbl = new PdfPTable(2);
        infoTbl.setWidthPercentage(100);
        infoTbl.setWidths(new float[]{ 55f, 45f });
        infoTbl.setSpacingBefore(0f);
        infoTbl.setSpacingAfter(6f);

        // Left — party "TO" box
        PdfPCell toCell = new PdfPCell();
        toCell.setPadding(8f);
        toCell.setBackgroundColor(C_STRIPE);
        toCell.setBorderColor(C_BORDER);
        toCell.setBorderWidth(0.5f);

        Paragraph toPara = new Paragraph();
        toPara.add(new Chunk("TO:  ", fLbl));
        String partyDisplay = (d.party.code != null && !d.party.code.isBlank()
                ? d.party.code + " — " : "") + d.party.name;
        toPara.add(new Chunk(partyDisplay, new Font(bfBold, 9.5f, Font.NORMAL, C_DARK)));
        toCell.addElement(toPara);

        if (!d.party.address.isBlank()) {
            Paragraph addr = new Paragraph(d.party.address, fSmall);
            addr.setSpacingBefore(3f);
            toCell.addElement(addr);
        }
        if (!d.party.trn.isBlank()) {
            Paragraph trn = new Paragraph();
            trn.setSpacingBefore(3f);
            trn.add(new Chunk("TRN: ", fLbl));
            trn.add(new Chunk(d.party.trn, fBody));
            toCell.addElement(trn);
        }
        if (!d.party.phone.isBlank()) {
            Paragraph ph = new Paragraph();
            ph.setSpacingBefore(2f);
            ph.add(new Chunk("TEL: ", fLbl));
            ph.add(new Chunk(d.party.phone, fBody));
            toCell.addElement(ph);
        }
        if (!d.party.email.isBlank()) {
            Paragraph em = new Paragraph();
            em.setSpacingBefore(2f);
            em.add(new Chunk("Email: ", fLbl));
            em.add(new Chunk(d.party.email, fBody));
            toCell.addElement(em);
        }
        infoTbl.addCell(toCell);

        // Right — voucher details box
        PdfPCell vCell = new PdfPCell();
        vCell.setPadding(8f);
        vCell.setBackgroundColor(C_STRIPE);
        vCell.setBorderColor(C_BORDER);
        vCell.setBorderWidth(0.5f);

        addInfoLine(vCell, "Voucher No.",    d.voucherNumber,     fLbl, fBold);
        addInfoLine(vCell, "Date",           d.voucherDate,       fLbl, fBody);
        addInfoLine(vCell, "Payment",        d.paymentMethod,     fLbl, fBody);
        if (d.branch != null)
            addInfoLine(vCell, "Branch", d.branch.getName(),      fLbl, fBody);
        if ("SALE".equals(d.docType) && !d.cashierName.equals("—"))
            addInfoLine(vCell, "Cashier",    d.cashierName,       fLbl, fBody);
        if (d.exchangeRate != null)
            addInfoLine(vCell, "Exch. Rate", fmtAed(d.exchangeRate), fLbl, fBody);
        infoTbl.addCell(vCell);

        doc.add(infoTbl);

        // ════════════════════════════════════════════════════════════
        //  2.  LINE ITEMS TABLE  (7 columns)
        // ════════════════════════════════════════════════════════════
        // S.No | Description | Purity | Unit | Qty/Wt | Unit Price | Amount (AED)
        PdfPTable itemTbl = new PdfPTable(7);
        itemTbl.setWidthPercentage(100);
        itemTbl.setWidths(new float[]{ 28f, 170f, 58f, 44f, 68f, 82f, 82f });
        itemTbl.setHeaderRows(1);
        itemTbl.setSpacingBefore(0f);
        itemTbl.setSpacingAfter(4f);

        // Header row
        String[] hdrs = { "S.No", "Description", "Purity", "Unit", "Qty / Wt", "Unit Price", "Amount (AED)" };
        int[] aligns  = { Element.ALIGN_CENTER, Element.ALIGN_LEFT, Element.ALIGN_CENTER,
                          Element.ALIGN_CENTER, Element.ALIGN_CENTER, Element.ALIGN_RIGHT, Element.ALIGN_RIGHT };
        for (int i = 0; i < hdrs.length; i++) {
            PdfPCell hc = new PdfPCell(new Phrase(hdrs[i], fHdrCol));
            hc.setBackgroundColor(C_HDR_DARK);
            hc.setHorizontalAlignment(aligns[i]);
            hc.setVerticalAlignment(Element.ALIGN_MIDDLE);
            hc.setPaddingTop(5f);
            hc.setPaddingBottom(5f);
            hc.setPaddingLeft(4f);
            hc.setPaddingRight(4f);
            hc.setBorderColor(new Color(255, 255, 255));
            hc.setBorderWidth(0.4f);
            itemTbl.addCell(hc);
        }

        // Data rows
        boolean alt = false;
        for (ItemRow row : d.rows) {
            Color rowBg = alt ? C_ALT_ROW : Color.WHITE;
            alt = !alt;
            String[] vals = {
                    String.valueOf(row.sn),
                    row.description,
                    row.purity,
                    row.unit,
                    row.qty,
                    row.unitPrice,
                    row.amount
            };
            Font[] fts = { fBody, fBody, fSmall, fSmall, fSmall, fBody, fBody };
            for (int i = 0; i < vals.length; i++) {
                PdfPCell dc = new PdfPCell(new Phrase(vals[i] != null ? vals[i] : "", fts[i]));
                dc.setBackgroundColor(rowBg);
                dc.setHorizontalAlignment(aligns[i]);
                dc.setVerticalAlignment(Element.ALIGN_MIDDLE);
                dc.setPadding(4f);
                dc.setBorderColor(C_BORDER);
                dc.setBorderWidth(0.3f);
                itemTbl.addCell(dc);
            }
        }
        doc.add(itemTbl);

        // ════════════════════════════════════════════════════════════
        //  3.  SUMMARY BLOCK  (right-aligned two-column sub-table)
        // ════════════════════════════════════════════════════════════
        PdfPTable sumOuter = new PdfPTable(2);
        sumOuter.setWidthPercentage(100);
        sumOuter.setWidths(new float[]{ 55f, 45f });
        sumOuter.setSpacingBefore(2f);
        sumOuter.setSpacingAfter(6f);

        // Left cell — empty (spacer)
        PdfPCell spacer = new PdfPCell(new Phrase(""));
        spacer.setBorder(Rectangle.NO_BORDER);
        sumOuter.addCell(spacer);

        // Right cell — summary rows
        PdfPTable sumTbl = new PdfPTable(2);
        sumTbl.setWidthPercentage(100);
        sumTbl.setWidths(new float[]{ 60f, 40f });

        addSumRow(sumTbl, "Subtotal",             fmtAed(d.subtotal),      fLbl, fBody, Color.WHITE);
        if (d.discountAmount.compareTo(BigDecimal.ZERO) != 0)
            addSumRow(sumTbl, "Discount",         "- " + fmtAed(d.discountAmount), fLbl, fBody, Color.WHITE);
        if (d.vatPercent > 0 || d.vatAmount.compareTo(BigDecimal.ZERO) != 0)
            addSumRow(sumTbl, "VAT @ " + fmtPct(d.vatPercent) + "%",
                      fmtAed(d.vatAmount), fLbl, fBody, Color.WHITE);
        if (d.roundOffAmount.compareTo(BigDecimal.ZERO) != 0)
            addSumRow(sumTbl, "Round Off",
                      (d.roundOffAmount.compareTo(BigDecimal.ZERO) > 0 ? "" : "-")
                      + fmtAed(d.roundOffAmount.abs()), fLbl, fBody, Color.WHITE);

        // Divider row
        PdfPCell divL = new PdfPCell(new Phrase(""));
        divL.setBorderWidthTop(0.8f);
        divL.setBorderWidthBottom(0f);
        divL.setBorderWidthLeft(0f);
        divL.setBorderWidthRight(0f);
        divL.setBorderColorTop(C_BORDER);
        divL.setPaddingBottom(2f);
        divL.setColspan(2);
        sumTbl.addCell(divL);

        addSumRow(sumTbl, "TOTAL (AED)",          fmtAed(d.totalAmount),   fTotal, fTotal, C_TOTAL_BG);

        PdfPCell sumCell = new PdfPCell(sumTbl);
        sumCell.setBorderColor(C_BORDER);
        sumCell.setBorderWidth(0.5f);
        sumCell.setPadding(0f);
        sumOuter.addCell(sumCell);

        doc.add(sumOuter);

        // ════════════════════════════════════════════════════════════
        //  4.  ACCOUNT UPDATE LINE  +  AED WORDS
        // ════════════════════════════════════════════════════════════
        boolean isSale = "SALE".equals(d.docType);
        String direction   = isSale ? "DEBITED" : "CREDITED";
        String words       = AedWords.convert(d.totalAmount);

        Paragraph acctLine = new Paragraph();
        acctLine.setSpacingBefore(2f);
        acctLine.add(new Chunk("A/c " + direction + ": ", fLbl));
        Font fDir = isSale
                ? new Font(bfBold, 9f, Font.NORMAL, C_DEBIT)
                : new Font(bfBold, 9f, Font.NORMAL, C_CREDIT);
        acctLine.add(new Chunk("AED " + fmtAed(d.totalAmount), fDir));
        doc.add(acctLine);

        Paragraph wordsLine = new Paragraph();
        wordsLine.setSpacingBefore(1f);
        wordsLine.add(new Chunk("In Words: ", fLbl));
        wordsLine.add(new Chunk(words, fSmall));
        doc.add(wordsLine);

        // ════════════════════════════════════════════════════════════
        //  5.  NARRATION  (verbatim from ledger, never regenerated)
        // ════════════════════════════════════════════════════════════
        if (d.narration != null && !d.narration.isBlank()) {
            PdfPTable narrTbl = new PdfPTable(1);
            narrTbl.setWidthPercentage(100);
            narrTbl.setSpacingBefore(6f);
            narrTbl.setSpacingAfter(6f);

            PdfPCell nc = new PdfPCell();
            nc.setPadding(7f);
            nc.setBackgroundColor(C_STRIPE);
            nc.setBorderColor(C_BORDER);
            nc.setBorderWidth(0.5f);

            Paragraph np = new Paragraph();
            np.add(new Chunk("Narration: ", fLbl));
            np.add(new Chunk(d.narration, fBody));
            nc.addElement(np);
            narrTbl.addCell(nc);
            doc.add(narrTbl);
        }

        // ════════════════════════════════════════════════════════════
        //  6.  DECLARATION  (Sales-only, manual flag)
        // ════════════════════════════════════════════════════════════
        if (d.includeDeclaration) {
            PdfPTable declTbl = new PdfPTable(1);
            declTbl.setWidthPercentage(100);
            declTbl.setSpacingBefore(4f);
            declTbl.setSpacingAfter(6f);

            PdfPCell dc = new PdfPCell();
            dc.setPadding(8f);
            dc.setBackgroundColor(C_DECL_BG);
            dc.setBorderColor(C_DECL_BDR);
            dc.setBorderWidth(1f);

            Paragraph dp = new Paragraph();
            dp.add(new Chunk("DECLARATION: ", new Font(bfBold, 7.5f, Font.NORMAL, new Color(92, 45, 4))));
            dp.add(new Chunk(DECLARATION_TEXT, fDecl));
            dp.setLeading(10f);
            dc.addElement(dp);
            declTbl.addCell(dc);
            doc.add(declTbl);
        }

        // ════════════════════════════════════════════════════════════
        //  7.  SIGNATURE BLOCK
        // ════════════════════════════════════════════════════════════
        PdfPTable sigTbl = new PdfPTable(3);
        sigTbl.setWidthPercentage(100);
        sigTbl.setWidths(new float[]{ 1f, 1f, 1f });
        sigTbl.setSpacingBefore(16f);

        String bizName = d.business.getBusinessName();
        String[] sigLabels = { "Received By", "For " + bizName, "Authorised Signatory" };
        for (String lbl : sigLabels) {
            PdfPCell sc = new PdfPCell();
            sc.setBorder(Rectangle.NO_BORDER);
            sc.setPaddingTop(2f);
            sc.setPaddingBottom(2f);
            sc.setHorizontalAlignment(Element.ALIGN_CENTER);

            // Signature line
            Paragraph sigLine = new Paragraph("___________________________",
                    new Font(bf, 9f, Font.NORMAL, C_BORDER));
            sigLine.setAlignment(Element.ALIGN_CENTER);
            sc.addElement(sigLine);

            Paragraph sigLbl = new Paragraph(lbl, fLbl);
            sigLbl.setAlignment(Element.ALIGN_CENTER);
            sigLbl.setSpacingBefore(2f);
            sc.addElement(sigLbl);

            sigTbl.addCell(sc);
        }
        doc.add(sigTbl);

        doc.close();
        return baos.toByteArray();
    }

    // ════════════════════════════════════════════════════════════════════
    //  PAGE EVENT — header stripe drawn on every page
    // ════════════════════════════════════════════════════════════════════

    private static class InvoiceHeaderEvent extends PdfPageEventHelper {

        private final Business business;
        private final Branch   branch;
        private final String   docType;       // "SALE" or "PURCHASE"
        private final String   voucherNumber;
        private final BaseFont bf;
        private final BaseFont bfBold;

        private PdfTemplate totalPagesTpl;

        InvoiceHeaderEvent(Business business, Branch branch,
                            String docType, String voucherNumber,
                            BaseFont bf, BaseFont bfBold) {
            this.business      = business;
            this.branch        = branch;
            this.docType       = docType;
            this.voucherNumber = voucherNumber;
            this.bf            = bf;
            this.bfBold        = bfBold;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPagesTpl = writer.getDirectContent().createTemplate(30, 12);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float w  = document.getPageSize().getWidth();   // 595.28  (A4 portrait)
            float h  = document.getPageSize().getHeight();  // 841.89
            float ml = 36f;
            float mr = 36f;

            // Header strip: from (h - 10) down to (h - 105) = 95pt tall
            float stripTop    = h - 10f;
            float stripBottom = h - 105f;
            float stripH      = stripTop - stripBottom;

            // ── Background ────────────────────────────────────────────
            cb.saveState();
            cb.setColorFill(C_STRIPE);
            cb.rectangle(ml, stripBottom, w - ml - mr, stripH);
            cb.fill();
            cb.setColorStroke(C_BORDER);
            cb.setLineWidth(0.5f);
            cb.rectangle(ml, stripBottom, w - ml - mr, stripH);
            cb.stroke();
            cb.restoreState();

            float innerL = ml + 8f;
            float innerR = w - mr - 8f;
            float midX   = w / 2f;

            Font fCoName  = new Font(bfBold, 13f, Font.NORMAL, new Color(17, 24, 39));
            Font fCoMeta  = new Font(bf,      8f, Font.NORMAL, C_MED);
            Font fTitle   = new Font(bfBold, 11f, Font.NORMAL, C_HDR_DARK);
            Font fCopy    = new Font(bfBold,  8f, Font.NORMAL, C_MED);
            Font fPageNum = new Font(bf,      7f, Font.NORMAL, C_MUTED);

            // ── Left: Company name + branch info ──────────────────────
            float yL = stripTop - 16f;
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(business.getBusinessName(), fCoName), innerL, yL, 0);

            if (branch != null) {
                yL -= 13f;
                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                        new Phrase(branch.getName(), fCoMeta), innerL, yL, 0);
                String addrLine = buildAddrLine(branch);
                if (!addrLine.isBlank()) {
                    yL -= 11f;
                    ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                            new Phrase(addrLine, fCoMeta), innerL, yL, 0);
                }
                if (branch.getPhone() != null && !branch.getPhone().isBlank()) {
                    yL -= 11f;
                    ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                            new Phrase("Tel: " + branch.getPhone(), fCoMeta), innerL, yL, 0);
                }
            }

            if (business.getEmail() != null && !business.getEmail().isBlank()) {
                yL -= 11f;
                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                        new Phrase("Email: " + business.getEmail(), fCoMeta), innerL, yL, 0);
            }

            // ── Center: "TAX INVOICE (FIXED)" with double-rule ────────
            float yC = stripTop - 20f;
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("TAX INVOICE (FIXED)", fTitle), midX, yC, 0);

            // Double rule — two thin lines below the title
            cb.saveState();
            cb.setColorStroke(C_HDR_DARK);
            float ruleY1 = yC - 5f;
            float ruleY2 = yC - 8f;
            float rx = midX - 90f;
            float rw = 180f;
            cb.setLineWidth(1.2f);
            cb.moveTo(rx, ruleY1); cb.lineTo(rx + rw, ruleY1); cb.stroke();
            cb.setLineWidth(0.4f);
            cb.moveTo(rx, ruleY2); cb.lineTo(rx + rw, ruleY2); cb.stroke();
            cb.restoreState();

            // Document type sub-label
            Font fSub = new Font(bf, 7.5f, Font.NORMAL, C_MUTED);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("SALE".equals(docType) ? "Sales Invoice" : "Purchase Invoice", fSub),
                    midX, yC - 18f, 0);

            // ── Right: ACCOUNTS COPY + Page X of Y ───────────────────
            float yR = stripTop - 16f;
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("ACCOUNTS COPY", fCopy), innerR, yR, 0);

            yR -= 13f;
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Ref: " + voucherNumber, fPageNum), innerR, yR, 0);

            // Page X of Y
            float pnY = stripBottom + 5f;
            String pnText = "Page " + writer.getPageNumber() + " of ";
            float pnW = bf.getWidthPoint(pnText, 7f);
            float pnX = innerR - pnW - 30f;
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(pnText, fPageNum), pnX, pnY, 0);
            cb.addTemplate(totalPagesTpl, pnX + pnW, pnY - 1f);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            int total = writer.getPageNumber() - 1;
            Font fPageNum = new Font(bf, 7, Font.NORMAL, C_MUTED);
            ColumnText.showTextAligned(totalPagesTpl, Element.ALIGN_LEFT,
                    new Phrase(String.valueOf(total), fPageNum), 1, 2, 0);
        }

        private static String buildAddrLine(Branch b) {
            StringBuilder sb = new StringBuilder();
            if (b.getAddress() != null && !b.getAddress().isBlank()) sb.append(b.getAddress());
            if (b.getCity()    != null && !b.getCity().isBlank())
                sb.append(sb.length() > 0 ? ", " : "").append(b.getCity());
            if (b.getCountry() != null && !b.getCountry().isBlank())
                sb.append(sb.length() > 0 ? ", " : "").append(b.getCountry());
            return sb.toString();
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════════

    /** Fetch the first ledger narration for a document — verbatim, never regenerated. */
    private String fetchNarration(Long refId, String refType) {
        List<PartyLedgerEntry> entries =
                ledgerRepo.findByReferenceIdAndReferenceType(refId, refType);
        if (entries != null && !entries.isEmpty()) {
            String n = entries.get(0).getNarration();
            return (n != null && !n.isBlank()) ? n : null;
        }
        return null;
    }

    /** Resolve party info for a sale from customer/supplier records. */
    private PartyInfo resolvePartyForSale(Long customerId, String customerName, Long businessId) {
        PartyInfo p = new PartyInfo();
        if (customerId == null) {
            p.name = safe(customerName, "Walk-in");
            return p;
        }
        if (customerId < 0) {
            // Negative namespace → supplier-backed party
            supplierRepo.findByIdAndBusinessId(-customerId, businessId).ifPresent(s -> {
                p.code    = safe(s.getPartyCode(), "");
                p.name    = safe(s.getName(), safe(customerName, "—"));
                p.address = safe(s.getAddress(), "");
                p.trn     = safe(s.getTradeLicenseNumber(), "");
                p.phone   = safe(s.getPhone(), "");
                p.email   = safe(s.getEmail(), "");
            });
            if (p.name == null) p.name = safe(customerName, "—");
        } else {
            // Positive ID → standard Customer
            customerRepo.findById(customerId).ifPresent(c -> {
                p.name  = safe(c.getFullName(), safe(customerName, "—"));
                p.phone = safe(c.getPhone(), "");
                p.email = safe(c.getEmail(), "");
                // If customer has a linked supplier, get TRN/address from there
                if (c.getLinkedSupplierId() != null) {
                    supplierRepo.findByIdAndBusinessId(c.getLinkedSupplierId(), businessId)
                            .ifPresent(s -> {
                                if (p.code    == null) p.code    = safe(s.getPartyCode(), "");
                                if (p.address == null) p.address = safe(s.getAddress(), "");
                                if (p.trn     == null) p.trn     = safe(s.getTradeLicenseNumber(), "");
                            });
                }
            });
            if (p.name == null) p.name = safe(customerName, "Walk-in");
        }
        // Null-guard remaining fields
        if (p.code    == null) p.code    = "";
        if (p.address == null) p.address = "";
        if (p.trn     == null) p.trn     = "";
        if (p.phone   == null) p.phone   = "";
        if (p.email   == null) p.email   = "";
        return p;
    }

    /** Add one key–value line to the voucher info cell. */
    private static void addInfoLine(PdfPCell cell, String label, String value,
                                     Font fLabel, Font fValue) {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(3f);
        p.add(new Chunk(label + ":  ", fLabel));
        p.add(new Chunk(value != null ? value : "—", fValue));
        cell.addElement(p);
    }

    /** Add one summary row (label left, value right) with optional background. */
    private static void addSumRow(PdfPTable tbl, String label, String value,
                                   Font fLabel, Font fValue, Color bg) {
        PdfPCell lc = new PdfPCell(new Phrase(label, fLabel));
        lc.setBackgroundColor(bg);
        lc.setHorizontalAlignment(Element.ALIGN_LEFT);
        lc.setPadding(4f);
        lc.setBorderColor(C_BORDER);
        lc.setBorderWidth(0.3f);
        tbl.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value != null ? value : "0.00", fValue));
        vc.setBackgroundColor(bg);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vc.setPadding(4f);
        vc.setBorderColor(C_BORDER);
        vc.setBorderWidth(0.3f);
        tbl.addCell(vc);
    }

    /** Format BigDecimal as "1,234.56". */
    private static String fmtAed(BigDecimal v) {
        if (v == null) return "0.00";
        return String.format("%,.2f", v);
    }

    /** Format double VAT percent without trailing zeros. */
    private static String fmtPct(double v) {
        if (v == Math.floor(v)) return String.valueOf((int) v);
        return String.valueOf(v);
    }

    /** Format LocalDateTime as "dd/MM/yyyy HH:mm". */
    private static String fmtDateTime(LocalDateTime dt) {
        if (dt == null) return "—";
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /** Null-safe BigDecimal — returns ZERO if null. */
    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /** Null-safe string — returns defaultVal if null or blank. */
    private static String safe(String v, String defaultVal) {
        return (v != null && !v.isBlank()) ? v : defaultVal;
    }

    /** Null-safe toString — returns defaultVal if null or blank. */
    private static String safe(Object v, String defaultVal) {
        if (v == null) return defaultVal;
        String s = v.toString().trim();
        return s.isBlank() ? defaultVal : s;
    }

    // ════════════════════════════════════════════════════════════════════
    //  INNER DATA CLASSES
    // ════════════════════════════════════════════════════════════════════

    /** All data needed to render one invoice page. */
    private static class InvoiceData {
        String      docType;
        String      voucherNumber;
        String      voucherDate;
        String      paymentMethod;
        String      cashierName;
        Business    business;
        Branch      branch;
        PartyInfo   party;
        BigDecimal  subtotal;
        BigDecimal  discountAmount;
        double      vatPercent;
        BigDecimal  vatAmount;
        BigDecimal  roundOffAmount;
        BigDecimal  totalAmount;
        BigDecimal  exchangeRate;
        List<ItemRow> rows;
        String      narration;
        boolean     includeDeclaration;
    }

    /** One line item row. */
    private static class ItemRow {
        final int    sn;
        final String description;
        final String purity;
        final String unit;
        final String qty;
        final String unitPrice;
        final String amount;

        ItemRow(int sn, String description, String purity,
                String unit, String qty, String unitPrice, String amount) {
            this.sn          = sn;
            this.description = description;
            this.purity      = purity;
            this.unit        = unit;
            this.qty         = qty;
            this.unitPrice   = unitPrice;
            this.amount      = amount;
        }
    }

    /** Party (customer or supplier) display info. */
    private static class PartyInfo {
        String code    = "";
        String name    = "—";
        String address = "";
        String trn     = "";
        String phone   = "";
        String email   = "";
    }
}
