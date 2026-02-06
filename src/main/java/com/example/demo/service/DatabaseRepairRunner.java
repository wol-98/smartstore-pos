package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.model.Sale;
import com.example.demo.model.SaleItem;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DatabaseRepairRunner implements CommandLineRunner {

    @Autowired private SaleRepository saleRepo;
    @Autowired private ProductRepository productRepo;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("🔧 Database Repair Agent: Checking for broken records...");

        // 1. Load all known Products into a "Lookup Map" (Name -> ID)
        // This makes searching super fast (no repeated database queries)
        List<Product> allProducts = productRepo.findAll();
        Map<String, Long> productMap = new HashMap<>();
        
        for (Product p : allProducts) {
            // We use lowercase to make matching easier (case-insensitive)
            productMap.put(p.getName().toLowerCase().trim(), p.getId());
        }

        // 2. Scan History for Broken Items
        List<Sale> allSales = saleRepo.findAll();
        int fixedCount = 0;

        for (Sale sale : allSales) {
            boolean isSaleDirty = false; // Flag to track if we changed anything

            if (sale.getItems() != null) {
                for (SaleItem item : sale.getItems()) {
                    
                    // 🚩 FOUND IT: An old item with missing Product ID
                    if (item.getProductId() == null) {
                        String nameToFind = item.getProductName().toLowerCase().trim();
                        
                        // Try to find the ID in our map
                        if (productMap.containsKey(nameToFind)) {
                            Long foundId = productMap.get(nameToFind);
                            
                            // 🛠️ FIX IT: Update the ID
                            item.setProductId(foundId);
                            isSaleDirty = true;
                            fixedCount++;
                        }
                    }
                }
            }

            // 3. Save changes only if we fixed something in this sale
            if (isSaleDirty) {
                saleRepo.save(sale);
            }
        }

        if (fixedCount > 0) {
            System.out.println("✅ REPAIR COMPLETE: Successfully backfilled IDs for " + fixedCount + " items.");
        } else {
            System.out.println("✨ System Healthy: No broken records found.");
        }
    }
}