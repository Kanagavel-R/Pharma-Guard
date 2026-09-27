package com.pharmaguard.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AlertScheduler {

    private final MedicineMonitoringEngine monitoringEngine;

    public AlertScheduler(MedicineMonitoringEngine monitoringEngine) {
        this.monitoringEngine = monitoringEngine;
    }

    // Runs automatically every 60 seconds
    @Scheduled(fixedRate = 60000)
    public void monitorMedicines() {
        monitoringEngine.runFullMonitoringCycle();
    }
}