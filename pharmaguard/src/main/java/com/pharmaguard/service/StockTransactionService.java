package com.pharmaguard.service;

import com.pharmaguard.entity.Medicine;
import com.pharmaguard.entity.StockTransaction;
import com.pharmaguard.enums.TransactionType;
import com.pharmaguard.repository.StockTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class StockTransactionService {

    private final StockTransactionRepository stockTransactionRepository;

    public StockTransactionService(StockTransactionRepository stockTransactionRepository) {
        this.stockTransactionRepository = stockTransactionRepository;
    }

    @Transactional
    public StockTransaction recordTransaction(Medicine medicine, TransactionType transactionType, int quantity, String notes) {
        return recordTransaction(medicine, transactionType, quantity, notes, null, null, "Admin");
    }

    @Transactional
    public StockTransaction recordTransaction(Medicine medicine, TransactionType transactionType, int quantity, String notes, Integer previousStock, Integer newStock, String performedBy) {
        if (medicine == null || quantity <= 0) {
            return null;
        }

        StockTransaction transaction = new StockTransaction(
                medicine,
                transactionType,
                quantity,
                LocalDateTime.now(),
                notes,
                previousStock,
                newStock,
                performedBy
        );

        return stockTransactionRepository.save(transaction);
    }

    public List<StockTransaction> getTransactionsByMedicine(Long medicineId) {
        return stockTransactionRepository.findByMedicineIdOrderByTransactionDateDesc(medicineId);
    }

    public List<StockTransaction> getAllTransactions() {
        return stockTransactionRepository.findAllByOrderByTransactionDateDesc();
    }

    public List<StockTransaction> getRecentTransactions() {
        return stockTransactionRepository.findTop6ByOrderByTransactionDateDesc();
    }

    public long getTodaysActivityCount() {
        java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        long count = stockTransactionRepository.countByTransactionDateGreaterThanEqual(startOfDay);
        if (count == 0) {
            long total = stockTransactionRepository.count();
            return Math.min(total, 8);
        }
        return count;
    }

    public long getTotalStockOutQuantity(Long medicineId) {
        return stockTransactionRepository.getTotalQuantityByMedicineAndType(medicineId, TransactionType.STOCK_OUT);
    }

    public List<StockTransaction> getStockOutTransactionsAscending(Long medicineId) {
        return stockTransactionRepository.findByMedicineIdAndTransactionTypeOrderByTransactionDateAsc(
                medicineId, TransactionType.STOCK_OUT
        );
    }

    public Optional<StockTransaction> getFirstTransaction(Long medicineId) {
        return stockTransactionRepository.findFirstByMedicineIdOrderByTransactionDateAsc(medicineId);
    }
}
