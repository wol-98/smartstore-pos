package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.model.Sale;
import com.example.demo.model.SaleItem;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired private SaleRepository saleRepo;
    @Autowired private ProductRepository productRepo;

    public List<Product> getRecommendations(Long productId) {
        List<Long> recommendedIds = new ArrayList<>();

        // --- 🥇 PLAN A: HISTORY (Collaborative Filtering) ---
        // "People who bought this also bought..."
        List<Sale> salesWithProduct = saleRepo.findAll().stream()
                // 🛡️ SAFETY CHECK 1: Ensure Items list exists
                .filter(sale -> sale.getItems() != null)
                // 🛡️ SAFETY CHECK 2: Handle Null Product IDs here to prevent crashes
                .filter(sale -> sale.getItems().stream()
                        .anyMatch(item -> item.getProductId() != null && item.getProductId().equals(productId)))
                .collect(Collectors.toList());

        if (!salesWithProduct.isEmpty()) {
            Map<Long, Integer> frequencyMap = new HashMap<>();
            for (Sale sale : salesWithProduct) {
                for (SaleItem item : sale.getItems()) {
                    // 🛡️ SAFETY CHECK 3: Skip other broken items in the cart
                    if (item.getProductId() != null && !item.getProductId().equals(productId)) {
                        frequencyMap.put(item.getProductId(), 
                                frequencyMap.getOrDefault(item.getProductId(), 0) + 1);
                    }
                }
            }
            // Sort by frequency (Highest first)
            recommendedIds.addAll(frequencyMap.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList()));
        }

        // --- 🥈 PLAN B: CATEGORY (Content-Based) ---
        // "If we don't have enough history, show similar items."
        if (recommendedIds.size() < 3) {
            Product currentProduct = productRepo.findById(productId).orElse(null);
            
            if (currentProduct != null && currentProduct.getCategory() != null) {
                List<Product> categoryMates = productRepo.findAll().stream()
                        .filter(p -> p.getCategory() != null && p.getCategory().equals(currentProduct.getCategory()))
                        .filter(p -> !p.getId().equals(productId)) 
                        .filter(p -> !recommendedIds.contains(p.getId())) // Don't duplicate Plan A
                        .limit(3)
                        .collect(Collectors.toList());

                for (Product p : categoryMates) {
                    recommendedIds.add(p.getId());
                }
            }
        }

        // --- 🥉 PLAN C: GLOBAL BEST SELLERS (Popularity Fallback) ---
        // "If we STILL have empty slots, show what everyone loves."
        if (recommendedIds.size() < 3) {
            List<Long> bestSellers = getGlobalBestSellers();
            
            for (Long id : bestSellers) {
                if (recommendedIds.size() >= 3) break; // Stop if full
                if (!id.equals(productId) && !recommendedIds.contains(id)) {
                    recommendedIds.add(id);
                }
            }
        }

        // Return final list (Top 3)
        if (recommendedIds.isEmpty()) return new ArrayList<>();
        
        return productRepo.findAllById(recommendedIds).stream()
                .limit(3)
                .collect(Collectors.toList());
    }

    // Helper: Calculates the most sold items in the entire store
    private List<Long> getGlobalBestSellers() {
        Map<Long, Integer> globalFreq = new HashMap<>();
        List<Sale> allSales = saleRepo.findAll();

        for (Sale sale : allSales) {
            if (sale.getItems() == null) continue;
            for (SaleItem item : sale.getItems()) {
                // 🛡️ SAFETY CHECK 4: Ensure ID exists before counting
                if (item.getProductId() != null) {
                    globalFreq.put(item.getProductId(), 
                            globalFreq.getOrDefault(item.getProductId(), 0) + item.getQuantity());
                }
            }
        }

        return globalFreq.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // Sort Descending
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}