package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.model.Sale;
import com.example.demo.model.SaleItem;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired private SaleRepository saleRepo;
    @Autowired private ProductRepository productRepo;

    // 🎯 TARGET: ₹20,000/day
    private static final double DAILY_TARGET = 20000.0;

    public Map<String, Object> getDashboardStats(LocalDate start, LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23, 59, 59);

        // 1. Get Filtered Sales (Today/Selected Range)
        List<Sale> sales = saleRepo.findSalesWithItems(startDt, endDt);
        
        // 2. 🚀 NEW: Get Lifetime Count (e.g., 84) to match Invoice IDs
        long lifetimeOrders = saleRepo.count();

        double totalRevenue = 0.0;
        double totalCost = 0.0;
        int periodOrders = sales.size(); // Orders in the selected range (e.g., 5 today)
        Set<String> lowStockItems = new HashSet<>();

        // 📊 Data Containers
        Map<String, Integer> productSales = new HashMap<>();
        Map<String, Double> categoryRevenue = new HashMap<>();
        Map<String, Double> staffPerformance = new HashMap<>();
        Map<String, Double> paymentStats = new HashMap<>();
        Map<Integer, Integer> hourlyTraffic = new HashMap<>(); 

        // Initialize Hourly Map (0 to 23 hours)
        for(int i=9; i<=21; i++) hourlyTraffic.put(i, 0); 

        for (Sale sale : sales) {
            double amount = sale.getTotalAmount().doubleValue();
            totalRevenue += amount;

            // 1. Payment Methods 
            String pMethod = (sale.getPaymentMethod() != null) ? sale.getPaymentMethod().toUpperCase() : "CASH";
            paymentStats.put(pMethod, paymentStats.getOrDefault(pMethod, 0.0) + amount);

            // 2. Staff Stats
            String cashier = (sale.getCashierName() != null) ? sale.getCashierName() : "Unknown";
            staffPerformance.put(cashier, staffPerformance.getOrDefault(cashier, 0.0) + amount);

            // 3. Hourly Traffic 
            int hour = sale.getDate().getHour();
            if(hour >= 9 && hour <= 21) {
                hourlyTraffic.put(hour, hourlyTraffic.getOrDefault(hour, 0) + 1);
            }

            // 4. Item Details
            if (sale.getItems() != null) {
                for (SaleItem item : sale.getItems()) {
                    Product p = productRepo.findById(item.getProductId()).orElse(null);
                    if (p != null) {
                        if (p.getCostPrice() != null) {
                            totalCost += p.getCostPrice() * item.getQuantity();
                        }
                        if (p.getStock() <= 5) {
                            lowStockItems.add(p.getName());
                        }
                        if (p.getCategory() != null) {
                            double lineTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())).doubleValue();
                            categoryRevenue.put(p.getCategory(), categoryRevenue.getOrDefault(p.getCategory(), 0.0) + lineTotal);
                        }
                        productSales.put(item.getProductName(), productSales.getOrDefault(item.getProductName(), 0) + item.getQuantity());
                    }
                }
            }
        }

        double totalProfit = totalRevenue - totalCost;
        double profitMargin = totalRevenue > 0 ? (totalProfit / totalRevenue) * 100 : 0.0;
        
        double targetPercent = (totalRevenue / DAILY_TARGET) * 100;
        if(targetPercent > 100) targetPercent = 100;

        // Sort Data
        Map<String, Integer> topProducts = productSales.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        Map<String, Object> response = new HashMap<>();
        response.put("totalRevenue", totalRevenue);
        response.put("totalOrders", periodOrders); // Today's Count
        response.put("lifetimeOrders", lifetimeOrders); // 🚀 Lifetime Count (84)
        response.put("lowStockCount", lowStockItems.size());
        response.put("totalProfit", totalProfit);
        response.put("profitMargin", profitMargin);
        
        response.put("topProducts", topProducts);
        response.put("categoryRevenue", categoryRevenue);
        response.put("staffPerformance", staffPerformance);
        response.put("paymentStats", paymentStats);
        response.put("hourlyTraffic", hourlyTraffic); 
        
        response.put("dailyTarget", DAILY_TARGET);
        response.put("targetPercent", targetPercent);
        
        response.put("recentSales", sales.stream()
                .sorted(Comparator.comparing(Sale::getDate).reversed())
                .limit(10)
                .collect(Collectors.toList()));

        return response;
    }
}