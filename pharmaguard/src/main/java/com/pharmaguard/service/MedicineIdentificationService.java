package com.pharmaguard.service;

import com.pharmaguard.entity.Medicine;
import com.pharmaguard.entity.MedicineBatch;
import com.pharmaguard.repository.MedicineBatchRepository;
import com.pharmaguard.repository.MedicineRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MedicineIdentificationService {

    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository medicineBatchRepository;

    public MedicineIdentificationService(MedicineRepository medicineRepository, MedicineBatchRepository medicineBatchRepository) {
        this.medicineRepository = medicineRepository;
        this.medicineBatchRepository = medicineBatchRepository;
    }

    public Map<String, Object> identifyMedicine(String code, String batchNumber, String medicineName, String manufacturer) {
        Map<String, Object> response = new HashMap<>();
        
        // 1. Try batch code lookup in MedicineBatch database
        if (batchNumber != null && !batchNumber.isBlank()) {
            Optional<MedicineBatch> batchOpt = medicineBatchRepository.findByBatchNumberIgnoreCase(batchNumber.trim());
            if (batchOpt.isPresent()) {
                MedicineBatch mb = batchOpt.get();
                response.put("matched", true);
                response.put("matchSource", "BATCH_REGISTRY");
                response.put("confidence", 0.95);
                response.put("medicineName", mb.getMedicineName());
                response.put("batchNumber", mb.getBatchNumber());
                response.put("manufacturer", mb.getManufacturer());
                response.put("genericName", mb.getGenericName());
                response.put("strength", mb.getStrength());
                response.put("dosageForm", mb.getDosageForm());
                response.put("expiryDate", mb.getExpiryDate());
                response.put("batchRecord", mb);
                return response;
            }
        }

        // 2. Try code / GTIN / barcode lookup
        if (code != null && !code.isBlank()) {
            Optional<MedicineBatch> batchByCode = medicineBatchRepository.findByBarcode(code.trim());
            if (batchByCode.isEmpty()) {
                batchByCode = medicineBatchRepository.findByGtin(code.trim());
            }
            if (batchByCode.isPresent()) {
                MedicineBatch mb = batchByCode.get();
                response.put("matched", true);
                response.put("matchSource", "BARCODE_REGISTRY");
                response.put("confidence", 0.92);
                response.put("medicineName", mb.getMedicineName());
                response.put("batchNumber", mb.getBatchNumber());
                response.put("manufacturer", mb.getManufacturer());
                response.put("expiryDate", mb.getExpiryDate());
                response.put("batchRecord", mb);
                return response;
            }

            // Check existing Medicine entity stock table
            List<Medicine> matchedMeds = medicineRepository.findByBatchNumberContainingIgnoreCase(code.trim());
            if (!matchedMeds.isEmpty()) {
                Medicine m = matchedMeds.get(0);
                response.put("matched", true);
                response.put("matchSource", "INVENTORY_STOCK");
                response.put("confidence", 0.90);
                response.put("medicineName", m.getMedicineName());
                response.put("batchNumber", m.getBatchNumber());
                response.put("manufacturer", m.getManufacturer());
                response.put("expiryDate", m.getExpiryDate());
                return response;
            }
        }

        // 3. Try medicine name fuzzy lookup
        if (medicineName != null && !medicineName.isBlank()) {
            List<Medicine> matchedMeds = medicineRepository.findByMedicineNameContainingIgnoreCase(medicineName.trim());
            if (!matchedMeds.isEmpty()) {
                Medicine m = matchedMeds.get(0);
                response.put("matched", true);
                response.put("matchSource", "MEDICINE_CATALOG");
                response.put("confidence", 0.75);
                response.put("medicineName", m.getMedicineName());
                response.put("batchNumber", batchNumber != null ? batchNumber : m.getBatchNumber());
                response.put("manufacturer", m.getManufacturer());
                response.put("expiryDate", m.getExpiryDate());
                return response;
            }

            List<MedicineBatch> batchByName = medicineBatchRepository.findByMedicineNameContainingIgnoreCase(medicineName.trim());
            if (!batchByName.isEmpty()) {
                MedicineBatch mb = batchByName.get(0);
                response.put("matched", true);
                response.put("matchSource", "BATCH_NAME_CATALOG");
                response.put("confidence", 0.70);
                response.put("medicineName", mb.getMedicineName());
                response.put("batchNumber", batchNumber != null ? batchNumber : mb.getBatchNumber());
                response.put("manufacturer", mb.getManufacturer());
                response.put("expiryDate", mb.getExpiryDate());
                return response;
            }
        }

        // Unidentified / Low confidence fallback
        response.put("matched", false);
        response.put("matchSource", "NONE");
        response.put("confidence", 0.20);
        response.put("medicineName", medicineName != null && !medicineName.isBlank() ? medicineName : "Unknown Medicine");
        response.put("batchNumber", batchNumber != null ? batchNumber : "UNLISTED");
        response.put("manufacturer", manufacturer != null ? manufacturer : "Unverified Manufacturer");
        return response;
    }
}
