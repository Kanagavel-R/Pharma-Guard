package com.pharmaguard.service;

import com.pharmaguard.dto.MedicineIntelligenceDTO;
import com.pharmaguard.entity.Alert;
import com.pharmaguard.entity.Medicine;
import com.pharmaguard.repository.AlertRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final InventoryIntelligenceService intelligenceService;
    private final MedicineRepositoryProvider medicineRepositoryProvider;
    private final SettingsService settingsService;

    private static final int EXPIRING_SOON_DAYS = 30;

    public AlertService(AlertRepository alertRepository,
                        @Lazy InventoryIntelligenceService intelligenceService,
                        @Lazy MedicineRepositoryProvider medicineRepositoryProvider,
                        @Lazy SettingsService settingsService) {
        this.alertRepository = alertRepository;
        this.intelligenceService = intelligenceService;
        this.medicineRepositoryProvider = medicineRepositoryProvider;
        this.settingsService = settingsService;
    }

    // =========================================================
    // REFRESH ALL MEDICINE ALERTS REAL-TIME
    // =========================================================
    @Transactional
    public void refreshAllMedicineAlerts() {
        List<Medicine> medicines = medicineRepositoryProvider.getAllMedicines();
        for (Medicine medicine : medicines) {
            checkMedicineAlerts(medicine);
        }
    }

    // =========================================================
    // CREATE OR UPDATE ALERT
    // =========================================================
    @Transactional
    public void createAlert(String alertType, String message, Long medicineId, String medicineName) {
        if (settingsService != null) {
            com.pharmaguard.entity.PharmacySettings s = settingsService.getSettings();
            if ("LOW_STOCK".equals(alertType) || "STOCK_OUT_RISK".equals(alertType)) {
                if (!s.isLowStockAlerts()) return;
            } else if ("EXPIRED".equals(alertType) || "EXPIRING_SOON".equals(alertType) || "EXPIRY_WASTE_RISK".equals(alertType)) {
                if (!s.isExpiryAlerts()) return;
            } else if ("SUPPLIER".equals(alertType)) {
                if (!s.isSupplierAlerts()) return;
            } else if ("VERIFICATION".equals(alertType)) {
                if (!s.isVerificationAlerts()) return;
            }
        }

        Optional<Alert> existingOpt = alertRepository.findFirstByMedicineIdAndAlertTypeAndResolvedFalse(medicineId, alertType);

        if (existingOpt.isPresent()) {
            Alert existing = existingOpt.get();
            existing.setMessage(message);
            existing.setMedicineName(medicineName);
            alertRepository.save(existing);
            return;
        }

        Alert alert = new Alert();
        alert.setAlertType(alertType);
        alert.setMessage(message);
        alert.setMedicineId(medicineId);
        alert.setMedicineName(medicineName);
        alert.setResolved(false);
        alert.setCreatedAt(LocalDateTime.now());

        alertRepository.save(alert);
    }

    // =========================================================
    // CHECK ALL MEDICINE ALERTS (REAL-TIME EVALUATION)
    // =========================================================
    @Transactional
    public void checkMedicineAlerts(Medicine medicine) {
        if (medicine == null || medicine.getId() == null) {
            return;
        }

        // First resolve alerts that are no longer valid
        clearStaleAlerts(medicine);

        MedicineIntelligenceDTO intel = intelligenceService.evaluateMedicineIntelligence(medicine);

        // 1. LOW STOCK
        if (medicine.getQuantity() <= medicine.getReorderLevel()) {
            createAlert(
                    "LOW_STOCK",
                    "Low stock warning: " + medicine.getMedicineName() + " (Batch: " + medicine.getBatchNumber() + ") has " + medicine.getQuantity() + " units remaining (Reorder Level: " + medicine.getReorderLevel() + ").",
                    medicine.getId(),
                    medicine.getMedicineName()
            );
        }

        // 2. EXPIRED & 3. EXPIRING SOON
        LocalDate expiryDate = medicine.getExpiryDate();
        LocalDate today = LocalDate.now();

        if (expiryDate != null) {
            if (expiryDate.isBefore(today)) {
                createAlert(
                        "EXPIRED",
                        "CRITICAL: " + medicine.getMedicineName() + " (Batch: " + medicine.getBatchNumber() + ") expired on " + expiryDate + ".",
                        medicine.getId(),
                        medicine.getMedicineName()
                );
            } else if (!expiryDate.isAfter(today.plusDays(EXPIRING_SOON_DAYS))) {
                createAlert(
                        "EXPIRING_SOON",
                        "Expiring Soon: " + medicine.getMedicineName() + " (Batch: " + medicine.getBatchNumber() + ") expires on " + expiryDate + ".",
                        medicine.getId(),
                        medicine.getMedicineName()
                );
            }
        }

        // 4. STOCK OUT RISK
        if ("STOCK_OUT_RISK".equals(intel.getStockOutStatus()) || medicine.getQuantity() == 0) {
            createAlert(
                    "STOCK_OUT_RISK",
                    "Stock-Out Risk: " + medicine.getMedicineName() + " is estimated to run out " +
                            (intel.getEstimatedStockOutDays() != null ? "in " + intel.getEstimatedStockOutDays() + " days." : "immediately (0 stock)."),
                    medicine.getId(),
                    medicine.getMedicineName()
            );
        }

        // 5. REORDER RECOMMENDED
        if (intel.isReorderRecommended()) {
            createAlert(
                    "REORDER_RECOMMENDED",
                    "Reorder Recommended: " + intel.getReorderExplanation() + " Recommended order quantity: " + intel.getRecommendedReorderQuantity() + " units.",
                    medicine.getId(),
                    medicine.getMedicineName()
            );
        }

        // 6. EXPIRY WASTE RISK
        if (intel.isExpiryWasteRisk()) {
            createAlert(
                    "EXPIRY_WASTE_RISK",
                    "Expiry Waste Risk: " + intel.getExpiryWasteExplanation(),
                    medicine.getId(),
                    medicine.getMedicineName()
            );
        }

        // 7. FEFO RECOMMENDATION
        if (intel.getFefoRecommendedBatch() != null) {
            createAlert(
                    "FEFO_RECOMMENDATION",
                    intel.getFefoRecommendationMessage(),
                    medicine.getId(),
                    medicine.getMedicineName()
            );
        }
    }

    // =========================================================
    // REMOVE STALE ALERTS
    // =========================================================
    @Transactional
    public void clearStaleAlerts(Medicine medicine) {
        if (medicine == null || medicine.getId() == null) {
            return;
        }

        List<Alert> activeAlerts = alertRepository.findByMedicineIdAndResolvedFalse(medicine.getId());
        LocalDate today = LocalDate.now();
        LocalDate expiryDate = medicine.getExpiryDate();
        MedicineIntelligenceDTO intel = intelligenceService.evaluateMedicineIntelligence(medicine);

        for (Alert alert : activeAlerts) {
            boolean stillValid = true;

            if ("LOW_STOCK".equals(alert.getAlertType())) {
                stillValid = medicine.getQuantity() <= medicine.getReorderLevel();
            } else if ("EXPIRED".equals(alert.getAlertType())) {
                stillValid = expiryDate != null && expiryDate.isBefore(today);
            } else if ("EXPIRING_SOON".equals(alert.getAlertType())) {
                stillValid = expiryDate != null && !expiryDate.isBefore(today) && !expiryDate.isAfter(today.plusDays(EXPIRING_SOON_DAYS));
            } else if ("STOCK_OUT_RISK".equals(alert.getAlertType())) {
                stillValid = "STOCK_OUT_RISK".equals(intel.getStockOutStatus()) || medicine.getQuantity() == 0;
            } else if ("REORDER_RECOMMENDED".equals(alert.getAlertType())) {
                stillValid = intel.isReorderRecommended();
            } else if ("EXPIRY_WASTE_RISK".equals(alert.getAlertType())) {
                stillValid = intel.isExpiryWasteRisk();
            } else if ("FEFO_RECOMMENDATION".equals(alert.getAlertType())) {
                stillValid = intel.getFefoRecommendedBatch() != null;
            } else {
                stillValid = false;
            }

            if (!stillValid) {
                alert.setResolved(true);
                alert.setResolvedAt(LocalDateTime.now());
                alertRepository.save(alert);
            }
        }
    }

    @Transactional
    public void deleteAlertsForMedicine(Long medicineId) {
        alertRepository.deleteByMedicineId(medicineId);
    }

    public List<Alert> getActiveAlerts() {
        return alertRepository.findByResolvedFalse();
    }

    public long getActiveAlertCount() {
        return alertRepository.countByResolvedFalse();
    }

    @Transactional
    public void resolveAlert(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found with ID: " + id));

        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        alertRepository.save(alert);
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    public List<Alert> getRecentAlerts() {
        return alertRepository.findTop5ByOrderByCreatedAtDesc();
    }

    public String getAlertStatus(Alert alert) {
        if (alert == null) {
            return "UNKNOWN";
        }
        return alert.isResolved() ? "RESOLVED" : "ACTIVE";
    }

    @Service
    public static class MedicineRepositoryProvider {
        private final com.pharmaguard.repository.MedicineRepository medicineRepository;

        public MedicineRepositoryProvider(com.pharmaguard.repository.MedicineRepository medicineRepository) {
            this.medicineRepository = medicineRepository;
        }

        public List<Medicine> getAllMedicines() {
            return medicineRepository.findAll();
        }
    }
}