package com.example.demo.controller;

import com.example.demo.model.Customer;
import com.example.demo.model.Sale;
import com.example.demo.model.SaleItem;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.ExcelService;
import com.example.demo.service.PdfService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    @Autowired private SaleRepository saleRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private CustomerRepository customerRepo;
    
    @Autowired private PdfService pdfService;
    @Autowired private EmailService emailService;
    @Autowired private ExcelService excelService;

    // --- 1. CREATE SALE ---
    @PostMapping
    public Sale createSale(@RequestBody Map<String, Object> saleData) {
        Sale sale = new Sale();
        sale.setPaymentMethod((String) saleData.get("paymentMethod"));
        sale.setStatus((String) saleData.get("status"));
        sale.setCashierName((String) saleData.get("cashierName"));
        sale.setDate(java.time.LocalDateTime.now());

        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) saleData.get("items");
        BigDecimal total = BigDecimal.ZERO;
        List<SaleItem> saleItems = new ArrayList<>();

        for (Map<String, Object> itemData : itemsData) {
            SaleItem item = new SaleItem();
            item.setProductName((String) itemData.get("name"));
            
            double priceVal = ((Number) itemData.get("price")).doubleValue();
            item.setPrice(BigDecimal.valueOf(priceVal));

            int qty = ((Number) itemData.get("quantity")).intValue();
            item.setQuantity(qty);
            
            item.setSale(sale);
            saleItems.add(item);

            int prodId = ((Number) itemData.get("productId")).intValue();
            productRepo.findById((long) prodId).ifPresent(p -> {
                p.setStock(p.getStock() - qty);
                productRepo.save(p);
            });
            
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(qty)));
        }
        
        sale.setItems(saleItems);
        sale.setTotalAmount(total);

        // 🧠 CRM LOGIC: Calculate & Save Points 🧠
        String phone = (String) saleData.get("customerPhone");
        
        // 🚨 IMPORTANT: Save the phone to the Sale Record for the PDF
        sale.setCustomerPhone(phone); 

        if (phone != null && !phone.isEmpty()) {
            // 1. Calculate Points (1 point per $10)
            int earnedPoints = total.intValue() / 10;
            
            // 2. Find or Create Customer
            Customer cust = customerRepo.findByPhone(phone).orElse(new Customer());
            if(cust.getPhone() == null) {
                cust.setPhone(phone);
                cust.setName("Loyal Customer"); // Default name
                cust.setPoints(0);
            }
            
            // 3. Update Balance
            cust.setPoints(cust.getPoints() + earnedPoints);
            customerRepo.save(cust);
        }

        return saleRepo.save(sale);
    }

    // --- 2. DOWNLOAD PDF ---
    @GetMapping("/{id}/pdf")
    public void downloadReceipt(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Sale sale = saleRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found"));
        
        ByteArrayInputStream pdfStream = pdfService.generateInvoice(sale);
        
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=invoice-" + id + ".pdf");
        
        org.apache.commons.io.IOUtils.copy(pdfStream, response.getOutputStream());
    }

    // --- 3. SEND EMAIL ---
    @PostMapping("/{id}/email")
    public ResponseEntity<?> sendReceiptEmail(@PathVariable Long id, @RequestParam String email) {
        try {
            Sale sale = saleRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Sale not found"));
            
            emailService.sendReceiptWithAttachment(email, sale);
            
            return ResponseEntity.ok().body("Email sent successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error sending email: " + e.getMessage());
        }
    }

    // --- 4. EXPORT EXCEL ---
    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportSalesToExcel() {
        List<Sale> sales = saleRepo.findAll();
        
        ByteArrayInputStream in = excelService.exportSalesToExcel(sales);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=sales-report.xlsx");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(new InputStreamResource(in));
    }
}