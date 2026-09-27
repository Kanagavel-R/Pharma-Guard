package com.pharmaguard.service;

import com.pharmaguard.entity.Medicine;
import com.pharmaguard.repository.MedicineRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicineMonitoringService {

    private final MedicineRepository medicineRepository;
    private final AlertService alertService;

    public MedicineMonitoringService(
            MedicineRepository medicineRepository,
            AlertService alertService) {

        this.medicineRepository = medicineRepository;
        this.alertService = alertService;
    }

    public void checkMedicines() {
        List<Medicine> medicines = medicineRepository.findAll();
        for (Medicine medicine : medicines) {
            alertService.checkMedicineAlerts(medicine);
        }
    }
}