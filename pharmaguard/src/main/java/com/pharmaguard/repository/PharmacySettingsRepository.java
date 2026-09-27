package com.pharmaguard.repository;

import com.pharmaguard.entity.PharmacySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PharmacySettingsRepository extends JpaRepository<PharmacySettings, Long> {
}
