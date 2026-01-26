package com.example.demo.service;

import com.example.demo.model.Customer;
import com.example.demo.model.Product;
import com.example.demo.model.Sale;
import com.example.demo.model.SaleItem;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    @Autowired private SaleRepository saleRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private CustomerRepository customerRepo;
    @Autowired private PredictionService predictionService; // 🤖 Connects to your AI Brain

    // 📘 KNOWLEDGE BASE (Static "How-To" Answers)
    private Map<String, String> knowledgeBase = new HashMap<>();

    @PostConstruct
    public void init() {
        knowledgeBase.put("export", "Click the green 'Export' button in the header to download Excel files.");
        knowledgeBase.put("kiosk", "Launch Kiosk Mode by clicking the 🚀 Rocket icon.");
        knowledgeBase.put("dark mode", "Toggle the theme using the 🌙 Moon icon in the top right.");
        knowledgeBase.put("invoice", "You can print thermal receipts or download PDFs from the 'Recent Sales' list.");
        knowledgeBase.put("supplier", "Manage vendors in the 'Supplier Master' section (Top Left).");
    }

    public String processQuery(String query) {
        query = query.toLowerCase();

        // 1. 🏷️ PRODUCT PRICE CHECK (Selling & Buying)
        if (query.contains("price of") || query.contains("how much is")) {
            return getProductPrice(query);
        }

        // 2. 🔢 INVENTORY COUNTS
        if (query.contains("how many products") || query.contains("total items")) {
            long count = productRepo.count();
            return "📦 We currently have " + count + " unique products in the inventory.";
        }

        // 3. 💰 PROFIT & REVENUE
        if (query.contains("profit") || query.contains("margin")) {
            return getProfitSummary();
        }
        if (query.contains("revenue") || query.contains("income") || query.contains("total sales")) {
            return getRevenueSummary(query);
        }

        // 4. 🔥 BEST SELLERS & CATEGORIES
        if (query.contains("best seller") || query.contains("top product")) {
            return getBestSellers();
        }
        if (query.contains("category") || query.contains("categories")) {
            return getCategorySummary();
        }

        // 5. 📉 STOCK ALERTS
        if (query.contains("stock") || query.contains("low")) {
            return getStockStatus();
        }

        // 6. 🤖 AI FORECAST
        if (query.contains("forecast") || query.contains("predict") || query.contains("tomorrow")) {
            return getAIForecast();
        }

        // 7. 🧾 LIVE TRANSACTIONS
        if (query.contains("transaction") || query.contains("recent sale") || query.contains("last sale")) {
            return getRecentTransactions();
        }

        // 8. 👥 STAFF & VIPs
        if (query.contains("staff") || query.contains("cashier")) {
            return getTopStaff();
        }
        if (query.contains("vip") || query.contains("customer")) {
            return getVIPCustomers();
        }

        // 9. 📘 KNOWLEDGE BASE FALLBACK
        for (String key : knowledgeBase.keySet()) {
            if (query.contains(key)) return "💡 " + knowledgeBase.get(key);
        }

        return "🤖 I can help! Try asking: 'Price of Apple', 'Net Profit', 'AI Forecast', or 'Top VIPs'.";
    }

    // ==========================================================
    // 🧠 ANALYTICAL LOGIC (The "Dynamic" Part)
    // ==========================================================

    private String getProductPrice(String query) {
        // Extract product name from query (e.g., "price of iphone")
        String search = query.replace("price of", "").replace("how much is", "").trim();
        List<Product> products = productRepo.findAll();
        
        // Find closest match
        Optional<Product> match = products.stream()
            .filter(p -> p.getName().toLowerCase().contains(search))
            .findFirst();

        if (match.isPresent()) {
            Product p = match.get();
            double profit = p.getPrice() - (p.getCostPrice() != null ? p.getCostPrice() : 0);
            return "🏷️ **" + p.getName() + "**\n" +
                   "• Selling Price: ₹" + p.getPrice() + "\n" +
                   "• Buying Price: ₹" + (p.getCostPrice() != null ? p.getCostPrice() : "N/A") + "\n" +
                   "• Profit per unit: ₹" + profit;
        }
        return "❌ I couldn't find a product named '" + search + "'.";
    }

    private String getProfitSummary() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        List<Sale> sales = saleRepo.findSalesWithItems(start, LocalDateTime.now());
        
        double revenue = 0;
        double cost = 0;
        for(Sale s : sales) {
            revenue += s.getTotalAmount().doubleValue();
            if(s.getItems() != null) {
                for(var item : s.getItems()) {
                     Product p = productRepo.findById(item.getProductId()).orElse(null);
                     if(p != null && p.getCostPrice() != null) cost += p.getCostPrice() * item.getQuantity();
                }
            }
        }
        return "💰 **Net Profit Today**\nRevenue: ₹" + revenue + "\nCost: ₹" + cost + "\n----------------\nProfit: ₹" + (revenue - cost);
    }

    private String getRevenueSummary(String query) {
        boolean isMonth = query.contains("month");
        LocalDateTime start = isMonth ? LocalDate.now().withDayOfMonth(1).atStartOfDay() : LocalDate.now().atStartOfDay();
        List<Sale> sales = saleRepo.findByDateAfter(start);
        double total = sales.stream().mapToDouble(s -> s.getTotalAmount().doubleValue()).sum();
        return "📊 **Total Revenue (" + (isMonth ? "This Month" : "Today") + ")**\n₹" + total + " across " + sales.size() + " orders.";
    }

    private String getBestSellers() {
        // Group all sale items by product name
        Map<String, Integer> counts = saleRepo.findAll().stream()
            .flatMap(s -> s.getItems().stream())
            .collect(Collectors.groupingBy(SaleItem::getProductName, Collectors.summingInt(SaleItem::getQuantity)));
        
        return "🔥 **Top 3 Best Sellers:**\n" +
               counts.entrySet().stream()
                   .sorted((a,b) -> b.getValue().compareTo(a.getValue()))
                   .limit(3)
                   .map(e -> "1. " + e.getKey() + " (" + e.getValue() + " sold)")
                   .collect(Collectors.joining("\n"));
    }

    private String getCategorySummary() {
        // Calculate revenue per category
        Map<String, Double> catRev = new HashMap<>();
        List<Sale> sales = saleRepo.findSalesWithItems(LocalDate.now().withDayOfMonth(1).atStartOfDay(), LocalDateTime.now());
        
        for (Sale s : sales) {
            for (SaleItem i : s.getItems()) {
                Product p = productRepo.findById(i.getProductId()).orElse(null);
                if (p != null) {
                    String c = p.getCategory() != null ? p.getCategory() : "Uncategorized";
                    catRev.put(c, catRev.getOrDefault(c, 0.0) + (i.getPrice() * i.getQuantity()));
                }
            }
        }
        return "📂 **Top Categories (This Month):**\n" +
               catRev.entrySet().stream()
                   .sorted((a,b) -> b.getValue().compareTo(a.getValue()))
                   .limit(3)
                   .map(e -> "• " + e.getKey() + ": ₹" + e.getValue())
                   .collect(Collectors.joining("\n"));
    }

    private String getAIForecast() {
        // Reuse the logic from your DashboardController
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Sale> recentSales = saleRepo.findByDateAfter(sevenDaysAgo);
        Map<LocalDate, Integer> dailyTotals = recentSales.stream()
            .collect(Collectors.groupingBy(s -> s.getDate().toLocalDate(), 
                Collectors.summingInt(s -> s.getItems().stream().mapToInt(SaleItem::getQuantity).sum())));
        
        int prediction = predictionService.predictNextDaySales(dailyTotals);
        return "🤖 **AI Prediction:**\nBased on recent trends, I expect we will sell approx. **" + prediction + " units** tomorrow.";
    }

    private String getRecentTransactions() {
        List<Sale> recent = saleRepo.findAll().stream()
            .sorted(Comparator.comparing(Sale::getDate).reversed())
            .limit(3)
            .collect(Collectors.toList());
            
        return "🧾 **Last 3 Transactions:**\n" +
               recent.stream()
                   .map(s -> "#" + s.getId() + " by " + s.getCashierName() + ": ₹" + s.getTotalAmount())
                   .collect(Collectors.joining("\n"));
    }

    private String getTopStaff() {
        Map<String, Long> stats = saleRepo.findAll().stream()
            .collect(Collectors.groupingBy(s -> s.getCashierName() != null ? s.getCashierName() : "Unknown", Collectors.counting()));
        
        return "🏆 **Star Performer:**\n" +
               stats.entrySet().stream().max(Map.Entry.comparingByValue())
                   .map(e -> e.getKey() + " with " + e.getValue() + " completed sales!")
                   .orElse("No data.");
    }

    private String getVIPCustomers() {
        List<Customer> vips = customerRepo.findTop20ByOrderByPointsDesc();
        if (vips.isEmpty()) return "No VIP customers yet.";
        Customer top = vips.get(0);
        return "💎 **Top VIP Customer:**\n" + top.getName() + " (" + top.getPoints() + " pts)\nPhone: " + top.getPhone();
    }
    
    private String getStockStatus() {
        long low = productRepo.findAll().stream().filter(p -> p.getStock() <= 5).count();
        return low == 0 ? "✅ All Stock Levels Healthy." : "⚠️ **Alert:** " + low + " products are running low on stock!";
    }
}