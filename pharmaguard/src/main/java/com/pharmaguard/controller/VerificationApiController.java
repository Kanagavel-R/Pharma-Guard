package com.pharmaguard.controller;

import com.pharmaguard.entity.VerificationHistory;
import com.pharmaguard.service.BatchVerificationService;
import com.pharmaguard.service.OcrProcessingService;
import com.pharmaguard.service.VerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medicine")
public class VerificationApiController {

    private final OcrProcessingService ocrProcessingService;
    private final BatchVerificationService batchVerificationService;
    private final VerificationService verificationService;
    private final com.pharmaguard.repository.MedicineBatchRepository medicineBatchRepository;
    private final com.pharmaguard.repository.MedicineRepository medicineRepository;
    private final com.pharmaguard.service.MedicineService medicineService;

    public VerificationApiController(
            OcrProcessingService ocrProcessingService,
            BatchVerificationService batchVerificationService,
            VerificationService verificationService,
            com.pharmaguard.repository.MedicineBatchRepository medicineBatchRepository,
            com.pharmaguard.repository.MedicineRepository medicineRepository,
            com.pharmaguard.service.MedicineService medicineService) {
        this.ocrProcessingService = ocrProcessingService;
        this.batchVerificationService = batchVerificationService;
        this.verificationService = verificationService;
        this.medicineBatchRepository = medicineBatchRepository;
        this.medicineRepository = medicineRepository;
        this.medicineService = medicineService;
    }

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> uploadAndScanImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "Uploaded image file is empty.");
            return ResponseEntity.badRequest().body(response);
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/jpg") && !contentType.equals("image/png") && !contentType.equals("image/webp"))) {
            response.put("success", false);
            response.put("message", "Invalid file type. Supported formats: JPG, JPEG, PNG, WEBP.");
            return ResponseEntity.badRequest().body(response);
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            response.put("success", false);
            response.put("message", "File size exceeds 10MB limit.");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("success", true);
        response.put("message", "Image validated successfully.");
        response.put("fileName", file.getOriginalFilename());
        response.put("fileSize", file.getSize());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/ocr")
    public ResponseEntity<Map<String, Object>> processOcr(@RequestBody Map<String, String> payload) {
        String rawText = payload.get("rawText");
        Map<String, Object> ocrResult = ocrProcessingService.processOcrText(rawText);
        return ResponseEntity.ok(ocrResult);
    }

    @PostMapping("/verify")
    public ResponseEntity<VerificationHistory> runVerification(
            @RequestBody Map<String, Object> req,
            Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "admin";
        String rawOcrText = (String) req.get("rawOcrText");
        String barcodeInput = (String) req.get("barcode");
        String manualMedicineName = (String) req.get("medicineName");
        String manualBatchNumber = (String) req.get("batchNumber");
        String manualManufacturer = (String) req.get("manufacturer");

        LocalDate manualExpiry = null;
        if (req.get("expiryDate") != null && !req.get("expiryDate").toString().isBlank()) {
            try {
                manualExpiry = LocalDate.parse(req.get("expiryDate").toString());
            } catch (Exception ignored) {}
        }

        String deviceSessionId = (String) req.getOrDefault("deviceSessionId", "SESSION-" + System.currentTimeMillis());
        String locationRegion = (String) req.getOrDefault("locationRegion", "Consent-Default");

        VerificationHistory history = verificationService.processVerification(
                1L, username, rawOcrText, barcodeInput, manualMedicineName, manualBatchNumber, manualManufacturer, manualExpiry, deviceSessionId, locationRegion
        );

        return ResponseEntity.ok(history);
    }

    @PostMapping("/verify-qr")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Map<String, Object>> verifyQr(
            @RequestBody Map<String, String> payload,
            Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "admin";
        String prefix = payload.get("prefix");
        String medicine = payload.get("medicine");
        String batch = payload.get("batch");
        String serial = payload.get("serial");
        String rawQrText = payload.get("rawQrText");

        Map<String, Object> response = new HashMap<>();

        // 1. Validate Prefix & Format
        if (prefix == null || !"PHARMA-GUARD".equalsIgnoreCase(prefix.trim())) {
            response.put("success", false);
            response.put("status", "INVALID_FORMAT");
            response.put("action", "not_verified");
            response.put("title", "Invalid Pharma Guard QR");
            response.put("message", "This QR code is readable but is not a valid Pharma Guard medicine verification code.");
            return ResponseEntity.ok(response);
        }

        // 2. Query Database for Batch Record
        java.util.Optional<com.pharmaguard.entity.MedicineBatch> batchOpt = (batch != null && !batch.isBlank()) ?
                medicineBatchRepository.findByBatchNumberIgnoreCase(batch.trim()) : java.util.Optional.empty();

        if (batchOpt.isEmpty()) {
            List<com.pharmaguard.entity.MedicineBatch> byMed = (medicine != null && !medicine.isBlank()) ?
                    medicineBatchRepository.findByMedicineNameContainingIgnoreCase(medicine.trim()) : List.of();
            if (!byMed.isEmpty()) {
                batchOpt = java.util.Optional.of(byMed.get(0));
            }
        }

        if (batchOpt.isEmpty()) {
            VerificationHistory history = verificationService.processVerification(
                    1L, username, rawQrText, serial, medicine, batch, "Unregistered Mfr", null, "QR-SESSION-" + System.currentTimeMillis(), "Scan-Location"
            );

            response.put("success", false);
            response.put("status", "NOT_FOUND");
            response.put("action", "not_verified");
            response.put("title", "Medicine Not Found");
            response.put("message", "The QR code was detected, but this medicine could not be verified against the Pharma Guard database.");
            response.put("verification", history);
            return ResponseEntity.ok(response);
        }

        com.pharmaguard.entity.MedicineBatch mb = batchOpt.get();
        VerificationHistory history = verificationService.processVerification(
                1L, username, rawQrText, serial, mb.getMedicineName(), mb.getBatchNumber(), mb.getManufacturer(), mb.getExpiryDate(), "QR-SESSION-" + System.currentTimeMillis(), "Scan-Location"
        );

        // 3. Check for duplicates in Medicine DB & automatically insert if not exists
        String targetBatch = mb.getBatchNumber();
        java.util.Optional<com.pharmaguard.entity.Medicine> existingMedOpt = medicineRepository.findFirstByBatchNumberIgnoreCase(targetBatch);

        com.pharmaguard.entity.Medicine savedMedicine;
        String action;
        String dbMessage;
        String title;

        if (existingMedOpt.isPresent()) {
            savedMedicine = existingMedOpt.get();
            action = "already_exists";
            dbMessage = "Already exists in database";
            title = "Medicine Verified";
        } else {
            com.pharmaguard.entity.Medicine newMed = new com.pharmaguard.entity.Medicine();
            newMed.setMedicineName(mb.getMedicineName());
            newMed.setCategory("General / Analgesics");
            newMed.setBatchNumber(mb.getBatchNumber());
            newMed.setQuantity(100);
            newMed.setPrice(15.00);
            newMed.setExpiryDate(mb.getExpiryDate() != null ? mb.getExpiryDate() : LocalDate.now().plusYears(3));
            newMed.setReorderLevel(10);
            newMed.setSupplierName(mb.getManufacturer() != null ? mb.getManufacturer() : "Pharma Guard Certified Labs");
            newMed.setManufacturer(mb.getManufacturer() != null ? mb.getManufacturer() : "Pharma Guard Certified Labs");
            newMed.setGenericName(mb.getGenericName());
            newMed.setStrength(mb.getStrength());
            newMed.setDosageForm(mb.getDosageForm());
            newMed.setBarcode(serial != null ? serial : (mb.getBarcode() != null ? mb.getBarcode() : mb.getGtin()));

            savedMedicine = medicineService.saveMedicine(newMed);
            action = "created";
            dbMessage = "✓ Added to Medicine Database";
            title = "Medicine Verified & Added";
        }

        response.put("success", true);
        response.put("status", "VERIFIED");
        response.put("action", action);
        response.put("title", title);
        response.put("message", "Medicine verified successfully against Pharma Guard database.");
        response.put("dbMessage", dbMessage);
        response.put("medicine", history.getMedicineName());
        response.put("batch", history.getBatchNumber());
        response.put("serial", serial != null ? serial : history.getBarcodeHash());
        response.put("verificationStatus", "Genuine / Verified");
        response.put("verification", history);
        response.put("medicineRecord", savedMedicine);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch/verify")
    public ResponseEntity<Map<String, Object>> verifyBatch(@RequestBody Map<String, String> payload) {
        String batchNumber = payload.get("batchNumber");
        String medicineName = payload.get("medicineName");
        String manufacturer = payload.get("manufacturer");

        Map<String, Object> res = batchVerificationService.verifyBatch(batchNumber, medicineName, manufacturer, null);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/history")
    public ResponseEntity<List<VerificationHistory>> getHistory() {
        return ResponseEntity.ok(verificationService.getAllVerifications());
    }

    @GetMapping("/verification/{id}")
    public ResponseEntity<VerificationHistory> getVerificationById(@PathVariable("id") Long id) {
        return verificationService.getVerificationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
