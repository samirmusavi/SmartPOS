package com.business.managementsystem.controller;

import com.business.managementsystem.service.TaxExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tax")
public class TaxExportController {

    private final TaxExportService taxExportService;

    public TaxExportController(TaxExportService taxExportService) {
        this.taxExportService = taxExportService;
    }

    private Long parseLongHeader(String header) {
        if (header == null || header.isBlank()) return null;
        try { return Long.parseLong(header); }
        catch (NumberFormatException e) { return null; }
    }

    private Long requireBusinessId(String header) {
        Long id = parseLongHeader(header);
        if (id == null) {
            throw new RuntimeException("Business ID header is required.");
        }
        return id;
    }

    // GET /api/tax/export
    @GetMapping(value = "/export", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<byte[]> exportTaxCsv(
            @RequestHeader(value = "X-Business-Id", required = false) String bh,
            @RequestHeader(value = "X-Branch-Id", required = false) String brh) {

        Long businessId = requireBusinessId(bh);
        Long branchId = parseLongHeader(brh);

        String csvData = taxExportService.generateTaxExportCsv(businessId, branchId);
        byte[] output = csvData.getBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "fta_vat_export_" + businessId + ".csv");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok()
                .headers(headers)
                .body(output);
    }
}
