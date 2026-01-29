package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.model.Sale;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class AlertScheduler {

    @Autowired private ProductRepository productRepo;
    @Autowired private SaleRepository saleRepo;
    @Autowired private EmailService emailService;

    // TODO: Move this to application.properties for security
    private final String ADMIN_EMAIL = "grabmeonly@gmail.com"; 

    // 🕒 Check every 4 hours (14400000 ms)
    @Scheduled(fixedRate = 14400000) 
    public void checkLowStock() {
        List<Product> products = productRepo.findAll();
        StringBuilder alertBody = new StringBuilder("<h3>⚠️ Low Stock Alert</h3><ul>");
        boolean needsEmail = false;
        List<Product> productsToUpdate = new ArrayList<>();

        System.out.println("🕵️ Scheduler Running: Checking " + products.size() + " products...");

        for (Product p : products) {
            if (p.getStock() == null) continue;

            // CASE 1: Low Stock & Not Yet Alerted
            if (p.getStock() <= 5 && !Boolean.TRUE.equals(p.getIsAlertSent())) {
                alertBody.append("<li><b>").append(p.getName())
                         .append("</b>: Only ").append(p.getStock()).append(" left!</li>");
                
                p.setIsAlertSent(true); 
                productsToUpdate.add(p);
                needsEmail = true;
                System.out.println("🔻 Low Stock Detected: " + p.getName());
            }
            
            // CASE 2: Restock Detected (Robustness Fix) 🛠️
            // Logic: Stock is healthy (>5) BUT the flag is still true. Reset it.
            else if (p.getStock() > 5 && Boolean.TRUE.equals(p.getIsAlertSent())) {
                p.setIsAlertSent(false);
                productsToUpdate.add(p);
                // 👇 The Fix: Now we know when a human acted!
                System.out.println("🔄 Restock Detected: Resetting alert flag for " + p.getName());
            }
        }
        alertBody.append("</ul><p>Please contact suppliers or approve Purchase Order.</p>");

        // 1. Batch Update Database
        if (!productsToUpdate.isEmpty()) {
            productRepo.saveAll(productsToUpdate);
        }

        // 2. Send Email ONLY if needed
        if (needsEmail) {
            System.out.println("📧 Sending Low Stock Email to Admin...");
            emailService.sendSimpleEmail(ADMIN_EMAIL, "⚠️ Inventory Warning - Action Needed", alertBody.toString());
        } else {
            System.out.println("✅ Stock Check Complete: No new alerts.");
        }
    }

    // 🌙 Daily Report at 10 PM
    @Scheduled(cron = "0 0 22 * * ?") 
    public void sendDailyReport() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

        List<Sale> todaysSales = saleRepo.findByDateBetween(startOfDay, endOfDay);
        
        BigDecimal totalRevenue = todaysSales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String subject = "📊 Daily Closing Report - " + LocalDate.now();
        String body = "<h3>End of Day Summary</h3>" +
                      "<p><b>Total Sales:</b> " + todaysSales.size() + "</p>" +
                      "<p><b>Total Revenue:</b> Rs. " + totalRevenue + "</p>" +
                      "<br><i>Automated by SmartStore POS</i>";

        emailService.sendSimpleEmail(ADMIN_EMAIL, subject, body);
        System.out.println("✅ Sent Daily Report.");
    }
}