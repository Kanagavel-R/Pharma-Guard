package com.pharmaguard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaguard.entity.Alert;
import com.pharmaguard.entity.VerificationHistory;
import com.pharmaguard.repository.AlertRepository;
import com.pharmaguard.repository.VerificationHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class VerificationService {

    private final OcrProcessingService ocrProcessingService;
    private final MedicineIdentificationService medicineIdentificationService;
    private final BatchVerificationService batchVerificationService;
    private final ManufacturerVerificationService manufacturerVerificationService;
    private final DuplicateScanService duplicateScanService;
    private final MedicineRiskEngine medicineRiskEngine;
    private final GeminiService geminiService;
    private final VerificationHistoryRepository verificationHistoryRepository;
    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;

    public VerificationService(
            OcrProcessingService ocrProcessingService,
            MedicineIdentificationService medicineIdentificationService,
            BatchVerificationService batchVerificationService,
            ManufacturerVerificationService manufacturerVerificationService,
            DuplicateScanService duplicateScanService,
            MedicineRiskEngine medicineRiskEngine,
            GeminiService geminiService,
            VerificationHistoryRepository verificationHistoryRepository,
            AlertRepository alertRepository) {
        this.ocrProcessingService = ocrProcessingService;
        this.medicineIdentificationService = medicineIdentificationService;
        this.batchVerificationService = batchVerificationService;
        this.manufacturerVerificationService = manufacturerVerificationService;
        this.duplicateScanService = duplicateScanService;
        this.medicineRiskEngine = medicineRiskEngine;
        this.geminiService = geminiService;
        this.verificationHistoryRepository = verificationHistoryRepository;
        this.alertRepository = alertRepository;
        this.objectMapper = new ObjectMapper();
    }

    public VerificationHistory processVerification(
            Long userId,
            String username,
            String rawOcrText,
            String barcodeInput,
            String manualMedicineName,
            String manualBatchNumber,
            String manualManufacturer,
            LocalDate manualExpiryDate,
            String deviceSessionId,
            String locationRegion) {

        // 1. OCR Processing & Extraction
        Map<String, Object> ocrResult = ocrProcessingService.processOcrText(rawOcrText);
        @SuppressWarnings("unchecked")
        Map<String, String> ocrExtracted = (Map<String, String>) ocrResult.getOrDefault("extracted", Map.of());

        String barcode = barcodeInput != null && !barcodeInput.isBlank() ? barcodeInput.trim() : ocrExtracted.get("barcode");
        String batchNumber = manualBatchNumber != null && !manualBatchNumber.isBlank() ? manualBatchNumber.trim() : ocrExtracted.get("batchNumber");
        String medicineName = manualMedicineName != null && !manualMedicineName.isBlank() ? manualMedicineName.trim() : ocrExtracted.get("medicineName");
        String manufacturer = manualManufacturer != null && !manualManufacturer.isBlank() ? manualManufacturer.trim() : ocrExtracted.get("manufacturer");

        LocalDate expiryDate = manualExpiryDate;
        if (expiryDate == null && ocrExtracted.containsKey("expiryDate")) {
            try {
                String expStr = ocrExtracted.get("expiryDate");
                if (expStr.contains("/")) {
                    String[] parts = expStr.split("/");
                    if (parts.length == 2) {
                        int month = Integer.parseInt(parts[0]);
                        int year = Integer.parseInt(parts[1]);
                        expiryDate = LocalDate.of(year, month, 28);
                    }
                }
            } catch (Exception ignored) {}
        }

        // 2. Medicine Identification
        Map<String, Object> medIdResult = medicineIdentificationService.identifyMedicine(barcode, batchNumber, medicineName, manufacturer);
        String identifiedName = (String) medIdResult.getOrDefault("medicineName", medicineName != null ? medicineName : "Unknown Medicine");
        String identifiedBatch = (String) medIdResult.getOrDefault("batchNumber", batchNumber != null ? batchNumber : "UNLISTED");
        String identifiedMfr = (String) medIdResult.getOrDefault("manufacturer", manufacturer != null ? manufacturer : "Unverified Manufacturer");

        // 3. Batch Verification
        Map<String, Object> batchResult = batchVerificationService.verifyBatch(identifiedBatch, identifiedName, identifiedMfr, expiryDate);

        // 4. Manufacturer Verification
        Map<String, Object> mfrResult = manufacturerVerificationService.verifyManufacturer(identifiedMfr, identifiedName);

        // 5. Duplicate Scan Analysis
        String barcodeHash = barcode != null && !barcode.isBlank() ? barcode : identifiedBatch;
        Map<String, Object> scanAnalysis = duplicateScanService.analyzeScanPattern(barcodeHash, identifiedBatch);

        // 6. Risk Engine Calculation
        Map<String, Object> riskResult = medicineRiskEngine.calculateRisk(ocrResult, medIdResult, batchResult, mfrResult, scanAnalysis, expiryDate, barcode);

        int riskScore = (int) riskResult.get("riskScore");
        String riskLevel = (String) riskResult.get("riskLevel");
        String statusText = (String) riskResult.get("statusText");

        @SuppressWarnings("unchecked")
        List<String> positiveFactors = (List<String>) riskResult.get("positiveFactors");
        @SuppressWarnings("unchecked")
        List<String> warningFactors = (List<String>) riskResult.get("warningFactors");

        // 7. AI Risk Explanation
        String aiExplanation = geminiService.explainVerificationRisk(identifiedName, identifiedBatch, identifiedMfr, riskScore, riskLevel, positiveFactors, warningFactors);

        // 8. Save Verification History Record
        VerificationHistory vh = new VerificationHistory();
        vh.setUserId(userId != null ? userId : 1L);
        vh.setUsername(username != null ? username : "SystemUser");
        vh.setMedicineName(identifiedName);
        vh.setBatchNumber(identifiedBatch);
        vh.setManufacturer(identifiedMfr);
        vh.setBarcodeHash(barcodeHash);
        vh.setGenericName(ocrExtracted.getOrDefault("genericName", ""));
        vh.setStrength(ocrExtracted.getOrDefault("strength", ""));
        vh.setDosageForm(ocrExtracted.getOrDefault("dosageForm", ""));
        vh.setRawOcrText(rawOcrText);

        try {
            vh.setExtractedInfoJson(objectMapper.writeValueAsString(ocrExtracted));
            vh.setVerificationSignalsJson(objectMapper.writeValueAsString(Map.of(
                    "batchResult", batchResult,
                    "mfrResult", mfrResult,
                    "scanAnalysis", scanAnalysis,
                    "medIdResult", medIdResult
            )));
            vh.setDetectedIssuesJson(objectMapper.writeValueAsString(warningFactors));
        } catch (Exception e) {
            vh.setExtractedInfoJson("{}");
            vh.setVerificationSignalsJson("{}");
            vh.setDetectedIssuesJson("[]");
        }

        vh.setRiskScore(riskScore);
        vh.setRiskLevel(riskLevel);
        vh.setVerificationStatus(statusText);
        vh.setAiExplanation(aiExplanation);
        vh.setCreatedAt(LocalDateTime.now());

        VerificationHistory savedVh = verificationHistoryRepository.save(vh);

        // 9. Log Scan Event for telemetry
        duplicateScanService.logScanEvent(savedVh.getId(), userId, barcodeHash, identifiedBatch, deviceSessionId, locationRegion, statusText);

        // 10. Create System Alert if Risk is Elevated
        if (riskScore >= 25) {
            Alert alert = new Alert();
            alert.setVerificationId(savedVh.getId());
            alert.setMedicineName(identifiedName);
            alert.setAlertType("VERIFICATION_RISK");
            alert.setSeverity(riskLevel);
            String titleText = "⚠️ " + riskLevel + " Risk Detected: " + identifiedName + " (" + identifiedBatch + ")";
            if (titleText.length() > 240) titleText = titleText.substring(0, 235) + "...";
            alert.setTitle(titleText);

            String alertMessage = "Verification assigned risk score " + riskScore + "/100 (" + riskLevel + "). Issues: " + String.join("; ", warningFactors);
            if (alertMessage.length() > 240) alertMessage = alertMessage.substring(0, 235) + "...";
            alert.setMessage(alertMessage);
            alert.setCreatedAt(LocalDateTime.now());
            alertRepository.save(alert);
        }

        return savedVh;
    }

    public List<VerificationHistory> getAllVerifications() {
        return verificationHistoryRepository.findAllByOrderByCreatedAtDesc();
    }

    public long getTotalVerificationsCount() {
        return verificationHistoryRepository.count();
    }

    public List<VerificationHistory> getUserVerifications(Long userId) {
        return verificationHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<VerificationHistory> getVerificationsByRisk(String riskLevel) {
        return verificationHistoryRepository.findByRiskLevelOrderByCreatedAtDesc(riskLevel);
    }

    public Optional<VerificationHistory> getVerificationById(Long id) {
        return verificationHistoryRepository.findById(id);
    }

    public List<VerificationHistory> getRecentVerifications() {
        return verificationHistoryRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public long getCountByRisk(String riskLevel) {
        return verificationHistoryRepository.countByRiskLevel(riskLevel);
    }
}
