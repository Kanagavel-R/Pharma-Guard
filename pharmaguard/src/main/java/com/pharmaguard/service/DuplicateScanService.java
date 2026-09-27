package com.pharmaguard.service;

import com.pharmaguard.entity.ScanEvent;
import com.pharmaguard.repository.ScanEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class DuplicateScanService {

    private final ScanEventRepository scanEventRepository;

    public DuplicateScanService(ScanEventRepository scanEventRepository) {
        this.scanEventRepository = scanEventRepository;
    }

    public void logScanEvent(Long verificationId, Long userId, String barcodeHash, String batchNumber, String deviceId, String location, String result) {
        ScanEvent event = new ScanEvent(verificationId, userId, barcodeHash, batchNumber, deviceId, location, result);
        scanEventRepository.save(event);
    }

    public Map<String, Object> analyzeScanPattern(String barcodeHash, String batchNumber) {
        Map<String, Object> analysis = new HashMap<>();

        long totalScans = 0;
        if (barcodeHash != null && !barcodeHash.isBlank()) {
            totalScans += scanEventRepository.countByBarcodeHash(barcodeHash);
        }
        if (batchNumber != null && !batchNumber.isBlank()) {
            totalScans += scanEventRepository.countByBatchNumber(batchNumber);
        }

        String searchHash = barcodeHash != null ? barcodeHash : "";
        String searchBatch = batchNumber != null ? batchNumber : "";

        long uniqueUsers = scanEventRepository.countUniqueUsersForCode(searchHash, searchBatch);
        long recentScans = scanEventRepository.countRecentScansForCode(searchHash, searchBatch, LocalDateTime.now().minusDays(7));

        analysis.put("totalScans", totalScans);
        analysis.put("uniqueUsers", uniqueUsers);
        analysis.put("recentScans7Days", recentScans);

        boolean suspicious = totalScans > 8 || uniqueUsers > 5 || recentScans > 5;
        analysis.put("suspiciousPattern", suspicious);

        if (suspicious) {
            analysis.put("status", "SUSPICIOUS_PATTERN");
            analysis.put("warning", "⚠️ SUSPICIOUS SCAN PATTERN: Product identifier scanned unusually repeatedly across multiple sessions.");
        } else {
            analysis.put("status", "NORMAL");
            analysis.put("warning", null);
        }

        return analysis;
    }
}
