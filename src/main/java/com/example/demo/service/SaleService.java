package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SaleService {

    @Autowired private SaleRepository saleRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private CustomerRepository customerRepo; 

    @Transactional
    public Sale createSale(Map<String, Object> payload) { 
        
        // 1. Extract Data
        String cashierName = (String) payload.getOrDefault("cashierName", "Unknown");
        String payMethod = (String) payload.getOrDefault("paymentMethod", "Cash");
        String status = (String) payload.getOrDefault("status", "Paid");
        String custPhone = (String) payload.getOrDefault("customerPhone", "");

        Sale sale = new Sale();
        sale.setCashierName(cashierName);
        sale.setPaymentMethod(payMethod);
        sale.setStatus(status);
        sale.setDate(LocalDateTime.now());

        // 2. Process Items
        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) payload.get("items");
        List<SaleItem> saleItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map<String, Object> item : itemsData) {
            Long prodId = Long.valueOf(item.get("productId").toString());
            int qty = Integer.parseInt(item.get("quantity").toString());

            Product p = productRepo.findById(prodId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + prodId));

            // Stock Check
            if (p.getStock() < qty) {
                throw new RuntimeException("Not enough stock for: " + p.getName());
            }
            
            // Deduct Stock
            p.setStock(p.getStock() - qty);
            productRepo.save(p);

            // Create Sale Item
            SaleItem si = new SaleItem();
            si.setProductId(p.getId());
            si.setProductName(p.getName());
            si.setQuantity(qty);
            
            BigDecimal price = BigDecimal.valueOf(Double.parseDouble(item.get("price").toString()));
            si.setPrice(price);
            si.setSale(sale); 
            
            saleItems.add(si);
            totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(qty)));
        }

        sale.setItems(saleItems);
        sale.setTotalAmount(totalAmount);

        // =================================================================================
        // 3. 🧠 ADVANCED FEATURE: TIERED LOYALTY SYSTEM (Gold/Silver Status)
        // =================================================================================
        if (custPhone != null && !custPhone.trim().isEmpty()) {
            Customer customer = customerRepo.findByPhone(custPhone);
            
            if (customer == null) {
                customer = new Customer();
                customer.setPhone(custPhone);
                customer.setName("Guest " + custPhone);
                customer.setPoints(0);
            }

            // --- 🚀 UPGRADE STARTS HERE ---
            // Logic: High value customers earn points FASTER (Gamification)
            double multiplier = 1.0;
            int currentPoints = customer.getPoints() != null ? customer.getPoints() : 0;

            if (currentPoints > 1000) {
                multiplier = 2.0;       // 👑 GOLD TIER: 2x Points
            } else if (currentPoints > 500) {
                multiplier = 1.5;       // 🥈 SILVER TIER: 1.5x Points
            }

            // Base: 1 Point per ₹10 spent
            int basePoints = totalAmount.intValue() / 10;
            
            // Final Calculation
            int finalPoints = (int) (basePoints * multiplier);
            
            customer.setPoints(currentPoints + finalPoints);
            // --- 🚀 UPGRADE ENDS HERE ---
            
            customerRepo.save(customer);
            sale.setCustomerId(customer.getId()); 
        }

        return saleRepo.save(sale);
    }
}