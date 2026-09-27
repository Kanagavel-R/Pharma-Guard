package com.pharmaguard.service;

import com.pharmaguard.entity.Medicine;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExpiryPredictionService {

    public Map<String, Object> predictMedicineExpiry(Medicine medicine) {
        Map<String, Object> result = new HashMap<>();
        if (medicine == null || medicine.getExpiryDate() == null) {
            result.put("daysRemaining", 0);
            result.put("riskTier", "UNKNOWN");
            result.put("recommendation", "No expiry date recorded.");
            return result;
        }

        LocalDate today = LocalDate.now();
        LocalDate expiry = medicine.getExpiryDate();
        long daysRemaining = ChronoUnit.DAYS.between(today, expiry);

        String riskTier;
        String recommendation;
        if (daysRemaining < 0) {
            riskTier = "EXPIRED";
            recommendation = "DISPOSE IMMEDIATELY — Batch passed expiry on " + expiry;
        } else if (daysRemaining <= 7) {
            riskTier = "CRITICAL";
            recommendation = "HIGH EXPIRY RISK — Quarantine batch or apply priority FEFO dispensing.";
        } else if (daysRemaining <= 30) {
            riskTier = "MEDIUM";
            recommendation = "MEDIUM EXPIRY RISK — Monitor sales velocity; discount or request supplier return if unsold.";
        } else if (daysRemaining <= 90) {
            riskTier = "LOW_WARNING";
            recommendation = "LOW EXPIRY RISK — Normal inventory tracking.";
        } else {
            riskTier = "SAFE";
            recommendation = "SAFE — Stock within optimal shelf life window.";
        }

        result.put("daysRemaining", daysRemaining);
        result.put("riskTier", riskTier);
        result.put("expiryDate", expiry.toString());
        result.put("recommendation", recommendation);
        result.put("disclaimer", "Predictive analytics are based on stored batch dates and historical inventory logs.");

        return result;
    }
}
