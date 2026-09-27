package com.pharmaguard.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MedicineRiskEngine {

    public Map<String, Object> calculateRisk(
            Map<String, Object> ocrResult,
            Map<String, Object> medIdResult,
            Map<String, Object> batchResult,
            Map<String, Object> mfrResult,
            Map<String, Object> scanAnalysis,
            LocalDate expiryDate,
            String barcode) {

        int score = 0;
        List<String> positiveFactors = new ArrayList<>();
        List<String> warningFactors = new ArrayList<>();

        // 1. OCR Confidence Signal (0 to 10 risk penalty)
        double ocrConfidence = (double) ocrResult.getOrDefault("confidence", 0.5);
        if (ocrConfidence >= 0.8) {
            positiveFactors.add("High OCR text clarity & extraction confidence");
        } else if (ocrConfidence < 0.5) {
            score += 15;
            warningFactors.add("Low OCR text extraction clarity — packaging image slightly unclear");
        }

        // 2. Barcode Verification Signal (0 to 15 risk penalty)
        if (barcode != null && !barcode.isBlank() && barcode.length() >= 8) {
            positiveFactors.add("Barcode / QR code recognized and validated");
        } else {
            score += 10;
            warningFactors.add("Barcode could not be read or was unlisted");
        }

        // 3. Database & Batch Match Signal (0 to 25 risk penalty)
        boolean idMatched = (boolean) medIdResult.getOrDefault("matched", false);
        String batchStatus = (String) batchResult.getOrDefault("status", "UNABLE_TO_VERIFY");

        if (idMatched && "MATCHED".equals(batchStatus)) {
            positiveFactors.add("Batch record verified in central database");
            positiveFactors.add("Medicine identity and batch number confirmed");
        } else if (idMatched) {
            positiveFactors.add("Medicine name matched in pharmaceutical registry");
            if ("MISMATCH".equals(batchStatus)) {
                score += 30;
                warningFactors.add("Batch information found on packaging partially mismatches official database record");
            } else {
                score += 15;
                warningFactors.add("Batch number unlisted in active central batch registry (Unable to verify)");
            }
        } else {
            score += 35;
            warningFactors.add("Medicine information could not be matched against active database");
        }

        // 4. Manufacturer Verification Signal (0 to 15 risk penalty)
        boolean mfrMatched = (boolean) mfrResult.getOrDefault("matched", false);
        if (mfrMatched) {
            positiveFactors.add("Manufacturer entity verified in registered network");
        } else {
            score += 10;
            warningFactors.add("Manufacturer information could not be independently verified");
        }

        // 5. Expiry Verification Signal (0 to 40 risk penalty)
        String expiryStatus = "VALID";
        if (expiryDate != null) {
            LocalDate now = LocalDate.now();
            if (expiryDate.isBefore(now)) {
                expiryStatus = "EXPIRED";
                score += 45; // Critical penalty
                warningFactors.add("EXPIRATION CRITICAL: Stated expiry date (" + expiryDate + ") has passed!");
            } else {
                long daysUntilExp = ChronoUnit.DAYS.between(now, expiryDate);
                if (daysUntilExp <= 30) {
                    expiryStatus = "EXPIRING_SOON";
                    score += 20;
                    warningFactors.add("EXPIRING SOON: Medicine expires in " + daysUntilExp + " days (" + expiryDate + ")");
                } else {
                    positiveFactors.add("Expiry date valid (" + daysUntilExp + " days remaining)");
                }
            }
        } else {
            score += 10;
            warningFactors.add("Expiry date missing or could not be parsed");
        }

        // 6. Duplicate Scan Pattern Signal (0 to 25 risk penalty)
        boolean suspiciousScan = (boolean) scanAnalysis.getOrDefault("suspiciousPattern", false);
        if (suspiciousScan) {
            score += 25;
            warningFactors.add("SUSPICIOUS SCAN PATTERN: Product identifier has unusually repeated scans across multiple users/sessions");
        } else {
            positiveFactors.add("Normal scan frequency pattern recorded");
        }

        // Bound score to 0 - 100
        score = Math.max(0, Math.min(100, score));

        // Determine Risk Level & Verification Status
        String riskLevel;
        String statusText;

        if (score <= 24) {
            riskLevel = "LOW";
            statusText = "Verified";
        } else if (score <= 49) {
            riskLevel = "MODERATE";
            statusText = "Partially Verified";
        } else if (score <= 74) {
            riskLevel = "HIGH";
            statusText = "Suspicious / High Risk";
        } else {
            riskLevel = "CRITICAL";
            statusText = "Critical Risk";
        }

        Map<String, Object> riskResult = new HashMap<>();
        riskResult.put("riskScore", score);
        riskResult.put("riskLevel", riskLevel);
        riskResult.put("statusText", statusText);
        riskResult.put("expiryStatus", expiryStatus);
        riskResult.put("positiveFactors", positiveFactors);
        riskResult.put("warningFactors", warningFactors);

        return riskResult;
    }
}
