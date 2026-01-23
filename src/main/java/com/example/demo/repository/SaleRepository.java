package com.example.demo.repository;

import com.example.demo.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    
    // 🧠 SMART QUERY: Finds sales only after a specific date
    // Spring Data JPA automatically writes the SQL for this!
    List<Sale> findByDateAfter(LocalDateTime date);
}