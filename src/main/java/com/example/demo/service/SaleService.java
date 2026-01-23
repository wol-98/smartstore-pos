package com.example.demo.service;

import com.example.demo.model.Customer;
import com.example.demo.model.Product;
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

    @Autowired
    private SaleRepository saleRepo;

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private CustomerRepository customerRepo;

    @Transactional
    public Sale processSale(Map<String, Object> saleData) {
        // 1. Extract Data from JSON
        String cashierName = (String) saleData.get("cashierName");
        String customerPhone = (String) saleData.get("customerPhone");
        String paymentMethod = (String) saleData.get("paymentMethod");
        String status = (String) saleData.get("status");
        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) saleData.get("items");

        // 2. Handle Customer (The Fix 🛠️)
        // We check if phone exists. If yes, look up. If null/empty, it's a guest.
        if (customerPhone != null && !customerPhone.isEmpty()) {
            Customer customer = customerRepo.findByPhone(customerPhone);
            
            if (customer == null) {
                // Create New Customer if not found
                customer = new Customer();
                customer.setName("New Customer"); // You can add a name field to the form later
                customer.setPhone(customerPhone);
                customer.setPoints(0);
                customerRepo.save(customer);
            }
            
            // Add Loyalty Points (e.g., 10 points per sale)
            customer.setPoints(customer.getPoints() + 10);
            customerRepo.save(customer);
        }

        // 3. Create Sale Object
        Sale sale = new Sale();
        sale.setCashierName(cashierName);
        sale.setCustomerPhone(customerPhone);
        sale.setPaymentMethod(paymentMethod);
        sale.setStatus(status);
        sale.setDate(LocalDateTime.now());

        List<SaleItem> saleItems = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        // 4. Process Items & Deduct Stock
        for (Map<String, Object> itemData : itemsData) {
            Long prodId = Long.valueOf(itemData.get("productId").toString());
            int quantity = Integer.parseInt(itemData.get("quantity").toString());

            Product product = productRepo.findById(prodId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + prodId));

            if (product.getStock() < quantity) {
                throw new RuntimeException("Out of Stock: " + product.getName());
            }

            // Deduct Stock
            product.setStock(product.getStock() - quantity);
            productRepo.save(product);

            // Create Sale Item
            SaleItem saleItem = new SaleItem();
            saleItem.setProductId(product.getId());
            saleItem.setProductName(product.getName());
            saleItem.setQuantity(quantity);
            saleItem.setPrice(BigDecimal.valueOf(product.getPrice()));
            saleItem.setSale(sale); // Link back to parent
            
            saleItems.add(saleItem);

            // Add to Total
            BigDecimal itemTotal = BigDecimal.valueOf(product.getPrice()).multiply(BigDecimal.valueOf(quantity));
            grandTotal = grandTotal.add(itemTotal);
        }

        sale.setItems(saleItems);
        sale.setTotalAmount(grandTotal);

        return saleRepo.save(sale);
    }
}