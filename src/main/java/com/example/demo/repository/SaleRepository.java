package com.example.demo.repository;

import com.example.demo.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    
    // 1. For PDF/Receipts (Prevents crash)
    @Query("SELECT s FROM Sale s LEFT JOIN FETCH s.items WHERE s.id = :id")
    Optional<Sale> findByIdWithItems(@Param("id") Long id);

    // 2. For AI Prediction
    List<Sale> findByDateAfter(LocalDateTime date);

    // 3. 🚀 THE FIX: This method allows the Dashboard to filter by date range.
    // Without this, DashboardController will show a Red Error.
    List<Sale> findByDateBetween(LocalDateTime start, LocalDateTime end);
}