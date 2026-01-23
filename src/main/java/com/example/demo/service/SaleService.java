package com.example.demo.service;

import com.example.demo.model.Customer;
import com.example.demo.model.Sale;
import com.example.demo.model.SaleItem;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
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

    @Transactional // Ensures if one part fails (e.g. Stock update), the whole sale cancels
    public Sale processSale(Map<String, Object> saleData) {
        Sale sale = new Sale();
        sale.setPaymentMethod((String) saleData.get("paymentMethod"));
        sale.setStatus((String) saleData.get("status"));
        sale.setCashierName((String) saleData.get("cashierName"));
        sale.setDate(LocalDateTime.now());
        
        String phone = (String) saleData.get("customerPhone");
        sale.setCustomerPhone(phone);

        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) saleData.get("items");
        BigDecimal total = BigDecimal.ZERO;
        List<SaleItem> saleItems = new ArrayList<>();

        for (Map<String, Object> itemData : itemsData) {
            SaleItem item = new SaleItem();
            item.setProductName((String) itemData.get("name"));
            
            // Handle number conversion safely
            double priceVal = ((Number) itemData.get("price")).doubleValue();
            item.setPrice(BigDecimal.valueOf(priceVal));

            int qty = ((Number) itemData.get("quantity")).intValue();
            item.setQuantity(qty);
            
            item.setSale(sale);
            saleItems.add(item);

            // 🧠 STOCK REDUCTION LOGIC
            int prodId = ((Number) itemData.get("productId")).intValue();
            productRepo.findById((long) prodId).ifPresent(p -> {
                if(p.getStock() >= qty) {
                    p.setStock(p.getStock() - qty);
                    productRepo.save(p);
                } else {
                    throw new RuntimeException("Insufficient Stock for: " + p.getName());
                }
            });
            
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(qty)));
        }
        
        sale.setItems(saleItems);
        sale.setTotalAmount(total);

        // 🧠 LOYALTY POINTS LOGIC
        if (phone != null && !phone.isEmpty()) {
            int earnedPoints = total.intValue() / 10;
            Customer cust = customerRepo.findByPhone(phone).orElse(new Customer());
            
            if(cust.getPhone() == null) {
                cust.setPhone(phone);
                cust.setName("Loyal Customer");
                cust.setPoints(0);
            }
            cust.setPoints(cust.getPoints() + earnedPoints);
            customerRepo.save(cust);
        }

        return saleRepo.save(sale);
    }
}