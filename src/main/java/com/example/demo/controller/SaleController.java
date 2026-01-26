package com.example.demo.controller;

import com.example.demo.model.Sale;
import com.example.demo.repository.SaleRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.ExcelService;
import com.example.demo.service.PdfService;
import com.example.demo.service.SaleService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.mail.internet.MimeMessage; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource; 
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender; 
import org.springframework.mail.javamail.MimeMessageHelper; 
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
    
    @Autowired private JavaMailSender mailSender;

    // --- 1. CREATE SALE ---
    @PostMapping
    public ResponseEntity<?> createSale(@RequestBody Map<String, Object> saleData) {
        try {
            Sale newSale = saleService.processSale(saleData);
            return ResponseEntity.ok(Map.of("id", newSale.getId(), "message", "Sale successful"));
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
        
        // Ensure you have 'commons-io' dependency in pom.xml for this line
        org.apache.commons.io.IOUtils.copy(pdfStream, response.getOutputStream());
    }

    // --- 3. SHARE INVOICE (Background Thread) ---
    @PostMapping("/{id}/share")
    public ResponseEntity<?> shareInvoice(@PathVariable Long id, @RequestParam String email) {
        
        // 🚀 Fetch Sale WITH items to prevent "LazyInitializationException"
        Sale sale = saleRepo.findByIdWithItems(id)
                .orElseThrow(() -> new RuntimeException("Sale not found"));

        // 🚀 Run Email Logic in Background Thread
        new Thread(() -> {
            try {
                // Generate PDF
                ByteArrayInputStream pdfStream = pdfService.generateInvoice(sale);
                byte[] pdfBytes = pdfStream.readAllBytes();

                // Prepare Email
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                
                helper.setFrom("SmartStore POS <grabmeonly@gmail.com>");
                helper.setTo(email);
                helper.setSubject("🧾 Your SmartStore Invoice #" + id);
                helper.setText("Hello,\n\nThank you for shopping with us! Please find your invoice attached.\n\nBest Regards,\nSmartStore Team");
                
                helper.addAttachment("Invoice-" + id + ".pdf", new ByteArrayResource(pdfBytes));
                
                mailSender.send(message);
                System.out.println("✅ Background Email Sent to " + email);
                
            } catch (Exception e) {
                System.err.println("❌ Background Email Failed: " + e.getMessage());
            }
        }).start();

        return ResponseEntity.ok("Email queued for sending to " + email);
    }

    // --- 4. EXPORT EXCEL ---
    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportSalesToExcel() {
        List<Sale> sales = saleRepo.findAll();
        ByteArrayInputStream in = excelService.exportSalesToExcel(sales);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=sales-report.xlsx");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}