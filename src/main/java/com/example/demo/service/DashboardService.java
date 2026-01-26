package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.model.Sale;
import com.example.demo.model.SaleItem;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired private SaleRepository saleRepo;
    @Autowired private ProductRepository productRepo;

    public Map<String, Object> getDashboardStats(LocalDate start, LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23, 59, 59);

        // 1. Fetch Sales with Items (Optimized)
        List<Sale> sales = saleRepo.findSalesWithItems(startDt, endDt);

        double totalRevenue = 0.0;
        double totalCost = 0.0;
        int totalOrders = sales.size();
        Set<String> lowStockItems = new HashSet<>();

        // 2. Loop through sales to calculate Profit
        for (Sale sale : sales) {
            totalRevenue += sale.getTotalAmount().doubleValue();
            
            for (SaleItem item : sale.getItems()) {
                // Get the cost price from the Product table
                Product p = productRepo.findById(item.getProductId()).orElse(null);
                if (p != null) {
                    if (p.getCostPrice() != null) {
                        totalCost += p.getCostPrice() * item.getQuantity();
                    }
                    if (p.getStock() <= 5) {
                        lowStockItems.add(p.getName());
                    }
                }
            }
        }

        // 3. Math for Profit & Margin
        double totalProfit = totalRevenue - totalCost;
        double profitMargin = totalRevenue > 0 ? (totalProfit / totalRevenue) * 100 : 0.0;

        // 4. Calculate Top Products & Categories
        Map<String, Integer> productSales = new HashMap<>();
        Map<String, Double> categoryRevenue = new HashMap<>();
        Map<String, Double> staffPerformance = new HashMap<>();

        for (Sale sale : sales) {
            // Staff Stats
            String cashier = sale.getCashierName() != null ? sale.getCashierName() : "Unknown";
            staffPerformance.put(cashier, staffPerformance.getOrDefault(cashier, 0.0) + sale.getTotalAmount().doubleValue());

            for (SaleItem item : sale.getItems()) {
                // Product Stats
                productSales.put(item.getProductName(), productSales.getOrDefault(item.getProductName(), 0) + item.getQuantity());
                
                // Category Stats
                Product p = productRepo.findById(item.getProductId()).orElse(null);
                if(p != null && p.getCategory() != null) {
                    categoryRevenue.put(p.getCategory(), categoryRevenue.getOrDefault(p.getCategory(), 0.0) + (item.getPrice() * item.getQuantity()));
                }
            }
        }
        
        // Sort Top 5 Products
        Map<String, Integer> topProducts = productSales.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // 5. Build Response
        Map<String, Object> response = new HashMap<>();
        response.put("totalRevenue", totalRevenue);
        response.put("totalOrders", totalOrders);
        response.put("lowStockCount", lowStockItems.size());
        response.put("totalProfit", totalProfit); // 💰
        response.put("profitMargin", profitMargin); // 📈
        response.put("topProducts", topProducts);
        response.put("categoryRevenue", categoryRevenue);
        response.put("staffPerformance", staffPerformance);
        
        // Add recent sales for the table
        response.put("recentSales", sales.stream()
                .sorted(Comparator.comparing(Sale::getDate).reversed())
                .limit(10)
                .collect(Collectors.toList()));

        return response;
    }
}