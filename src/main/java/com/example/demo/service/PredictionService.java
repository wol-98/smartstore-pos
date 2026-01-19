package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PredictionService {

    private final String PYTHON_AI_URL = "http://localhost:5000/predict";

    public int getPredictedSales(List<Map<String, Object>> aiData) {
        // 🧪 If you have no sales data yet, return 0 to avoid empty model errors
        if (aiData == null || aiData.isEmpty()) {
            return 0;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            
            // 🚨 ALIGNMENT FIX: Wrap the list in a Map to create the {"history": [...]} structure
            // This is required by your updated Python code: data = request.json['history']
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("history", aiData);

            // Call Python and expect a Map response containing the "prediction" key
            Map<String, Object> response = restTemplate.postForObject(PYTHON_AI_URL, requestPayload, Map.class);
            
            if (response != null && response.containsKey("prediction")) {
                // Returns the sum of the next 7 days calculated by Linear Regression
                return (Integer) response.get("prediction");
            }
            return 0;
        } catch (Exception e) {
            System.err.println("❌ AI Service Error: " + e.getMessage());
            return 0; // Fallback if Python is offline or data is insufficient
        }
    }
}