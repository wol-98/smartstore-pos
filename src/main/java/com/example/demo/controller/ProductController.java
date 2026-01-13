package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepo;

    @GetMapping
    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productRepo.save(product);
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