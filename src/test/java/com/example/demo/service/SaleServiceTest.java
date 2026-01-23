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

@ExtendWith(MockitoExtension.class) // Enables Mockito
public class SaleServiceTest {

    @Mock private SaleRepository saleRepo;       // Fake Database
    @Mock private ProductRepository productRepo; // Fake Inventory
    @Mock private CustomerRepository customerRepo; // Fake CRM

    @InjectMocks
    private SaleService saleService; // The real service we are testing

    @Test
    public void testProcessSale_CalculatesTotalCorrectly() {
        // 1. SETUP: Prepare fake data
        Map<String, Object> request = new HashMap<>();
        request.put("paymentMethod", "Cash");
        request.put("status", "Paid");
        request.put("cashierName", "TestUser");
        request.put("customerPhone", "9998887777");

        // Create 2 items: 50.00 each, Quantity 1
        List<Map<String, Object>> items = new ArrayList<>();
        
        Map<String, Object> item1 = new HashMap<>();
        item1.put("name", "Test Product");
        item1.put("price", 50.00);
        item1.put("quantity", 1);
        item1.put("productId", 101);
        items.add(item1);

        Map<String, Object> item2 = new HashMap<>();
        item2.put("name", "Test Product 2");
        item2.put("price", 50.00);
        item2.put("quantity", 1);
        item2.put("productId", 102);
        items.add(item2);

        request.put("items", items);

        // Mock the Product Repository to return a fake product with stock
        Product fakeProduct = new Product();
        fakeProduct.setId(101L);
        fakeProduct.setStock(10);
        fakeProduct.setName("Test Product");
        when(productRepo.findById(anyLong())).thenReturn(Optional.of(fakeProduct));
        
        // Mock Sale Repository to just return the sale object back
        when(saleRepo.save(any(Sale.class))).thenAnswer(i -> i.getArguments()[0]);

        // Mock Customer Repository
        when(customerRepo.findByPhone(anyString())).thenReturn(Optional.empty());

        // 2. EXECUTE: Call the method
        Sale result = saleService.processSale(request);

        // 3. VERIFY: Did it work?
        // Expectation: 50 + 50 = 100
        assertEquals(new BigDecimal("100.0"), result.getTotalAmount());
        
        // Expectation: Stock should have reduced (Repo save called twice, once per item)
        verify(productRepo, times(2)).save(any(Product.class));
        
        // Expectation: Loyalty Points (100 / 10 = 10 points)
        verify(customerRepo, times(1)).save(any(Customer.class));
        
        System.out.println("✅ TEST PASSED: Revenue Calculation Verified!");
    }
}