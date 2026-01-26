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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    @Autowired private SaleRepository saleRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private CustomerRepository customerRepo;
    @Autowired private PredictionService predictionService;

    // 📘 KNOWLEDGE BASE
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

        // 1. 🏷️ PRODUCT DETAILS
        if (query.contains("price") || query.contains("how much") || query.contains("category of") || query.contains("what is")) {
            if(query.length() > 10) return getProductDetails(query);
        }

        // 2. 🔢 INVENTORY
        if (query.contains("how many products") || query.contains("total items")) {
            long count = productRepo.count();
            return "📦 We currently have " + count + " unique products in the inventory.";
        }

        // 3. 💰 PROFIT & REVENUE
        if (query.contains("profit") || query.contains("margin")) return getProfitSummary();
        if (query.contains("revenue") || query.contains("sales")) return getRevenueSummary(query);

        // 4. 🔥 BEST SELLERS
        if (query.contains("best seller") || query.contains("top product")) return getBestSellers();
        
        // 5. 📂 CATEGORIES
        if (query.contains("category") || query.contains("categories")) return getCategorySummary();

        // 6. 📉 STOCK ALERTS
        if (query.contains("stock") || query.contains("low")) return getStockStatus();

        // 7. 🤖 AI FORECAST
        if (query.contains("forecast") || query.contains("predict")) return getAIForecast();

        // 8. 🧾 LIVE TRANSACTIONS
        if (query.contains("transaction") || query.contains("recent") || query.contains("live")) return getRecentTransactions();

        // 9. 👥 STAFF & VIPs
        if (query.contains("staff") || query.contains("cashier")) return getTopStaff();
        if (query.contains("vip") || query.contains("customer")) return getVIPCustomers();

        // 10. 📘 FALLBACK
        for (String key : knowledgeBase.keySet()) {
            if (query.contains(key)) return "💡 " + knowledgeBase.get(key);
        }

        return "🤖 I can help! Try asking: 'Price of iPhone', 'Recent transactions', 'Net Profit', or 'Low stock'.";
    }

    // ==========================================================
    // 🧠 ANALYTICAL LOGIC
    // ==========================================================

    private String getProductDetails(String query) {
        String search = query.replace("price of", "").replace("how much is", "")
                             .replace("category of", "").replace("what is", "")
                             .replace("the", "").trim();
        
        List<Product> products = productRepo.findAll();
        Optional<Product> match = products.stream()
            .filter(p -> p.getName().toLowerCase().contains(search))
            .findFirst();

        if (match.isPresent()) {
            Product p = match.get();
            double price = p.getPrice();
            double cost = (p.getCostPrice() != null ? p.getCostPrice() : 0.0);
            double profit = price - cost;
            
            return "🏷️ **" + p.getName() + "**\n" +
                   "• Category: " + (p.getCategory() != null ? p.getCategory() : "N/A") + "\n" +
                   "• Stock: " + p.getStock() + " units\n" +
                   "• Selling Price: ₹" + price + "\n" +
                   "• Buying Price: ₹" + cost + "\n" +
                   "• Margin: ₹" + profit;
        }
        return "❌ I couldn't find a product matching '" + search + "'.";
    }

    private String getRecentTransactions() {
        List<Sale> recent = saleRepo.findAll().stream()
            .sorted(Comparator.comparing(Sale::getDate).reversed())
            .limit(3)
            .collect(Collectors.toList());
        
        if(recent.isEmpty()) return "No transactions yet today.";

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");
        
        return "🧾 **Live Transactions:**\n" +
               recent.stream()
                   .map(s -> "• " + s.getDate().format(timeFmt) + " | " + s.getCashierName() + "\n" + 
                             "   " + (s.getItems() != null ? s.getItems().size() : 0) + " items for ₹" + s.getTotalAmount())
                   .collect(Collectors.joining("\n"));
    }

    private String getCategorySummary() {
        Map<String, Double> catRev = new HashMap<>();
        List<Sale> sales = saleRepo.findSalesWithItems(LocalDate.now().withDayOfMonth(1).atStartOfDay(), LocalDateTime.now());
        
        for (Sale s : sales) {
            if(s.getItems() == null) continue;
            for (SaleItem i : s.getItems()) {
                Product p = productRepo.findById(i.getProductId()).orElse(null);
                if (p != null) {
                    String c = p.getCategory() != null ? p.getCategory() : "Uncategorized";
                    
                    // 🚀 FIXED: Handle BigDecimal Multiplication safely
                    BigDecimal itemPrice = i.getPrice(); // This is BigDecimal
                    double lineTotal = itemPrice.multiply(BigDecimal.valueOf(i.getQuantity())).doubleValue();
                    
                    catRev.put(c, catRev.getOrDefault(c, 0.0) + lineTotal);
                }
            }
        }
        return "📂 **Top Categories (Revenue):**\n" +
               catRev.entrySet().stream()
                   .sorted((a,b) -> b.getValue().compareTo(a.getValue()))
                   .limit(4)
                   .map(e -> "• " + e.getKey() + ": ₹" + String.format("%.0f", e.getValue()))
                   .collect(Collectors.joining("\n"));
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
        return "💰 **Net Profit Today**\nRevenue: ₹" + String.format("%.2f", revenue) + "\nCost: ₹" + String.format("%.2f", cost) + "\n----------------\nProfit: ₹" + String.format("%.2f", (revenue - cost));
    }

    private String getRevenueSummary(String query) {
        boolean isMonth = query.contains("month");
        LocalDateTime start = isMonth ? LocalDate.now().withDayOfMonth(1).atStartOfDay() : LocalDate.now().atStartOfDay();
        List<Sale> sales = saleRepo.findByDateAfter(start);
        double total = sales.stream().mapToDouble(s -> s.getTotalAmount().doubleValue()).sum();
        return "📊 **Total Revenue (" + (isMonth ? "This Month" : "Today") + ")**\n₹" + total + " across " + sales.size() + " orders.";
    }

    private String getBestSellers() {
        Map<String, Integer> counts = saleRepo.findAll().stream()
            .flatMap(s -> s.getItems().stream())
            .collect(Collectors.groupingBy(SaleItem::getProductName, Collectors.summingInt(SaleItem::getQuantity)));
        
        return "🔥 **Top 3 Best Sellers:**\n" +
               counts.entrySet().stream()
                   .sorted((a,b) -> b.getValue().compareTo(a.getValue()))
                   .limit(3)
                   .map(e -> "1. " + e.getKey() + " (" + e.getValue() + " units)")
                   .collect(Collectors.joining("\n"));
    }

    private String getAIForecast() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Sale> recentSales = saleRepo.findByDateAfter(sevenDaysAgo);
        Map<LocalDate, Integer> dailyTotals = recentSales.stream()
            .collect(Collectors.groupingBy(s -> s.getDate().toLocalDate(), 
                Collectors.summingInt(s -> s.getItems() != null ? s.getItems().stream().mapToInt(SaleItem::getQuantity).sum() : 0)));
        
        int prediction = predictionService.predictNextDaySales(dailyTotals);
        return "🤖 **AI Prediction:**\nBased on recent trends, I expect ~" + prediction + " units will be sold tomorrow.";
    }

    private String getTopStaff() {
        Map<String, Long> stats = saleRepo.findAll().stream()
            .collect(Collectors.groupingBy(s -> s.getCashierName() != null ? s.getCashierName() : "Unknown", Collectors.counting()));
        
        return "🏆 **Star Performer:**\n" +
               stats.entrySet().stream().max(Map.Entry.comparingByValue())
                   .map(e -> e.getKey() + " (" + e.getValue() + " sales)")
                   .orElse("No data.");
    }

    private String getVIPCustomers() {
        List<Customer> vips = customerRepo.findTop20ByOrderByPointsDesc();
        if (vips.isEmpty()) return "No VIP customers yet.";
        Customer top = vips.get(0);
        return "💎 **Top VIP:** " + top.getName() + "\nPoints: " + top.getPoints() + "\nPhone: " + top.getPhone();
    }
    
    private String getStockStatus() {
        long low = productRepo.findAll().stream().filter(p -> p.getStock() <= 5).count();
        return low == 0 ? "✅ All Stock Levels Healthy." : "⚠️ **Alert:** " + low + " products are running low on stock!";
    }
}