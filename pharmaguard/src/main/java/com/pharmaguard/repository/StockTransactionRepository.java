package com.pharmaguard.repository;

import com.pharmaguard.entity.StockTransaction;
import com.pharmaguard.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    List<StockTransaction> findByMedicineIdOrderByTransactionDateDesc(Long medicineId);

    List<StockTransaction> findByMedicineIdAndTransactionTypeOrderByTransactionDateAsc(Long medicineId, TransactionType transactionType);

    List<StockTransaction> findAllByOrderByTransactionDateDesc();

    List<StockTransaction> findTop6ByOrderByTransactionDateDesc();

    long countByTransactionDateGreaterThanEqual(java.time.LocalDateTime startOfDay);

    @Query("SELECT COALESCE(SUM(st.quantity), 0) FROM StockTransaction st WHERE st.medicine.id = :medicineId AND st.transactionType = :type")
    long getTotalQuantityByMedicineAndType(@Param("medicineId") Long medicineId, @Param("type") TransactionType type);

    Optional<StockTransaction> findFirstByMedicineIdAndTransactionTypeOrderByTransactionDateAsc(Long medicineId, TransactionType transactionType);

    Optional<StockTransaction> findFirstByMedicineIdOrderByTransactionDateAsc(Long medicineId);
}
