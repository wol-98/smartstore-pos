package com.example.demo.controller;

import com.example.demo.model.Sale;
import com.example.demo.repository.SaleRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.ExcelService;
import com.example.demo.service.PdfService;
import com.example.demo.service.SaleService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.mail.internet.MimeMessage; // 👈 Required for Email
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource; // 👈 Required for Attachment
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender; // 👈 Required for Email
import org.springframework.mail.javamail.MimeMessageHelper; // 👈 Required for Email
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    @Autowired private SaleRepository saleRepo;
    @Autowired private SaleService saleService;
    
    @Autowired private PdfService pdfService;
    @Autowired private EmailService emailService;
    @Autowired private ExcelService excelService;
    
    // 📧 Inject JavaMailSender directly for the new Share feature
    @Autowired private JavaMailSender mailSender;

    // --- 1. CREATE SALE (Logic delegated to Service) ---
    @PostMapping
    public ResponseEntity<?> createSale(@RequestBody Map<String, Object> saleData) {
        try {
            Sale newSale = saleService.processSale(saleData);
            return ResponseEntity.ok(newSale);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing sale: " + e.getMessage());
        }
    }

    // --- 2. DOWNLOAD PDF ---
    @GetMapping("/{id}/pdf")
    public void downloadReceipt(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Sale sale = saleRepo.findById(id).orElseThrow(() -> new RuntimeException("Sale not found"));
        ByteArrayInputStream pdfStream = pdfService.generateInvoice(sale);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=invoice-" + id + ".pdf");
        org.apache.commons.io.IOUtils.copy(pdfStream, response.getOutputStream());
    }

    // --- 3. SEND EMAIL (Legacy Endpoint) ---
    @PostMapping("/{id}/email")
    public ResponseEntity<?> sendReceiptEmail(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        try {
            Sale sale = saleRepo.findById(id).orElseThrow(() -> new RuntimeException("Sale not found"));
            String targetEmail = (body != null && body.containsKey("email")) ? body.get("email") : "grabmeonly@gmail.com";
            emailService.sendReceiptWithAttachment(targetEmail, sale);
            return ResponseEntity.ok().body("Email sent successfully to " + targetEmail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error sending email: " + e.getMessage());
        }
    }

    // --- 🚀 4. SHARE INVOICE (NEW Frontend Integration) ---
    @PostMapping("/{id}/share")
    public ResponseEntity<?> shareInvoice(@PathVariable Long id, @RequestParam String email) {
        try {
            // 1. Find Sale
            Sale sale = saleRepo.findById(id).orElseThrow(() -> new RuntimeException("Sale not found"));
            
            // 2. Generate PDF Bytes
            ByteArrayInputStream pdfStream = pdfService.generateInvoice(sale);
            byte[] pdfBytes = pdfStream.readAllBytes(); // Convert stream to byte array

            // 3. Prepare Email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(email);
            helper.setSubject("🧾 Your SmartStore Invoice #" + id);
            helper.setText("Hello,\n\nThank you for shopping with us! Please find your invoice attached.\n\nBest Regards,\nSmartStore Team");
            
            // 4. Attach PDF
            helper.addAttachment("Invoice-" + id + ".pdf", new ByteArrayResource(pdfBytes));
            
            // 5. Send
            mailSender.send(message);
            return ResponseEntity.ok("Email sent successfully to " + email);
            
        } catch (Exception e) {
            e.printStackTrace(); // Log error for debugging
            return ResponseEntity.internalServerError().body("Error sending email: " + e.getMessage());
        }
    }

    // --- 5. EXPORT EXCEL ---
    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportSalesToExcel() {
        List<Sale> sales = saleRepo.findAll();
        ByteArrayInputStream in = excelService.exportSalesToExcel(sales);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=sales-report.xlsx");
        return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("application/vnd.ms-excel")).body(new InputStreamResource(in));
    }
}