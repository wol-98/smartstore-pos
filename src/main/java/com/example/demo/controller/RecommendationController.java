package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @GetMapping("/{productId}")
    public List<Product> getRecommendations(@PathVariable Long productId) {
        return recommendationService.getRecommendations(productId);
    }
}