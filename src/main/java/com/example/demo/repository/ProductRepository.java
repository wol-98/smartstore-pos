package com.example.demo.repository;

import com.example.demo.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

// CHANGE <Product, Integer> TO <Product, Long>
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
	
	// 🧠 SMART QUERY: 
    // Counts products where Stock is LOWER than that product's specific Safety Limit
    @Query("SELECT COUNT(p) FROM Product p WHERE p.stock <= p.minStock")
    long countLowStock();
}