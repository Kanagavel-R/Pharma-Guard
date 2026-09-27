package com.pharmaguard.config;

import com.pharmaguard.entity.Medicine;
import com.pharmaguard.service.AlertService;
import com.pharmaguard.service.MedicineService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MedicineAlertInitializer
        implements CommandLineRunner {

    private final MedicineService medicineService;
    private final AlertService alertService;
    private final com.pharmaguard.service.ExpiryAlertService expiryAlertService;

    public MedicineAlertInitializer(
            MedicineService medicineService,
            AlertService alertService,
            com.pharmaguard.service.ExpiryAlertService expiryAlertService) {

        this.medicineService = medicineService;
        this.alertService = alertService;
        this.expiryAlertService = expiryAlertService;
    }

    @Override
    public void run(String... args) {
        java.time.LocalDate today = java.time.LocalDate.now();

        // Seed 3-day acceptance medicine if not already present
        if (!medicineService.getAllMedicines().stream().anyMatch(m -> "PCM-3DAY-ACCEPTANCE".equals(m.getBatchNumber()))) {
            Medicine m1 = new Medicine("Paracetamol 500mg", "Analgesic", "PCM-3DAY-ACCEPTANCE", 100, 15.00, today.plusDays(3), 20, "ABC Pharmaceuticals");
            m1.setManufacturer("ABC Pharmaceuticals Ltd.");
            medicineService.saveMedicine(m1);

            Medicine m2 = new Medicine("Amoxicillin 500mg", "Antibiotic", "AMX-6DAY-TEST", 75, 45.00, today.plusDays(6), 15, "Apex Global Pharma");
            m2.setManufacturer("Apex Global Pharma");
            medicineService.saveMedicine(m2);

            Medicine m3 = new Medicine("Cetirizine 10mg", "Antihistamine", "CET-18DAY-TEST", 120, 8.50, today.plusDays(18), 25, "Sun Health Sciences");
            m3.setManufacturer("Sun Health Sciences");
            medicineService.saveMedicine(m3);
        }

        for (Medicine medicine : medicineService.getAllMedicines()) {
            alertService.checkMedicineAlerts(medicine);
        }
        expiryAlertService.evaluateAllMedicinesExpiry(1L);

        System.out.println("Pharma Guard: Baseline expiry alert evaluation completed.");
    }
}