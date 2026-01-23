package com.example.demo.service;

import com.example.demo.model.Customer;
import com.example.demo.model.Product;
import com.example.demo.model.Sale;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SaleServiceTest {

    @Mock private SaleRepository saleRepo;       
    @Mock private ProductRepository productRepo; 
    @Mock private CustomerRepository customerRepo; 

    @InjectMocks
    private SaleService saleService; 

    @Test
    public void testProcessSale_CalculatesTotalCorrectly() {
        // 1. SETUP: Prepare fake data
        Map<String, Object> request = new HashMap<>();
        request.put("paymentMethod", "Cash");
        request.put("status", "Paid");
        request.put("cashierName", "TestUser");
        request.put("customerPhone", "9998887777");

        // Create 2 items
        List<Map<String, Object>> items = new ArrayList<>();
        
        Map<String, Object> item1 = new HashMap<>();
        item1.put("productId", 101);
        item1.put("quantity", 1);
        items.add(item1);

        Map<String, Object> item2 = new HashMap<>();
        item2.put("productId", 102); // Different ID, but we mock finding the same product object for simplicity
        item2.put("quantity", 1);
        items.add(item2);

        request.put("items", items);

        // --- MOCKING THE DATABASE ---

        // A. Mock Product (The Inventory)
        Product fakeProduct = new Product();
        fakeProduct.setId(101L);
        fakeProduct.setStock(10);
        fakeProduct.setName("Test Product");
        fakeProduct.setPrice(50.00); // 👈 IMPORTANT: Set price so calculation works!
        
        // When service asks for a product, give them this fake one
        when(productRepo.findById(anyLong())).thenReturn(Optional.of(fakeProduct));
        
        // B. Mock Sale (The Transaction)
        // When service saves a sale, just return it back
        when(saleRepo.save(any(Sale.class))).thenAnswer(i -> i.getArguments()[0]);

        // C. Mock Customer (The CRM)
        // 🚀 FIX: Return 'null' instead of Optional.empty()
        when(customerRepo.findByPhone(anyString())).thenReturn(null);

        // 2. EXECUTE
        Sale result = saleService.processSale(request);

        // 3. VERIFY
        // 50.00 + 50.00 = 100.0
        assertEquals(new BigDecimal("100.0"), result.getTotalAmount());
        
        verify(productRepo, times(2)).save(any(Product.class)); // Stock updated twice
        verify(customerRepo, times(2)).save(any(Customer.class)); // Customer created/updated
        
        System.out.println("✅ TEST PASSED: Revenue Calculation Verified!");
    }
}