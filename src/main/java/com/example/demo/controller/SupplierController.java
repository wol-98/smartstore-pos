package com.example.demo.controller;

import com.example.demo.model.Supplier;
import com.example.demo.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    @Autowired
    private SupplierRepository supplierRepo;

    @GetMapping
    public List<Supplier> getAllSuppliers() {
        return supplierRepo.findAll();
    }

    @PostMapping
    public Supplier saveSupplier(@RequestBody Supplier supplier) {
        return supplierRepo.save(supplier);
    }

    @DeleteMapping("/{id}")
    public void deleteSupplier(@PathVariable Long id) {
        supplierRepo.deleteById(id);
    }
}