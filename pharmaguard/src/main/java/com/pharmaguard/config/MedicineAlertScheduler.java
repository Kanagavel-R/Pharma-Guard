package com.pharmaguard.config;

import com.pharmaguard.service.MedicineMonitoringEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MedicineAlertScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MedicineAlertScheduler.class);
    private final MedicineMonitoringEngine monitoringEngine;

    public MedicineAlertScheduler(MedicineMonitoringEngine monitoringEngine) {
        this.monitoringEngine = monitoringEngine;
    }

    // Runs once every day at 9:00 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void checkMedicineAlertsDaily() {
        logger.info("PharmaGuard Daily Alert Check trigger firing...");
        monitoringEngine.runFullMonitoringCycle();
    }
}