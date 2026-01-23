package com.example.demo.service;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PredictionService {

    /**
     * 🧠 INTERNAL AI BRAIN (Linear Regression)
     * No Python required. This calculates the trend line (y = mx + c) directly in Java.
     */
    public int predictNextDaySales(Map<LocalDate, Integer> dailySales) {
        // 1. Need at least 2 days of data to make a line
        if (dailySales.size() < 2) {
            return 0; 
        }

        // 2. Sort dates (Time must move forward)
        List<LocalDate> sortedDates = dailySales.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
        
        int n = sortedDates.size();
        double sumX = 0;   // Sum of Days (0, 1, 2...)
        double sumY = 0;   // Sum of Sales
        double sumXY = 0;  // Sum of (Day * Sales)
        double sumX2 = 0;  // Sum of (Day^2)

        for (int i = 0; i < n; i++) {
            double x = i; // Day 0, Day 1, Day 2...
            double y = dailySales.get(sortedDates.get(i));

            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        // 3. Calculate Slope (m) and Intercept (c)
        // Formula: m = (n*Σxy - Σx*Σy) / (n*Σx² - (Σx)²)
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // 4. Predict Tomorrow (x = n)
        // Formula: y = mx + c
        double nextDayIndex = n;
        double predictedValue = slope * nextDayIndex + intercept;

        // Return result (ensure it's not negative)
        return Math.max(0, (int) Math.ceil(predictedValue));
    }
}