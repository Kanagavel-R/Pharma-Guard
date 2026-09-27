package com.pharmaguard.service;

import com.pharmaguard.entity.Medicine;
import com.pharmaguard.entity.StockTransaction;
import com.pharmaguard.enums.TransactionType;
import com.pharmaguard.repository.StockTransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DemandForecastingService {

    private final StockTransactionRepository transactionRepository;

    public DemandForecastingService(StockTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Map<String, Object> forecast30DayDemand(Medicine medicine) {
        Map<String, Object> result = new HashMap<>();
        if (medicine == null || medicine.getId() == null) {
            result.put("sufficientData", false);
            result.put("message", "Insufficient historical data for reliable forecasting.");
            return result;
        }

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<StockTransaction> transactions = transactionRepository.findByMedicineIdOrderByTransactionDateDesc(medicine.getId());

        if (transactions.isEmpty()) {
            result.put("sufficientData", false);
            result.put("message", "Insufficient historical transaction data for reliable forecasting.");
            result.put("estimatedDemand", 0);
            result.put("suggestedReorder", Math.max(0, medicine.getReorderLevel() - medicine.getQuantity()));
            return result;
        }

        int totalSales30Days = 0;
        int transactionCount = 0;

        for (StockTransaction tx : transactions) {
            if (tx.getTransactionDate() != null && tx.getTransactionDate().isAfter(thirtyDaysAgo)) {
                if (tx.getTransactionType() == TransactionType.STOCK_OUT || tx.getTransactionType() == TransactionType.SALE) {
                    totalSales30Days += tx.getQuantity();
                    transactionCount++;
                }
            }
        }

        if (transactionCount == 0 && transactions.size() < 3) {
            result.put("sufficientData", false);
            result.put("message", "At least 30 days of historical sales data is recommended for statistical forecasting.");
            result.put("estimatedDemand", 0);
            result.put("suggestedReorder", Math.max(0, medicine.getReorderLevel() - medicine.getQuantity()));
            return result;
        }

        int forecastedDemand = Math.max(totalSales30Days, 10);
        int safetyStock = medicine.getSafetyStock() > 0 ? medicine.getSafetyStock() : 10;
        int currentStock = medicine.getQuantity();
        int suggestedReorder = Math.max(0, (forecastedDemand + safetyStock) - currentStock);

        result.put("sufficientData", true);
        result.put("message", "Forecast calculated from 30-day historical consumption velocity.");
        result.put("estimatedDemand", forecastedDemand);
        result.put("currentStock", currentStock);
        result.put("safetyStock", safetyStock);
        result.put("suggestedReorder", suggestedReorder);

        return result;
    }
}
