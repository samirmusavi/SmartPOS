package com.business.managementsystem.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.math.BigDecimal;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:smartpos@example.com}")
    private String fromEmail;

    @Value("${smartpos.email.enabled:true}")
    private boolean emailEnabled;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String userName, String resetLink) {
        if (!emailEnabled) return;
        if (toEmail == null || toEmail.isBlank()) return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset your SmartPOS password");

            String html = """
                <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:560px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden">

                    <div style="background:linear-gradient(135deg,#6366f1,#4f46e5);padding:28px 32px;text-align:center">
                        <h1 style="color:#ffffff;margin:0;font-size:22px;font-weight:700;letter-spacing:-0.02em">SmartPOS</h1>
                        <p style="color:rgba(255,255,255,0.8);margin:6px 0 0;font-size:13px">Password Reset Request</p>
                    </div>

                    <div style="padding:32px">
                        <p style="color:#374151;font-size:15px;line-height:1.6;margin:0 0 16px">
                            Hi <strong>%s</strong>,
                        </p>
                        <p style="color:#374151;font-size:15px;line-height:1.6;margin:0 0 28px">
                            We received a request to reset your password. Click the button below to choose a new one. This link expires in <strong>30 minutes</strong>.
                        </p>

                        <div style="text-align:center;margin-bottom:28px">
                            <a href="%s"
                               style="display:inline-block;background:#6366f1;color:#ffffff;text-decoration:none;padding:14px 32px;border-radius:8px;font-size:15px;font-weight:600;letter-spacing:-0.01em">
                                Reset Password
                            </a>
                        </div>

                        <p style="color:#6b7280;font-size:13px;line-height:1.6;margin:0 0 8px">
                            If the button doesn't work, copy and paste this link into your browser:
                        </p>
                        <p style="color:#6366f1;font-size:12px;word-break:break-all;margin:0 0 24px">%s</p>

                        <p style="color:#9ca3af;font-size:13px;line-height:1.6;margin:0">
                            If you didn't request a password reset, you can safely ignore this email — your password won't change.
                        </p>
                    </div>

                    <div style="background:#f9fafb;padding:16px 32px;text-align:center;border-top:1px solid #e5e7eb">
                        <p style="color:#9ca3af;font-size:11px;margin:0">
                            This link expires in 30 minutes and can only be used once.
                        </p>
                    </div>
                </div>
                """.formatted(userName, resetLink, resetLink);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            System.err.println("Failed to send password reset email to " + toEmail + ": " + e.getMessage());
        }
    }

    public void sendPurchaseOrderEmail(String supplierEmail,
                                       String supplierName,
                                       String businessName,
                                       String productName,
                                       double quantity,
                                       BigDecimal unitCost,
                                       BigDecimal totalCost,
                                       String expectedDelivery,
                                       String notes,
                                       String createdBy) {
        if (!emailEnabled) return;
        if (supplierEmail == null || supplierEmail.isBlank()) return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(supplierEmail);
            helper.setSubject("New Purchase Order from " + businessName);

            String unitCostStr = unitCost != null ? "AED " + unitCost.toPlainString() : "As agreed";
            String totalCostStr = totalCost != null ? "AED " + totalCost.toPlainString() : "TBD";
            String deliveryStr = expectedDelivery != null && !expectedDelivery.isBlank()
                    ? expectedDelivery : "As soon as possible";
            String notesStr = notes != null && !notes.isBlank() ? notes : "No additional notes";

            String html = """
                <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:600px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden">
                    
                    <div style="background:linear-gradient(135deg,#6366f1,#4f46e5);padding:28px 32px;text-align:center">
                        <h1 style="color:#ffffff;margin:0;font-size:22px;font-weight:700;letter-spacing:-0.02em">SmartPOS</h1>
                        <p style="color:rgba(255,255,255,0.8);margin:6px 0 0;font-size:13px">Purchase Order Notification</p>
                    </div>
                    
                    <div style="padding:28px 32px">
                        <p style="color:#374151;font-size:15px;line-height:1.6;margin:0 0 20px">
                            Dear <strong>%s</strong>,
                        </p>
                        <p style="color:#374151;font-size:15px;line-height:1.6;margin:0 0 24px">
                            A new purchase order has been placed by <strong>%s</strong>. Please review the details below and prepare the order for delivery.
                        </p>
                        
                        <div style="background:#f9fafb;border:1px solid #e5e7eb;border-radius:10px;padding:20px 24px;margin-bottom:24px">
                            <h3 style="color:#6366f1;font-size:13px;text-transform:uppercase;letter-spacing:0.08em;margin:0 0 16px;font-weight:700">Order Details</h3>
                            <table style="width:100%%;border-collapse:collapse">
                                <tr style="border-bottom:1px solid #e5e7eb">
                                    <td style="padding:10px 0;color:#6b7280;font-size:13px;font-weight:600;width:140px">Product</td>
                                    <td style="padding:10px 0;color:#111827;font-size:14px;font-weight:600">%s</td>
                                </tr>
                                <tr style="border-bottom:1px solid #e5e7eb">
                                    <td style="padding:10px 0;color:#6b7280;font-size:13px;font-weight:600">Quantity</td>
                                    <td style="padding:10px 0;color:#111827;font-size:14px;font-weight:600">%s units</td>
                                </tr>
                                <tr style="border-bottom:1px solid #e5e7eb">
                                    <td style="padding:10px 0;color:#6b7280;font-size:13px;font-weight:600">Unit Cost</td>
                                    <td style="padding:10px 0;color:#111827;font-size:14px">%s</td>
                                </tr>
                                <tr style="border-bottom:1px solid #e5e7eb">
                                    <td style="padding:10px 0;color:#6b7280;font-size:13px;font-weight:600">Total Cost</td>
                                    <td style="padding:10px 0;color:#6366f1;font-size:16px;font-weight:700">%s</td>
                                </tr>
                                <tr style="border-bottom:1px solid #e5e7eb">
                                    <td style="padding:10px 0;color:#6b7280;font-size:13px;font-weight:600">Expected Delivery</td>
                                    <td style="padding:10px 0;color:#111827;font-size:14px">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding:10px 0;color:#6b7280;font-size:13px;font-weight:600">Notes</td>
                                    <td style="padding:10px 0;color:#111827;font-size:14px">%s</td>
                                </tr>
                            </table>
                        </div>
                        
                        <p style="color:#6b7280;font-size:13px;line-height:1.6;margin:0 0 4px">
                            Ordered by: <strong style="color:#374151">%s</strong>
                        </p>
                        <p style="color:#6b7280;font-size:13px;line-height:1.6;margin:0">
                            Please confirm receipt of this order by replying to this email.
                        </p>
                    </div>
                    
                    <div style="background:#f9fafb;padding:16px 32px;text-align:center;border-top:1px solid #e5e7eb">
                        <p style="color:#9ca3af;font-size:11px;margin:0">
                            This is an automated message from SmartPOS. Do not reply directly — contact the business for inquiries.
                        </p>
                    </div>
                </div>
                """.formatted(
                    supplierName,
                    businessName,
                    productName,
                    String.valueOf(quantity),
                    unitCostStr,
                    totalCostStr,
                    deliveryStr,
                    notesStr,
                    createdBy != null ? createdBy : "System"
            );

            helper.setText(html, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            // Log but don't fail the order — email is non-critical
            System.err.println("Failed to send PO email to " + supplierEmail + ": " + e.getMessage());
        }
    }
}