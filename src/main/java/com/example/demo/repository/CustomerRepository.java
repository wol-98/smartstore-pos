package com.example.demo.repository;

import com.example.demo.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByPhone(String phone);
    
    // 🧠 MAGIC QUERY: Finds top 5 customers with most points
    List<Customer> findTop5ByOrderByPointsDesc();
}