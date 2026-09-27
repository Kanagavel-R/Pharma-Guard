package com.pharmaguard.service;

import com.pharmaguard.entity.Medicine;
import com.pharmaguard.entity.MedicineRecall;
import com.pharmaguard.repository.MedicineRecallRepository;
import com.pharmaguard.repository.MedicineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RecallService {

    private final MedicineRecallRepository recallRepository;
    private final MedicineRepository medicineRepository;
    private final AlertService alertService;
    private final AuditLogService auditLogService;

    public RecallService(MedicineRecallRepository recallRepository,
                         MedicineRepository medicineRepository,
                         AlertService alertService,
                         AuditLogService auditLogService) {
        this.recallRepository = recallRepository;
        this.medicineRepository = medicineRepository;
        this.alertService = alertService;
        this.auditLogService = auditLogService;
    }

    public List<MedicineRecall> getAllRecalls() {
        return recallRepository.findAll();
    }

    public List<MedicineRecall> getActiveRecalls() {
        return recallRepository.findByStatus("ACTIVE");
    }

    public boolean isBatchRecalled(String batchNumber) {
        if (batchNumber == null || batchNumber.isBlank()) return false;
        return recallRepository.findFirstByBatchNumberIgnoreCaseAndStatus(batchNumber.trim(), "ACTIVE").isPresent();
    }

    @Transactional
    public MedicineRecall createRecall(MedicineRecall recall, String username) {
        if (recall.getBatchNumber() == null || recall.getBatchNumber().isBlank()) {
            throw new IllegalArgumentException("Batch number is required for recall.");
        }
        if (recall.getRecallReason() == null || recall.getRecallReason().isBlank()) {
            throw new IllegalArgumentException("Recall reason is required.");
        }

        // Find affected medicine stock
        Optional<Medicine> affectedOpt = medicineRepository.findFirstByBatchNumberIgnoreCase(recall.getBatchNumber().trim());
        if (affectedOpt.isPresent()) {
            Medicine med = affectedOpt.get();
            recall.setMedicineName(med.getMedicineName());
            recall.setAffectedStock(med.getQuantity());
            recall.setManufacturer(med.getManufacturer() != null ? med.getManufacturer() : "PharmaCorp");
        }

        MedicineRecall saved = recallRepository.save(recall);

        // Raise system alert
        alertService.createAlert(
                "RECALL_WARNING",
                "⚠️ RECALL ALERT: " + saved.getMedicineName() + " (Batch: " + saved.getBatchNumber() + ") — Reason: " + saved.getRecallReason(),
                affectedOpt.map(Medicine::getId).orElse(null),
                saved.getMedicineName()
        );

        auditLogService.logAction(username != null ? username : "Admin", "CREATE_RECALL", "Recall", String.valueOf(saved.getId()), "Created batch recall for " + saved.getBatchNumber());

        return saved;
    }

    @Transactional
    public MedicineRecall updateStatus(Long recallId, String newStatus, String username) {
        MedicineRecall recall = recallRepository.findById(recallId)
                .orElseThrow(() -> new RuntimeException("Recall record not found with ID: " + recallId));

        recall.setStatus(newStatus);
        MedicineRecall saved = recallRepository.save(recall);

        auditLogService.logAction(username != null ? username : "Admin", "UPDATE_RECALL_STATUS", "Recall", String.valueOf(saved.getId()), "Updated recall " + saved.getBatchNumber() + " status to " + newStatus);

        return saved;
    }

    public long getActiveRecallCount() {
        return recallRepository.countByStatus("ACTIVE");
    }
}
