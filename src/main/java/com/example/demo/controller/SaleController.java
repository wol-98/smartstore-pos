package com.example.demo.controller;

import com.example.demo.model.Sale;
import com.example.demo.repository.SaleRepository;
import com.example.demo.service.EmailService; // ✅ Active
import com.example.demo.service.PdfService;
import com.example.demo.service.SaleService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    @Autowired private SaleRepository saleRepo;
    @Autowired private SaleService saleService;
    @Autowired private PdfService pdfService;
    
    @Autowired private EmailService emailService; // ✅ WIRED UP

    // --- 1. CREATE SALE ---
    @PostMapping
    public ResponseEntity<?> createSale(@RequestBody Map<String, Object> saleData) {
        try {
            Sale newSale = saleService.createSale(saleData);
            return ResponseEntity.ok(Map.of("id", newSale.getId(), "message", "Sale successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing sale: " + e.getMessage());
        }
    }

    // --- 2. DOWNLOAD PDF ---
    @GetMapping("/{id}/pdf")
    public void downloadReceipt(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Sale sale = saleRepo.findByIdWithItems(id)
                .orElseThrow(() -> new RuntimeException("Sale not found"));
        
        ByteArrayInputStream pdfStream = pdfService.generateInvoice(sale);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=invoice-" + id + ".pdf");
        pdfStream.transferTo(response.getOutputStream());
    }

    // --- 3. SHARE INVOICE (✅ FIXED) ---
    @PostMapping("/{id}/share")
    public ResponseEntity<?> shareInvoice(@PathVariable Long id, @RequestParam String email) {
        try {
            // 1. Fetch Sale + Items (Critical for PDF generation)
            Sale sale = saleRepo.findByIdWithItems(id)
                    .orElseThrow(() -> new RuntimeException("Sale not found"));

            // 2. Call Email Service
            // We run this in a separate thread so the UI doesn't freeze while sending
            new Thread(() -> {
                try {
                    emailService.sendReceiptWithAttachment(email, sale);
                } catch (Exception e) {
                    System.err.println("❌ Error sending email: " + e.getMessage());
                }
            }).start();

            return ResponseEntity.ok("Email is being sent to " + email);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to initiate email: " + e.getMessage());
        }
    }

    // --- 4. EXPORT EXCEL ---
    @GetMapping("/export")
    public ResponseEntity<String> exportSalesToExcel() {
        return ResponseEntity.ok("Excel feature pending");
    }
}