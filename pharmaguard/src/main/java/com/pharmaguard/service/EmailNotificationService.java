package com.pharmaguard.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class EmailNotificationService {

    @Value("${pharmaguard.email.enabled:false}")
    private boolean emailEnabled;

    public void sendExpiryEmail(String recipientEmail, String medicineName, String batchNumber, LocalDate expiryDate, long daysRemaining) {
        if (!emailEnabled) {
            // Email service disabled / unconfigured
            return;
        }

        String subject = "⚠️ Pharma Guard — Medicine Expiring in " + daysRemaining + " Days";
        String body = "Hello,\n\n" +
                "Your medicine:\n" +
                medicineName + " (Batch: " + (batchNumber != null ? batchNumber : "N/A") + ")\n\n" +
                "is going to expire in " + daysRemaining + " days.\n\n" +
                "Expiry Date:\n" +
                expiryDate + "\n\n" +
                "Please verify the medicine and follow appropriate healthcare guidance.\n\n" +
                "Regards,\n" +
                "Pharma Guard System";

        System.out.println("EmailNotificationService: [READY] Would send email to " + recipientEmail + " -> Subject: " + subject);
    }
}
