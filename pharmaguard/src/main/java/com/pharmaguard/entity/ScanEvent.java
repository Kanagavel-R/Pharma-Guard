package com.pharmaguard.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scan_events", indexes = {
    @Index(name = "idx_scan_user_id", columnList = "userId"),
    @Index(name = "idx_scan_barcode", columnList = "barcodeHash"),
    @Index(name = "idx_scan_batch", columnList = "batchNumber"),
    @Index(name = "idx_scan_timestamp", columnList = "timestamp")
})
public class ScanEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long verificationId;
    private Long userId;
    private String barcodeHash;
    private String batchNumber;
    private String deviceSessionId;
    private String locationRegion;
    private String verificationResult;

    private LocalDateTime timestamp = LocalDateTime.now();

    public ScanEvent() {}

    public ScanEvent(Long verificationId, Long userId, String barcodeHash, String batchNumber, String deviceSessionId, String locationRegion, String verificationResult) {
        this.verificationId = verificationId;
        this.userId = userId;
        this.barcodeHash = barcodeHash;
        this.batchNumber = batchNumber;
        this.deviceSessionId = deviceSessionId;
        this.locationRegion = locationRegion;
        this.verificationResult = verificationResult;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVerificationId() { return verificationId; }
    public void setVerificationId(Long verificationId) { this.verificationId = verificationId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getBarcodeHash() { return barcodeHash; }
    public void setBarcodeHash(String barcodeHash) { this.barcodeHash = barcodeHash; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getDeviceSessionId() { return deviceSessionId; }
    public void setDeviceSessionId(String deviceSessionId) { this.deviceSessionId = deviceSessionId; }

    public String getLocationRegion() { return locationRegion; }
    public void setLocationRegion(String locationRegion) { this.locationRegion = locationRegion; }

    public String getVerificationResult() { return verificationResult; }
    public void setVerificationResult(String verificationResult) { this.verificationResult = verificationResult; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
