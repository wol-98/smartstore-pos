package com.example.demo.repository;

import com.example.demo.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // Finds the customers with the highest points
    List<Customer> findTop20ByOrderByPointsDesc();
    
    // Needed for Checkout logic
    Customer findByPhone(String phone);
}