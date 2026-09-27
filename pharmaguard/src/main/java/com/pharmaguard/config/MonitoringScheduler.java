package com.pharmaguard.config;

import com.pharmaguard.service.MedicineMonitoringEngine;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MonitoringScheduler {

    private final MedicineMonitoringEngine monitoringEngine;

    public MonitoringScheduler(MedicineMonitoringEngine monitoringEngine) {
        this.monitoringEngine = monitoringEngine;
    }

    @Scheduled(fixedRate = 120000)
    public void monitorMedicines() {
        monitoringEngine.runFullMonitoringCycle();
    }
}