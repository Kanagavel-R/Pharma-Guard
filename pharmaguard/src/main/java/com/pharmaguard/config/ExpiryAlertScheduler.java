package com.pharmaguard.config;

import com.pharmaguard.service.MedicineMonitoringEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ExpiryAlertScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ExpiryAlertScheduler.class);
    private final MedicineMonitoringEngine monitoringEngine;

    public ExpiryAlertScheduler(MedicineMonitoringEngine monitoringEngine) {
        this.monitoringEngine = monitoringEngine;
    }

    // Run automatically once every 6 hours and at midnight
    @Scheduled(cron = "0 0 0/6 * * ?")
    public void runAutomatedExpiryCheck() {
        logger.info("PharmaGuard Automated Expiry Alert Scheduler running expiry check...");
        monitoringEngine.runFullMonitoringCycle();
    }
}

