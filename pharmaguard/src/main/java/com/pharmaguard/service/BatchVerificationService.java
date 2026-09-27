package com.pharmaguard.service;

import com.pharmaguard.entity.MedicineBatch;
import com.pharmaguard.repository.MedicineBatchRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class BatchVerificationService {

    private final MedicineBatchRepository medicineBatchRepository;

    public BatchVerificationService(MedicineBatchRepository medicineBatchRepository) {
        this.medicineBatchRepository = medicineBatchRepository;
    }

    public Map<String, Object> verifyBatch(String batchNumber, String claimedMedicine, String claimedManufacturer, LocalDate claimedExpiry) {
        Map<String, Object> result = new HashMap<>();

        if (batchNumber == null || batchNumber.isBlank() || "UNLISTED".equalsIgnoreCase(batchNumber)) {
            result.put("status", "UNABLE_TO_VERIFY");
            result.put("matched", false);
            result.put("details", "Batch number missing or unlisted in central database.");
            return result;
        }

        Optional<MedicineBatch> batchOpt = medicineBatchRepository.findByBatchNumberIgnoreCase(batchNumber.trim());

        if (batchOpt.isEmpty()) {
            result.put("status", "UNABLE_TO_VERIFY");
            result.put("matched", false);
            result.put("details", "Batch number " + batchNumber + " could not be found in active database records.");
            return result;
        }

        MedicineBatch record = batchOpt.get();
        result.put("matched", true);
        result.put("batchRecord", record);

        boolean nameMatch = claimedMedicine == null || claimedMedicine.isBlank() ||
                record.getMedicineName().toLowerCase().contains(claimedMedicine.toLowerCase()) ||
                claimedMedicine.toLowerCase().contains(record.getMedicineName().toLowerCase());

        boolean mfrMatch = claimedManufacturer == null || claimedManufacturer.isBlank() ||
                record.getManufacturer().toLowerCase().contains(claimedManufacturer.toLowerCase()) ||
                claimedManufacturer.toLowerCase().contains(record.getManufacturer().toLowerCase());

        boolean expiryMatch = true;
        if (claimedExpiry != null && record.getExpiryDate() != null) {
            expiryMatch = claimedExpiry.getMonth() == record.getExpiryDate().getMonth() &&
                    claimedExpiry.getYear() == record.getExpiryDate().getYear();
        }

        if (nameMatch && mfrMatch && expiryMatch) {
            result.put("status", "MATCHED");
            result.put("details", "Batch information completely matches official manufacturer record.");
        } else {
            result.put("status", "MISMATCH");
            result.put("details", "Batch info found, but package details partially mismatch registered batch metadata.");
            result.put("nameMatch", nameMatch);
            result.put("mfrMatch", mfrMatch);
            result.put("expiryMatch", expiryMatch);
        }

        return result;
    }
}
