package com.pharmaguard.repository;

import com.pharmaguard.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    // Search medicine by name
    List<Medicine> findByMedicineNameContainingIgnoreCase(String keyword);

    Page<Medicine> findByMedicineNameContainingIgnoreCase(String keyword, Pageable pageable);

    // Search medicine by batch number
    List<Medicine> findByBatchNumberContainingIgnoreCase(String batchNumber);
    java.util.Optional<Medicine> findFirstByBatchNumberIgnoreCase(String batchNumber);

    // Total stock quantity
    @Query("SELECT COALESCE(SUM(m.quantity), 0) FROM Medicine m")
    long getTotalStock();

    // Count low-stock medicines (quantity <= reorderLevel and quantity > 0)
    @Query("""
            SELECT COUNT(m)
            FROM Medicine m
            WHERE m.quantity <= m.reorderLevel
            AND m.quantity > 0
            """)
    long countLowStockMedicines();

    // Count expiring soon medicines (within 30 days and not expired)
    @Query("""
            SELECT COUNT(m)
            FROM Medicine m
            WHERE m.expiryDate >= :today
            AND m.expiryDate <= :targetDate
            """)
    long countExpiringSoonMedicines(@Param("today") java.time.LocalDate today, @Param("targetDate") java.time.LocalDate targetDate);

    // Count expired medicines
    @Query("""
            SELECT COUNT(m)
            FROM Medicine m
            WHERE m.expiryDate < :today
            """)
    long countExpiredMedicines(@Param("today") java.time.LocalDate today);

    // Count out of stock medicines
    @Query("""
            SELECT COUNT(m)
            FROM Medicine m
            WHERE m.quantity = 0
            """)
    long countOutOfStockMedicines();

    // Get distinct categories
    @Query("SELECT DISTINCT m.category FROM Medicine m WHERE m.category IS NOT NULL ORDER BY m.category ASC")
    List<String> findDistinctCategories();

    // Get distinct suppliers
    @Query("SELECT DISTINCT m.supplierName FROM Medicine m WHERE m.supplierName IS NOT NULL AND m.supplierName <> '' ORDER BY m.supplierName ASC")
    List<String> findDistinctSuppliers();

    // Check duplicate batch number
    // When editing, ignore the current medicine ID
    @Query("""
            SELECT COUNT(m)
            FROM Medicine m
            WHERE m.batchNumber = :batchNumber
            AND (:id IS NULL OR m.id <> :id)
            """)
    long countDuplicateBatch(
            @Param("batchNumber") String batchNumber,
            @Param("id") Long id
    );
}