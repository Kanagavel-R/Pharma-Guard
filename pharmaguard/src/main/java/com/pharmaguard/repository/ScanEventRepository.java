package com.pharmaguard.repository;

import com.pharmaguard.entity.ScanEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScanEventRepository extends JpaRepository<ScanEvent, Long> {

    List<ScanEvent> findByBarcodeHash(String barcodeHash);

    List<ScanEvent> findByBatchNumber(String batchNumber);

    long countByBarcodeHash(String barcodeHash);

    long countByBatchNumber(String batchNumber);

    @Query("SELECT COUNT(DISTINCT s.userId) FROM ScanEvent s WHERE s.barcodeHash = :hash OR s.batchNumber = :batch")
    long countUniqueUsersForCode(@Param("hash") String hash, @Param("batch") String batch);

    @Query("SELECT COUNT(s) FROM ScanEvent s WHERE (s.barcodeHash = :hash OR s.batchNumber = :batch) AND s.timestamp >= :since")
    long countRecentScansForCode(@Param("hash") String hash, @Param("batch") String batch, @Param("since") LocalDateTime since);
}
