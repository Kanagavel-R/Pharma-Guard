package com.pharmaguard.service;

import com.pharmaguard.entity.Medicine;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MedicineRiskService {

    private final RecallService recallService;

    public MedicineRiskService(RecallService recallService) {
        this.recallService = recallService;
    }

    public Map<String, Object> calculateMedicineRisk(Medicine medicine) {
        Map<String, Object> result = new HashMap<>();
        if (medicine == null) {
            result.put("score", 0);
            result.put("tier", "LOW");
            result.put("label", "🟢 LOW RISK");
            result.put("factors", new ArrayList<String>());
            return result;
        }

        int score = 0;
        List<String> factors = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Factor 1: Expiry Proximity
        if (medicine.getExpiryDate() != null) {
            if (medicine.getExpiryDate().isBefore(today)) {
                score += 45;
                factors.add("Medicine batch is EXPIRED since " + medicine.getExpiryDate());
            } else {
                long daysLeft = ChronoUnit.DAYS.between(today, medicine.getExpiryDate());
                if (daysLeft <= 7) {
                    score += 30;
                    factors.add("Critical expiry window: " + daysLeft + " days remaining");
                } else if (daysLeft <= 30) {
                    score += 15;
                    factors.add("Expiring within 30 days (" + daysLeft + " days remaining)");
                } else if (daysLeft <= 90) {
                    score += 5;
                    factors.add("Expiring within 90 days");
                }
            }
        }

        // Factor 2: Recall Status
        if (medicine.getBatchNumber() != null && recallService.isBatchRecalled(medicine.getBatchNumber())) {
            score += 50;
            factors.add("Active manufacturer/health authority RECALL for batch " + medicine.getBatchNumber());
        }

        // Factor 3: Stock Anomalies
        if (medicine.getQuantity() == 0) {
            score += 15;
            factors.add("Stock Out: Quantity is 0 units");
        } else if (medicine.getQuantity() <= medicine.getReorderLevel()) {
            score += 10;
            factors.add("Low Stock: Quantity (" + medicine.getQuantity() + ") at or below reorder level (" + medicine.getReorderLevel() + ")");
        }

        score = Math.min(100, Math.max(0, score));

        String tier;
        String label;
        if (score >= 70) {
            tier = "HIGH";
            label = "🔴 HIGH RISK / NEEDS VERIFICATION";
        } else if (score >= 30) {
            tier = "MEDIUM";
            label = "🟠 MEDIUM RISK";
        } else {
            tier = "LOW";
            label = "🟢 LOW RISK";
        }

        result.put("score", score);
        result.put("tier", tier);
        result.put("label", label);
        result.put("factors", factors);

        return result;
    }
}
