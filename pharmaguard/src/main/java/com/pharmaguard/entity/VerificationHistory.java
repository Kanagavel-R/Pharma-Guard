package com.pharmaguard.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "verification_history")
public class VerificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String username;

    private String medicineName;
    private String genericName;
    private String strength;
    private String dosageForm;

    private String batchNumber;
    private String manufacturer;
    private String barcodeHash;

    @Column(columnDefinition = "TEXT")
    private String rawOcrText;

    @Column(columnDefinition = "TEXT")
    private String extractedInfoJson;

    @Column(columnDefinition = "TEXT")
    private String verificationSignalsJson;

    private int riskScore; // 0 to 100
    private String riskLevel; // LOW, MODERATE, HIGH, CRITICAL

    private String verificationStatus; // Verified, Partially Verified, Unable to Verify, Suspicious, High Risk

    @Column(columnDefinition = "TEXT")
    private String detectedIssuesJson;

    @Column(columnDefinition = "TEXT")
    private String aiExplanation;

    private LocalDateTime createdAt = LocalDateTime.now();

    public VerificationHistory() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public String getDosageForm() { return dosageForm; }
    public void setDosageForm(String dosageForm) { this.dosageForm = dosageForm; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getBarcodeHash() { return barcodeHash; }
    public void setBarcodeHash(String barcodeHash) { this.barcodeHash = barcodeHash; }

    public String getRawOcrText() { return rawOcrText; }
    public void setRawOcrText(String rawOcrText) { this.rawOcrText = rawOcrText; }

    public String getExtractedInfoJson() { return extractedInfoJson; }
    public void setExtractedInfoJson(String extractedInfoJson) { this.extractedInfoJson = extractedInfoJson; }

    public String getVerificationSignalsJson() { return verificationSignalsJson; }
    public void setVerificationSignalsJson(String verificationSignalsJson) { this.verificationSignalsJson = verificationSignalsJson; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getDetectedIssuesJson() { return detectedIssuesJson; }
    public void setDetectedIssuesJson(String detectedIssuesJson) { this.detectedIssuesJson = detectedIssuesJson; }

    public String getAiExplanation() { return aiExplanation; }
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
