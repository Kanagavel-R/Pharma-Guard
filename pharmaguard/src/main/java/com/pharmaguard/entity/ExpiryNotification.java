package com.pharmaguard.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expiry_notifications", indexes = {
    @Index(name = "idx_exp_notif_alert_key", columnList = "alertKey"),
    @Index(name = "idx_exp_notif_user_id", columnList = "userId"),
    @Index(name = "idx_exp_notif_is_read", columnList = "isRead"),
    @Index(name = "idx_exp_notif_expiry_date", columnList = "expiryDate"),
    @Index(name = "idx_exp_notif_severity", columnList = "severity")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_exp_notif_alert_key", columnNames = {"alertKey"})
})
public class ExpiryNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false)
    private Long medicineId;

    private String medicineName;
    private String batchNumber;

    @Column(nullable = false)
    private String type = "EXPIRY_ALERT";

    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String severity; // CRITICAL, HIGH, WARNING, EXPIRED

    private LocalDate expiryDate;

    private long daysRemaining;

    private boolean isRead = false;

    @Column(unique = true, nullable = false)
    private String alertKey; // userId_medicineId_EXPIRY_daysRemaining

    private LocalDateTime createdAt = LocalDateTime.now();

    public ExpiryNotification() {}

    public ExpiryNotification(Long userId, Long medicineId, String medicineName, String batchNumber, String title, String message, String severity, LocalDate expiryDate, long daysRemaining, String alertKey) {
        this.userId = userId;
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.batchNumber = batchNumber;
        this.title = title;
        this.message = message;
        this.severity = severity;
        this.expiryDate = expiryDate;
        this.daysRemaining = daysRemaining;
        this.alertKey = alertKey;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getMedicineId() { return medicineId; }
    public void setMedicineId(Long medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public long getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(long daysRemaining) { this.daysRemaining = daysRemaining; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getAlertKey() { return alertKey; }
    public void setAlertKey(String alertKey) { this.alertKey = alertKey; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
