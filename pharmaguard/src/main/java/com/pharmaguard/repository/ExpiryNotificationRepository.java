package com.pharmaguard.repository;

import com.pharmaguard.entity.ExpiryNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpiryNotificationRepository extends JpaRepository<ExpiryNotification, Long> {

    boolean existsByAlertKey(String alertKey);

    Optional<ExpiryNotification> findByAlertKey(String alertKey);

    List<ExpiryNotification> findByIsReadFalseOrderByCreatedAtDesc();

    List<ExpiryNotification> findAllByOrderByCreatedAtDesc();

    long countByIsReadFalse();

    List<ExpiryNotification> findBySeverityAndIsReadFalseOrderByCreatedAtDesc(String severity);

    @Query("SELECT n FROM ExpiryNotification n WHERE n.daysRemaining <= 3 AND n.isRead = false ORDER BY n.daysRemaining ASC")
    List<ExpiryNotification> findCriticalPopups();

    void deleteByMedicineId(Long medicineId);
}
