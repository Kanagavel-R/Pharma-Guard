package com.pharmaguard.repository;

import com.pharmaguard.entity.MedicineRecall;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MedicineRecallRepository extends JpaRepository<MedicineRecall, Long> {

    List<MedicineRecall> findByStatus(String status);

    List<MedicineRecall> findByBatchNumberIgnoreCase(String batchNumber);

    Optional<MedicineRecall> findFirstByBatchNumberIgnoreCaseAndStatus(String batchNumber, String status);

    long countByStatus(String status);
}
