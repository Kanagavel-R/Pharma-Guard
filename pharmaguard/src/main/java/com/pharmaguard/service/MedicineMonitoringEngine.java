package com.pharmaguard.service;

import com.pharmaguard.entity.Medicine;
import com.pharmaguard.repository.MedicineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MedicineMonitoringEngine {

    private static final Logger logger = LoggerFactory.getLogger(MedicineMonitoringEngine.class);

    private final MedicineRepository medicineRepository;
    private final AlertService alertService;
    private final ExpiryAlertService expiryAlertService;

    public MedicineMonitoringEngine(
            MedicineRepository medicineRepository,
            AlertService alertService,
            ExpiryAlertService expiryAlertService) {
        this.medicineRepository = medicineRepository;
        this.alertService = alertService;
        this.expiryAlertService = expiryAlertService;
    }

    /**
     * Single entry point for scheduled or triggered medicine monitoring.
     * Evaluates inventory rules, deduplicates alerts, and updates notification states.
     */
    @Transactional
    public void runFullMonitoringCycle() {
        logger.info("PharmaGuard Monitoring Engine: Starting full inventory & expiry monitoring cycle...");
        List<Medicine> medicines = medicineRepository.findAll();

        for (Medicine medicine : medicines) {
            if (medicine != null && medicine.getId() != null) {
                // 1. Evaluate stock and intelligence alerts idempotently
                alertService.checkMedicineAlerts(medicine);
                // 2. Evaluate expiry notifications with stable alert keys
                expiryAlertService.evaluateMedicineExpiry(medicine, 1L);
            }
        }
        logger.info("PharmaGuard Monitoring Engine: Completed monitoring cycle for {} medicines.", medicines.size());
    }

    @Transactional
    public void monitorSingleMedicine(Medicine medicine) {
        if (medicine != null && medicine.getId() != null) {
            alertService.checkMedicineAlerts(medicine);
            expiryAlertService.evaluateMedicineExpiry(medicine, 1L);
        }
    }
}
