package com.example.demo.service;

import com.example.demo.model.Sale;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PdfService pdfService;

    // 1. Send Receipt to Customer (With PDF Attachment)
    public void sendReceiptWithAttachment(String toEmail, Sale sale) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true = multipart (needed for attachments)
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("SmartStore POS <grabmeonly@gmail.com>");
            helper.setTo(toEmail);
            helper.setSubject("Receipt #" + sale.getId());
            helper.setText("<h1>Thank you!</h1><p>Please find your receipt attached here.</p>", true);

            // Generate PDF
            ByteArrayInputStream pdfStream = pdfService.generateInvoice(sale);
            byte[] pdfBytes = pdfStream.readAllBytes();

            helper.addAttachment("invoice-" + sale.getId() + ".pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            System.out.println("✅ Email Sent Successfully to " + toEmail);

        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    // 🚀 2. NEW: Send Simple Alert (For Admin Notifications)
    public void sendSimpleEmail(String toEmail, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // false = not multipart (text only), "utf-8" = encoding
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "utf-8");

            helper.setFrom("SmartStore Admin <grabmeonly@gmail.com>");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true); // true = enable HTML

            mailSender.send(message);
            System.out.println("🔔 Alert Email Sent to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("❌ Failed to send alert: " + e.getMessage());
        }
    }
}