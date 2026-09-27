package com.pharmaguard.repository;

import com.pharmaguard.entity.MedicineBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicineBatchRepository extends JpaRepository<MedicineBatch, Long> {

    Optional<MedicineBatch> findByBatchNumberIgnoreCase(String batchNumber);

    Optional<MedicineBatch> findByBarcode(String barcode);

    Optional<MedicineBatch> findByGtin(String gtin);

    List<MedicineBatch> findByMedicineNameContainingIgnoreCase(String medicineName);

    List<MedicineBatch> findByManufacturerContainingIgnoreCase(String manufacturer);
}
