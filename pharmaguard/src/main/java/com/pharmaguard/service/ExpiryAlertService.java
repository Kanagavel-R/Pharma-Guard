package com.pharmaguard.service;

import com.pharmaguard.entity.ExpiryNotification;
import com.pharmaguard.entity.Medicine;
import com.pharmaguard.repository.ExpiryNotificationRepository;
import com.pharmaguard.repository.MedicineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExpiryAlertService {

    private final MedicineRepository medicineRepository;
    private final ExpiryNotificationRepository notificationRepository;
    private final EmailNotificationService emailNotificationService;

    public ExpiryAlertService(
            MedicineRepository medicineRepository,
            ExpiryNotificationRepository notificationRepository,
            EmailNotificationService emailNotificationService) {
        this.medicineRepository = medicineRepository;
        this.notificationRepository = notificationRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional
    public void evaluateAllMedicinesExpiry(Long userId) {
        List<Medicine> medicines = medicineRepository.findAll();
        for (Medicine m : medicines) {
            evaluateMedicineExpiry(m, userId != null ? userId : 1L);
        }
    }

    @Transactional
    public void evaluateMedicineExpiry(Medicine medicine, Long userId) {
        if (medicine == null || medicine.getId() == null || medicine.getExpiryDate() == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate expiryDate = medicine.getExpiryDate();
        long daysRemaining = ChronoUnit.DAYS.between(today, expiryDate);

        String severity;
        String title;
        String message;

        if (daysRemaining < 0) {
            severity = "EXPIRED";
            title = "🚨 Medicine Expired";
            message = medicine.getMedicineName() + " has passed its stated expiry date (" + expiryDate + "). Do not rely on Pharma Guard to determine whether it is safe to use.";
        } else if (daysRemaining == 0) {
            severity = "CRITICAL";
            title = "🔴 Expiring Today";
            message = medicine.getMedicineName() + " expires today (" + expiryDate + "). Check packaging & follow healthcare guidance.";
        } else if (daysRemaining <= 3) {
            severity = "CRITICAL";
            title = "🔴 Critical Expiry Alert";
            message = medicine.getMedicineName() + " expires in " + daysRemaining + " day" + (daysRemaining > 1 ? "s" : "") + " (" + expiryDate + "). Check packaging & follow healthcare guidance.";
        } else if (daysRemaining <= 7) {
            severity = "HIGH";
            title = "🟠 Expiring Soon";
            message = medicine.getMedicineName() + " expires in " + daysRemaining + " days (" + expiryDate + ").";
        } else if (daysRemaining <= 30) {
            severity = "WARNING";
            title = "🟡 Expiring This Month";
            message = medicine.getMedicineName() + " expires in " + daysRemaining + " days (" + expiryDate + ").";
        } else {
            // More than 30 days - no active alert needed
            return;
        }

        String alertKey = userId + "_" + medicine.getId() + "_EXPIRY_" + expiryDate;

        Optional<ExpiryNotification> existingOpt = notificationRepository.findByAlertKey(alertKey);
        if (existingOpt.isPresent()) {
            ExpiryNotification existing = existingOpt.get();
            existing.setDaysRemaining(daysRemaining);
            existing.setSeverity(severity);
            existing.setTitle(title);
            existing.setMessage(message);
            notificationRepository.save(existing);
        } else {
            ExpiryNotification notification = new ExpiryNotification(
                    userId,
                    medicine.getId(),
                    medicine.getMedicineName(),
                    medicine.getBatchNumber(),
                    title,
                    message,
                    severity,
                    expiryDate,
                    daysRemaining,
                    alertKey
            );
            notificationRepository.save(notification);

            // Optional email notification if critical
            if ("CRITICAL".equals(severity)) {
                emailNotificationService.sendExpiryEmail("admin@pharmaguard.com", medicine.getMedicineName(), medicine.getBatchNumber(), expiryDate, daysRemaining);
            }
        }
    }

    public List<ExpiryNotification> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<ExpiryNotification> getUnreadNotifications() {
        return notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
    }

    public long getUnreadCount() {
        return notificationRepository.countByIsReadFalse();
    }

    public List<ExpiryNotification> getCriticalPopups() {
        return notificationRepository.findCriticalPopups();
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Optional<ExpiryNotification> opt = notificationRepository.findById(notificationId);
        if (opt.isPresent()) {
            ExpiryNotification n = opt.get();
            n.setRead(true);
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void markAllAsRead() {
        List<ExpiryNotification> unread = notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
        for (ExpiryNotification n : unread) {
            n.setRead(true);
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    // Filtered Expiring Medicines for the dedicated Expiry Monitor Page
    public List<Medicine> getExpiringMedicines(int maxDays) {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(maxDays);
        return medicineRepository.findAll().stream()
                .filter(m -> m.getExpiryDate() != null)
                .filter(m -> !m.getExpiryDate().isBefore(today) && !m.getExpiryDate().isAfter(targetDate))
                .sorted((a, b) -> a.getExpiryDate().compareTo(b.getExpiryDate()))
                .collect(Collectors.toList());
    }

    public List<Medicine> getExpiredMedicines() {
        LocalDate today = LocalDate.now();
        return medicineRepository.findAll().stream()
                .filter(m -> m.getExpiryDate() != null && m.getExpiryDate().isBefore(today))
                .sorted((a, b) -> b.getExpiryDate().compareTo(a.getExpiryDate()))
                .collect(Collectors.toList());
    }

    public long countCriticalExpiring(int maxDays) {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(maxDays);
        return medicineRepository.findAll().stream()
                .filter(m -> m.getExpiryDate() != null)
                .filter(m -> !m.getExpiryDate().isBefore(today) && !m.getExpiryDate().isAfter(targetDate))
                .count();
    }

    public long countExpired() {
        LocalDate today = LocalDate.now();
        return medicineRepository.findAll().stream()
                .filter(m -> m.getExpiryDate() != null && m.getExpiryDate().isBefore(today))
                .count();
    }
}
