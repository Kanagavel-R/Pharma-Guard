package com.pharmaguard.repository;

import com.pharmaguard.entity.VerificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationHistoryRepository extends JpaRepository<VerificationHistory, Long> {

    List<VerificationHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<VerificationHistory> findAllByOrderByCreatedAtDesc();

    List<VerificationHistory> findByRiskLevelOrderByCreatedAtDesc(String riskLevel);

    List<VerificationHistory> findTop10ByOrderByCreatedAtDesc();

    long countByRiskLevel(String riskLevel);
}
