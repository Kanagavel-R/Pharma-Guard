package com.pharmaguard.repository;

import com.pharmaguard.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    // ACTIVE ALERTS
    List<Alert> findByResolvedFalse();

    long countByResolvedFalse();

    // DUPLICATE PREVENTION & SEARCH
    boolean existsByMedicineIdAndAlertTypeAndResolvedFalse(Long medicineId, String alertType);

    Optional<Alert> findFirstByMedicineIdAndAlertTypeAndResolvedFalse(Long medicineId, String alertType);

    // RECENT ALERTS
    List<Alert> findTop5ByOrderByCreatedAtDesc();

    // MEDICINE ALERTS
    List<Alert> findByMedicineIdAndResolvedFalse(Long medicineId);

    void deleteByMedicineId(Long medicineId);
}