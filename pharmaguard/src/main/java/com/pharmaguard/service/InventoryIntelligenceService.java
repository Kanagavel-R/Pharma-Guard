package com.pharmaguard.service;

import com.pharmaguard.dto.DashboardIntelligenceDTO;
import com.pharmaguard.dto.MedicineIntelligenceDTO;
import com.pharmaguard.entity.Medicine;
import com.pharmaguard.entity.StockTransaction;
import com.pharmaguard.enums.TransactionType;
import com.pharmaguard.repository.MedicineRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryIntelligenceService {

    private final MedicineRepository medicineRepository;
    private final StockTransactionService stockTransactionService;

    public InventoryIntelligenceService(MedicineRepository medicineRepository, StockTransactionService stockTransactionService) {
        this.medicineRepository = medicineRepository;
        this.stockTransactionService = stockTransactionService;
    }

    /**
     * FEATURE 2: Calculate Average Daily Consumption based on STOCK_OUT history.
     */
    public double calculateAverageDailyConsumption(Long medicineId) {
        long totalConsumed = stockTransactionService.getTotalStockOutQuantity(medicineId);
        if (totalConsumed <= 0) {
            return 0.0;
        }

        Optional<StockTransaction> firstTx = stockTransactionService.getFirstTransaction(medicineId);
        if (firstTx.isEmpty()) {
            return 0.0;
        }

        LocalDate startDate = firstTx.get().getTransactionDate().toLocalDate();
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(startDate, today);
        if (days < 1) {
            days = 1; // Minimum period of 1 day to prevent division by zero
        }

        double rawAvg = (double) totalConsumed / days;
        return Math.round(rawAvg * 100.0) / 100.0;
    }

    /**
     * Full Intelligence Evaluation for a single Medicine.
     */
    public MedicineIntelligenceDTO evaluateMedicineIntelligence(Medicine medicine) {
        MedicineIntelligenceDTO dto = new MedicineIntelligenceDTO();
        dto.setMedicine(medicine);

        if (medicine == null || medicine.getId() == null) {
            return dto;
        }

        double dailyConsumption = calculateAverageDailyConsumption(medicine.getId());
        dto.setAverageDailyConsumption(dailyConsumption);
        boolean hasData = dailyConsumption > 0;
        dto.setHasSufficientData(hasData);

        int currentStock = medicine.getQuantity();

        // ----------------------------------------------------
        // FEATURE 3: STOCK-OUT PREDICTION
        // ----------------------------------------------------
        if (!hasData) {
            dto.setEstimatedStockOutDays(null);
            if (currentStock <= medicine.getReorderLevel()) {
                dto.setStockOutStatus("REORDER SOON");
                dto.setStockOutBadgeClass("warning");
            } else {
                dto.setStockOutStatus("INSUFFICIENT DATA");
                dto.setStockOutBadgeClass("secondary");
            }
        } else {
            double daysToStockOut = (double) currentStock / dailyConsumption;
            daysToStockOut = Math.round(daysToStockOut * 10.0) / 10.0;
            dto.setEstimatedStockOutDays(daysToStockOut);

            if (currentStock == 0 || daysToStockOut < 7) {
                dto.setStockOutStatus("STOCK-OUT RISK");
                dto.setStockOutBadgeClass("danger");
            } else if (daysToStockOut <= 14) {
                dto.setStockOutStatus("REORDER SOON");
                dto.setStockOutBadgeClass("warning");
            } else if (daysToStockOut <= 30) {
                dto.setStockOutStatus("MONITOR");
                dto.setStockOutBadgeClass("monitor");
            } else {
                dto.setStockOutStatus("SAFE");
                dto.setStockOutBadgeClass("safe");
            }
        }

        // ----------------------------------------------------
        // FEATURE 4: SMART REORDER RECOMMENDATION
        // ----------------------------------------------------
        int leadTime = medicine.getLeadTimeDays() > 0 ? medicine.getLeadTimeDays() : 7;
        int safetyStock = medicine.getSafetyStock() >= 0 ? medicine.getSafetyStock() : 10;

        int reorderPoint;
        if (hasData) {
            reorderPoint = (int) Math.ceil(dailyConsumption * leadTime) + safetyStock;
        } else {
            reorderPoint = medicine.getReorderLevel();
        }
        dto.setReorderPoint(reorderPoint);

        if (currentStock <= reorderPoint) {
            dto.setReorderRecommended(true);
            int targetStock = hasData ? (int) Math.ceil(dailyConsumption * (leadTime + 14)) + safetyStock : (reorderPoint * 2 + 10);
            int recommendedQty = Math.max(1, targetStock - currentStock);
            dto.setRecommendedReorderQuantity(recommendedQty);
            dto.setReorderExplanation("Reorder recommended because current stock (" + currentStock + ") is below the calculated reorder point (" + reorderPoint + ").");
        } else {
            dto.setReorderRecommended(false);
            dto.setRecommendedReorderQuantity(0);
            dto.setReorderExplanation("Stock level (" + currentStock + ") is currently safe above reorder point (" + reorderPoint + ").");
        }

        // ----------------------------------------------------
        // FEATURE 5: EXPIRY WASTE PREDICTION
        // ----------------------------------------------------
        LocalDate expiryDate = medicine.getExpiryDate();
        LocalDate today = LocalDate.now();

        if (expiryDate != null) {
            long daysToExpiry = ChronoUnit.DAYS.between(today, expiryDate);
            dto.setDaysUntilExpiry(daysToExpiry);

            if (daysToExpiry <= 0) {
                // Expired
                dto.setExpectedConsumptionBeforeExpiry(0.0);
                dto.setPotentialExpiryWaste(currentStock);
                dto.setExpiryWasteRisk(currentStock > 0);
                dto.setExpiryWasteExplanation("Medicine is expired! " + currentStock + " units are unusable.");
            } else if (hasData) {
                double expectedConsumption = dailyConsumption * daysToExpiry;
                expectedConsumption = Math.round(expectedConsumption * 10.0) / 10.0;
                dto.setExpectedConsumptionBeforeExpiry(expectedConsumption);

                int excess = currentStock - (int) Math.floor(expectedConsumption);
                if (excess > 0) {
                    dto.setPotentialExpiryWaste(excess);
                    dto.setExpiryWasteRisk(true);
                    dto.setExpiryWasteExplanation(excess + " units may remain unused before expiry on " + expiryDate + " based on recent average consumption rate (" + dailyConsumption + " units/day).");
                } else {
                    dto.setPotentialExpiryWaste(0);
                    dto.setExpiryWasteRisk(false);
                    dto.setExpiryWasteExplanation("Expected consumption (" + expectedConsumption + " units) exceeds current stock (" + currentStock + " units). Zero waste projected.");
                }
            } else {
                dto.setExpectedConsumptionBeforeExpiry(null);
                dto.setPotentialExpiryWaste(0);
                dto.setExpiryWasteRisk(false);
                dto.setExpiryWasteExplanation("Insufficient usage data to forecast expiry waste.");
            }
        } else {
            dto.setDaysUntilExpiry(null);
            dto.setExpectedConsumptionBeforeExpiry(null);
            dto.setPotentialExpiryWaste(0);
            dto.setExpiryWasteRisk(false);
            dto.setExpiryWasteExplanation("No expiry date configured.");
        }

        // ----------------------------------------------------
        // FEATURE 6: FEFO RECOMMENDATION
        // ----------------------------------------------------
        List<Medicine> sameMedicines = medicineRepository.findAll().stream()
                .filter(m -> m.getMedicineName().equalsIgnoreCase(medicine.getMedicineName()))
                .filter(m -> !m.getId().equals(medicine.getId()))
                .filter(m -> m.getQuantity() > 0)
                .filter(m -> m.getExpiryDate() != null)
                .sorted((m1, m2) -> m1.getExpiryDate().compareTo(m2.getExpiryDate()))
                .toList();

        if (!sameMedicines.isEmpty() && expiryDate != null) {
            Medicine earliestOtherBatch = sameMedicines.get(0);
            if (earliestOtherBatch.getExpiryDate().isBefore(expiryDate)) {
                dto.setFefoRecommendedBatch(earliestOtherBatch);
                dto.setFefoRecommendationMessage("FEFO Warning: Batch " + earliestOtherBatch.getBatchNumber() + " (expires " + earliestOtherBatch.getExpiryDate() + ") should be used before Batch " + medicine.getBatchNumber() + " (expires " + expiryDate + ").");
            } else {
                dto.setFefoRecommendedBatch(null);
                dto.setFefoRecommendationMessage("FEFO Optimal: Batch " + medicine.getBatchNumber() + " has the earliest expiry among active batches for " + medicine.getMedicineName() + ".");
            }
        } else {
            dto.setFefoRecommendedBatch(null);
            dto.setFefoRecommendationMessage("FEFO OK: No earlier active batches found for this medicine.");
        }

        // Summary Text
        if ("STOCK_OUT_RISK".equals(dto.getStockOutStatus())) {
            dto.setSummaryStatusText("Critical stock-out risk. Reorder urgently.");
        } else if (dto.isReorderRecommended()) {
            dto.setSummaryStatusText("Reorder recommended (" + dto.getRecommendedReorderQuantity() + " units).");
        } else if (dto.isExpiryWasteRisk()) {
            dto.setSummaryStatusText("Potential waste risk (" + dto.getPotentialExpiryWaste() + " units).");
        } else {
            dto.setSummaryStatusText("Inventory status is safe.");
        }

        return dto;
    }

    /**
     * Dashboard Intelligence Summary Aggregator.
     */
    public DashboardIntelligenceDTO getDashboardIntelligence() {
        List<Medicine> allMedicines = medicineRepository.findAll();
        DashboardIntelligenceDTO dashboard = new DashboardIntelligenceDTO();

        dashboard.setTotalMedicines(allMedicines.size());
        dashboard.setTotalStock(allMedicines.stream().mapToLong(Medicine::getQuantity).sum());
        dashboard.setLowStockCount(allMedicines.stream().filter(m -> m.getQuantity() <= m.getReorderLevel()).count());

        long stockOutRiskCount = 0;
        long reorderRecommendedCount = 0;
        long expiryWasteRiskCount = 0;
        long fefoCount = 0;

        for (Medicine medicine : allMedicines) {
            MedicineIntelligenceDTO intel = evaluateMedicineIntelligence(medicine);

            if ("STOCK_OUT_RISK".equals(intel.getStockOutStatus()) || medicine.getQuantity() == 0) {
                stockOutRiskCount++;
                dashboard.addSmartRecommendation(medicine.getMedicineName() + " (Batch " + medicine.getBatchNumber() + "): Critical stock-out risk! " +
                        (intel.getEstimatedStockOutDays() != null ? "Estimated stock-out in " + intel.getEstimatedStockOutDays() + " days." : "Stock is zero."));
            }

            if (intel.isReorderRecommended()) {
                reorderRecommendedCount++;
                dashboard.addSmartRecommendation(medicine.getMedicineName() + ": Reorder recommended (" + intel.getRecommendedReorderQuantity() + " units) because stock (" + medicine.getQuantity() + ") is below reorder point (" + intel.getReorderPoint() + ").");
            }

            if (intel.isExpiryWasteRisk()) {
                expiryWasteRiskCount++;
                dashboard.addSmartRecommendation(medicine.getMedicineName() + " (Batch " + medicine.getBatchNumber() + "): " + intel.getPotentialExpiryWaste() + " units may expire unused before " + medicine.getExpiryDate() + " based on daily consumption rate.");
            }

            if (intel.getFefoRecommendedBatch() != null) {
                fefoCount++;
                dashboard.addSmartRecommendation(medicine.getMedicineName() + ": Batch " + intel.getFefoRecommendedBatch().getBatchNumber() + " (expires " + intel.getFefoRecommendedBatch().getExpiryDate() + ") should be dispensed before Batch " + medicine.getBatchNumber() + ".");
            }
        }

        dashboard.setStockOutRiskCount(stockOutRiskCount);
        dashboard.setReorderRecommendedCount(reorderRecommendedCount);
        dashboard.setExpiryWasteRiskCount(expiryWasteRiskCount);
        dashboard.setFefoRecommendationCount(fefoCount);

        return dashboard;
    }
}
