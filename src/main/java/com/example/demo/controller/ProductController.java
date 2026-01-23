package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.ExcelService; // 👈 NEW IMPORT
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity; // 👈 NEW IMPORT
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // 👈 NEW IMPORT

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepo;
    
    @Autowired 
    private ExcelService excelService; // 👈 Inject the Service

    @GetMapping
    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productRepo.save(product);
    }

    // --- 🚀 NEW: BULK UPLOAD ENDPOINT ---
    @PostMapping("/upload")
    public ResponseEntity<?> uploadProducts(@RequestParam("file") MultipartFile file) {
        try {
            // 1. Parse Excel to List<Product>
            List<Product> products = excelService.parseExcelFile(file);
            
            // 2. Save All to DB (Batch Save)
            productRepo.saveAll(products);
            
            return ResponseEntity.ok("Uploaded " + products.size() + " products successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Could not upload file: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setName(productDetails.getName());
        product.setBrand(productDetails.getBrand());
        product.setCategory(productDetails.getCategory());
        product.setPrice(productDetails.getPrice());
        product.setStock(productDetails.getStock());
        product.setDiscount(productDetails.getDiscount());

        return productRepo.save(product);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productRepo.deleteById(id);
    }
}