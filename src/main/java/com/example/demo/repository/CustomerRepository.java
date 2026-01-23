package com.example.demo.repository;

import com.example.demo.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // 🚀 Matches the Service call exactly
    List<Customer> findTop20ByOrderByPointsDesc();
    
    // Helper for checkout
    Customer findByPhone(String phone);
}