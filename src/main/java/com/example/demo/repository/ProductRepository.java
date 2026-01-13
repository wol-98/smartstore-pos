package com.example.demo.repository;

import com.example.demo.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// CHANGE <Product, Integer> TO <Product, Long>
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}