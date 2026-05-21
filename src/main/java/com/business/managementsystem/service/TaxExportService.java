package com.business.managementsystem.service;

import com.business.managementsystem.model.SaleTransaction;
import com.business.managementsystem.repository.SaleTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TaxExportService {

    private final SaleTransactionRepository txRepository;

    public TaxExportService(SaleTransactionRepository txRepository) {
        this.txRepository = txRepository;
    }

    /**
     * Generates a CSV string containing FTA-compliant VAT tax export data.
     */
    @Transactional(readOnly = true)
    public String generateTaxExportCsv(Long businessId, Long branchId) {
        List<SaleTransaction> transactions;

        if (branchId != null) {
            transactions = txRepository.findByBusinessIdAndBranchIdOrderByCreatedAtDesc(businessId, branchId);
        } else {
            transactions = txRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // FTA required headers (approximate generic set)
        pw.println("Invoice Number,Invoice Date,Supply Time,Branch ID,Customer ID,Customer Name,Net Amount (AED),VAT Amount (AED),Total Amount (AED),Payment Method");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        for (SaleTransaction tx : transactions) {
            String invoiceNum = tx.getReceiptNumber() != null ? tx.getReceiptNumber() : "";
            String date = tx.getCreatedAt() != null ? tx.getCreatedAt().format(dateFormatter) : "";
            String time = tx.getCreatedAt() != null ? tx.getCreatedAt().format(timeFormatter) : "";
            String branch = tx.getBranchId() != null ? String.valueOf(tx.getBranchId()) : "";
            String custId = tx.getCustomerId() != null ? String.valueOf(tx.getCustomerId()) : "";
            String custName = tx.getCustomerName() != null ? tx.getCustomerName() : "";
            String netAmount = tx.getSubtotal() != null ? tx.getSubtotal().toString() : "0.00";
            String vatAmount = tx.getVatAmount() != null ? tx.getVatAmount().toString() : "0.00";
            String totalAmount = tx.getTotalAmount() != null ? tx.getTotalAmount().toString() : "0.00";
            String pm = tx.getPaymentMethod() != null ? tx.getPaymentMethod() : "";

            // Escape strings for CSV
            custName = escapeCsv(custName);

            pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    invoiceNum, date, time, branch, custId, custName, netAmount, vatAmount, totalAmount, pm);
        }

        pw.flush();
        return sw.toString();
    }

    private String escapeCsv(String value) {
        if (value.contains(",")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
