package com.business.managementsystem.controller;

import com.business.managementsystem.dto.SaleDTO;
import com.business.managementsystem.service.SaleService;
import com.business.managementsystem.service.TaxInvoicePdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService          saleService;
    private final TaxInvoicePdfService taxInvoicePdfService;

    public SaleController(SaleService saleService,
                          TaxInvoicePdfService taxInvoicePdfService) {
        this.saleService          = saleService;
        this.taxInvoicePdfService = taxInvoicePdfService;
    }

    private Long parseLongHeader(String header) {
        if (header == null || header.isBlank()) return null;
        try { return Long.parseLong(header); }
        catch (NumberFormatException e) { return null; }
    }

    private Long requireBusinessId(String header) {
        Long id = parseLongHeader(header);
        if (id == null)
            throw new RuntimeException("Business ID header is required.");
        return id;
    }

    // POST /api/sales/transaction
    // Accepts the full cart in one call.
    // Creates ONE transaction (one receipt number) + one sale row per item.
    // Body: {
    //   items: [ { productId, quantity }, ... ],
    //   paymentMethod: "Cash",
    //   amountReceived: 100.00,
    //   cashierName: "Ahmed"
    // }
    @PostMapping("/transaction")
    public ResponseEntity<Map<String, Object>> recordTransaction(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Business-Id",
                    required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",
                    required = false) String brh) {

        Long businessId = requireBusinessId(bh);
        Long branchId   = parseLongHeader(brh);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items =
                (List<Map<String, Object>>) request.get("items");

        String paymentMethod = request.containsKey("paymentMethod")
                ? request.get("paymentMethod").toString() : null;

        BigDecimal amountReceived = null;
        if (request.containsKey("amountReceived") &&
                request.get("amountReceived") != null) {
            amountReceived = new BigDecimal(
                    request.get("amountReceived").toString());
        }

        String cashierName = request.containsKey("cashierName")
                ? request.get("cashierName").toString() : null;

        Long customerId = request.containsKey("customerId") && request.get("customerId") != null
                ? Long.parseLong(request.get("customerId").toString()) : null;
        
        String customerName = request.containsKey("customerName") && request.get("customerName") != null
                ? request.get("customerName").toString() : null;

        BigDecimal discountAmount = request.containsKey("discountAmount") && request.get("discountAmount") != null
                ? new BigDecimal(request.get("discountAmount").toString()) : BigDecimal.ZERO;
        
        double vatPercent = request.containsKey("vatPercent") && request.get("vatPercent") != null
                ? Double.parseDouble(request.get("vatPercent").toString()) : 5.0;

        BigDecimal exchangeRate = request.containsKey("exchangeRate") && request.get("exchangeRate") != null
                ? new BigDecimal(request.get("exchangeRate").toString()) : null;

        String pricingMethod = request.containsKey("pricingMethod") && request.get("pricingMethod") != null
                ? request.get("pricingMethod").toString() : null;

        BigDecimal goldOzRate = request.containsKey("goldOzRate") && request.get("goldOzRate") != null
                ? new BigDecimal(request.get("goldOzRate").toString()) : null;

        BigDecimal premiumAmount = request.containsKey("premiumAmount") && request.get("premiumAmount") != null
                ? new BigDecimal(request.get("premiumAmount").toString()) : null;

        BigDecimal roundOffAmount = request.containsKey("roundOffAmount") && request.get("roundOffAmount") != null
                ? new BigDecimal(request.get("roundOffAmount").toString()) : BigDecimal.ZERO;

        Boolean includeReverseChargeDeclaration = request.containsKey("includeReverseChargeDeclaration")
                && Boolean.TRUE.equals(request.get("includeReverseChargeDeclaration"));

        // Unfixed pricing fields (Stage 2)
        // FIXED_AT_TRADE = default; UNFIXED_AT_TRADE = metal-only ledger now, AED deferred
        String originalPricingMethod = request.containsKey("originalPricingMethod")
                && request.get("originalPricingMethod") != null
                ? request.get("originalPricingMethod").toString() : "FIXED_AT_TRADE";

        // Per-oz USD premium/discount locked at deal time (only relevant for UNFIXED_AT_TRADE)
        BigDecimal agreedPremiumDiscount = request.containsKey("agreedPremiumDiscount")
                && request.get("agreedPremiumDiscount") != null
                ? new BigDecimal(request.get("agreedPremiumDiscount").toString()) : null;

        Map<String, Object> result = saleService.recordTransaction(
                businessId, branchId, items,
                paymentMethod, amountReceived, cashierName, customerId, customerName,
                discountAmount, vatPercent, exchangeRate, pricingMethod, goldOzRate, premiumAmount,
                roundOffAmount, includeReverseChargeDeclaration,
                originalPricingMethod, agreedPremiumDiscount);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // POST /api/sales — legacy single-item endpoint (kept for compatibility)
    @PostMapping
    public ResponseEntity<SaleDTO> recordSale(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Business-Id",
                    required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",
                    required = false) String brh) {

        Long businessId = requireBusinessId(bh);
        Long branchId   = parseLongHeader(brh);
        Long productId  = Long.parseLong(
                request.get("productId").toString());
        double quantity = Double.parseDouble(
                request.get("quantity").toString());

        SaleDTO sale = saleService.recordSale(
                businessId, productId, quantity, branchId);
        return ResponseEntity.status(HttpStatus.CREATED).body(sale);
    }

    // GET /api/sales
    @GetMapping
    public ResponseEntity<List<SaleDTO>> getAllSales(
            @RequestHeader(value = "X-Business-Id",
                    required = false) String bh) {
        return ResponseEntity.ok(
                saleService.getAllSales(requireBusinessId(bh)));
    }

    // GET /api/sales/recent
    @GetMapping("/recent")
    public ResponseEntity<List<SaleDTO>> getRecentSales(
            @RequestHeader(value = "X-Business-Id",
                    required = false) String bh) {
        return ResponseEntity.ok(
                saleService.getRecentSales(requireBusinessId(bh)));
    }

    // GET /api/sales/report
    // Branch-filtered when X-Branch-Id is present;
    // combined across all branches when absent (owner global view).
    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> getFullReport(
            @RequestHeader(value = "X-Business-Id",
                    required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",
                    required = false) String brh) {
        Long businessId = requireBusinessId(bh);
        Long branchId   = parseLongHeader(brh);
        return ResponseEntity.ok(
                saleService.getFullReport(businessId, branchId));
    }

    // GET /api/sales/transactions/list
    // Paginated + filterable transaction list for the Sales List page.
    @GetMapping("/transactions/list")
    public ResponseEntity<Map<String, Object>> getTransactionList(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "20") int    size,
            @RequestParam(required = false)    String search,
            @RequestParam(required = false)    String dateFrom,
            @RequestParam(required = false)    String dateTo) {

        Long businessId = requireBusinessId(bh);
        Long branchId   = parseLongHeader(brh);
        return ResponseEntity.ok(
                saleService.getTransactionList(businessId, branchId,
                        page, size, search, dateFrom, dateTo));
    }

    // GET /api/sales/transactions
    // Returns all transactions (one per checkout) for the transaction history table.
    // Each transaction has receipt number, total, items list, etc.
    @GetMapping("/transactions")
    public ResponseEntity<List<Map<String, Object>>> getTransactions(
            @RequestHeader(value = "X-Business-Id",
                    required = false) String bh,
            @RequestHeader(value = "X-Branch-Id",
                    required = false) String brh) {
        Long businessId = requireBusinessId(bh);
        Long branchId   = parseLongHeader(brh);
        return ResponseEntity.ok(
                saleService.getAllTransactions(businessId, branchId));
    }

    // GET /api/sales/receipt/{receiptNumber}
    // Returns full transaction details including all items,
    // branch, cashier, totals, payment info.
    @GetMapping("/receipt/{receiptNumber}")
    public ResponseEntity<Map<String, Object>> getReceipt(
            @PathVariable String receiptNumber,
            @RequestHeader(value = "X-Business-Id",
                    required = false) String bh) {
        return ResponseEntity.ok(
                saleService.getByReceiptNumber(
                        receiptNumber, requireBusinessId(bh)));
    }

    // GET /api/sales/{id}/invoice/pdf
    // Generates and streams a Tax Invoice (Fixed) as a PDF for the given sale transaction.
    // Authentication: Bearer token via X-Business-Id header (same as all other endpoints).
    @GetMapping("/{id}/invoice/pdf")
    public ResponseEntity<byte[]> getSaleInvoicePdf(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String bh) {
        try {
            Long businessId = requireBusinessId(bh);
            byte[] pdfBytes = taxInvoicePdfService.generateSaleInvoicePdf(id, businessId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"tax_invoice_sale_" + id + ".pdf\"");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
