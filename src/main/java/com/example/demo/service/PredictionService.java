package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PredictionService {

    // 🚀 CRITICAL FIX: Read the URL from Railway Environment Variables
    @Value("${AI_SERVICE_URL}")
    private String aiServiceUrl;

    public int getPredictedSales(List<Map<String, Object>> aiData) {
        if (aiData == null || aiData.isEmpty()) {
            return 0;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("history", aiData);

            // Use the dynamic URL (aiServiceUrl) instead of localhost
            Map<String, Object> response = restTemplate.postForObject(aiServiceUrl, requestPayload, Map.class);
            
            if (response != null && response.containsKey("prediction")) {
                Object pred = response.get("prediction");
                // Handle case where JSON returns Double (e.g. 5.0) instead of Integer
                if (pred instanceof Number) {
                    return ((Number) pred).intValue();
                }
            }
            return 0;
        } catch (Exception e) {
            // This is normal if the Python service is sleeping or starting up
            System.err.println("⚠️ AI Brain Offline: " + e.getMessage());
            return 0; 
        }
    }
}