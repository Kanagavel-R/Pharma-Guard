package com.pharmaguard.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "medicine_recalls")
public class MedicineRecall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String medicineName;

    @Column(nullable = false)
    private String batchNumber;

    private String manufacturer;

    @Column(length = 1000, nullable = false)
    private String recallReason;

    private LocalDateTime recallDate = LocalDateTime.now();

    @Column(nullable = false)
    private String severity = "HIGH"; // MEDIUM, HIGH, CRITICAL

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, RESOLVED, CANCELLED

    private Integer affectedStock = 0;

    private Integer affectedLocations = 1;

    public MedicineRecall() {}

    public MedicineRecall(String medicineName, String batchNumber, String manufacturer, String recallReason, String severity, Integer affectedStock) {
        this.medicineName = medicineName;
        this.batchNumber = batchNumber;
        this.manufacturer = manufacturer;
        this.recallReason = recallReason;
        this.severity = severity != null ? severity : "HIGH";
        this.affectedStock = affectedStock != null ? affectedStock : 0;
        this.status = "ACTIVE";
        this.recallDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getRecallReason() { return recallReason; }
    public void setRecallReason(String recallReason) { this.recallReason = recallReason; }

    public LocalDateTime getRecallDate() { return recallDate; }
    public void setRecallDate(LocalDateTime recallDate) { this.recallDate = recallDate; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getAffectedStock() { return affectedStock; }
    public void setAffectedStock(Integer affectedStock) { this.affectedStock = affectedStock; }

    public Integer getAffectedLocations() { return affectedLocations; }
    public void setAffectedLocations(Integer affectedLocations) { this.affectedLocations = affectedLocations; }
}
