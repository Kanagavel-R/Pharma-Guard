package com.pharmaguard.service;

import com.pharmaguard.entity.Medicine;
import com.pharmaguard.enums.TransactionType;
import com.pharmaguard.repository.MedicineRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final StockTransactionService stockTransactionService;
    private final AlertService alertService;

    public MedicineService(MedicineRepository medicineRepository,
                           StockTransactionService stockTransactionService,
                           @Lazy AlertService alertService) {
        this.medicineRepository = medicineRepository;
        this.stockTransactionService = stockTransactionService;
        this.alertService = alertService;
    }

    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll();
    }

    public Medicine getMedicineById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicine not found with ID: " + id));
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = {"categories", "suppliers"}, allEntries = true)
    public java.util.Optional<Medicine> findFirstByBatchNumber(String batchNumber) {
        if (batchNumber == null || batchNumber.isBlank()) return java.util.Optional.empty();
        return medicineRepository.findFirstByBatchNumberIgnoreCase(batchNumber.trim());
    }

    public Medicine saveMedicine(Medicine medicine) {
        if (medicine == null) {
            throw new IllegalArgumentException("Medicine payload cannot be null.");
        }

        if (medicine.getMedicineName() == null || medicine.getMedicineName().isBlank()) {
            throw new IllegalArgumentException("Medicine name is required.");
        }
        medicine.setMedicineName(medicine.getMedicineName().trim());

        if (medicine.getCategory() == null || medicine.getCategory().isBlank()) {
            throw new IllegalArgumentException("Category is required.");
        }
        medicine.setCategory(medicine.getCategory().trim());

        if (medicine.getBatchNumber() == null || medicine.getBatchNumber().isBlank()) {
            throw new IllegalArgumentException("Batch number is required.");
        }
        medicine.setBatchNumber(medicine.getBatchNumber().trim());

        if (medicine.getExpiryDate() == null) {
            throw new IllegalArgumentException("Expiry date is required.");
        }

        if (medicine.getPrice() <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0.");
        }

        if (medicine.getQuantity() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }

        if (isDuplicateBatch(medicine.getBatchNumber(), medicine.getId())) {
            throw new IllegalArgumentException("A medicine with batch number '" + medicine.getBatchNumber() + "' already exists.");
        }

        if (medicine.getStorageLocation() == null || medicine.getStorageLocation().isBlank()) {
            medicine.setStorageLocation("Main Rack");
        }
        if (medicine.getLeadTimeDays() <= 0) {
            medicine.setLeadTimeDays(7);
        }
        if (medicine.getSafetyStock() <= 0) {
            medicine.setSafetyStock(10);
        }
        if (medicine.getSupplierName() != null) {
            medicine.setSupplierName(medicine.getSupplierName().trim());
        }

        if (medicine.getId() == null) {
            Medicine saved = medicineRepository.save(medicine);
            if (saved.getQuantity() > 0) {
                stockTransactionService.recordTransaction(
                        saved,
                        TransactionType.STOCK_IN,
                        saved.getQuantity(),
                        "Initial stock addition"
                );
            }
            alertService.checkMedicineAlerts(saved);
            return saved;
        } else {
            Medicine existing = getMedicineById(medicine.getId());
            int oldQuantity = existing.getQuantity();
            int newQuantity = medicine.getQuantity();

            existing.setMedicineName(medicine.getMedicineName());
            existing.setCategory(medicine.getCategory());
            existing.setBatchNumber(medicine.getBatchNumber());
            existing.setQuantity(medicine.getQuantity());
            existing.setPrice(medicine.getPrice());
            existing.setExpiryDate(medicine.getExpiryDate());
            existing.setReorderLevel(medicine.getReorderLevel());
            existing.setSupplierName(medicine.getSupplierName());
            existing.setStorageLocation(medicine.getStorageLocation());
            existing.setLeadTimeDays(medicine.getLeadTimeDays());
            existing.setSafetyStock(medicine.getSafetyStock());
            if (medicine.getGenericName() != null) existing.setGenericName(medicine.getGenericName());
            if (medicine.getStrength() != null) existing.setStrength(medicine.getStrength());
            if (medicine.getDosageForm() != null) existing.setDosageForm(medicine.getDosageForm());
            if (medicine.getManufacturer() != null) existing.setManufacturer(medicine.getManufacturer());
            if (medicine.getBarcode() != null) existing.setBarcode(medicine.getBarcode());

            Medicine saved = medicineRepository.save(existing);

            if (newQuantity > oldQuantity) {
                stockTransactionService.recordTransaction(
                        saved,
                        TransactionType.STOCK_IN,
                        newQuantity - oldQuantity,
                        "Stock quantity increased via edit"
                );
            } else if (newQuantity < oldQuantity) {
                stockTransactionService.recordTransaction(
                        saved,
                        TransactionType.STOCK_OUT,
                        oldQuantity - newQuantity,
                        "Stock quantity decreased via edit"
                );
            }

            alertService.checkMedicineAlerts(saved);
            return saved;
        }
    }

    @Transactional
    public Medicine recordStockTransaction(Long medicineId, TransactionType transactionType, int quantity, String notes) {
        return recordStockTransaction(medicineId, transactionType, quantity, notes, "Admin");
    }

    @Transactional
    public Medicine recordStockTransaction(Long medicineId, TransactionType transactionType, int quantity, String notes, String username) {
        Medicine medicine = getMedicineById(medicineId);

        if (quantity <= 0) {
            throw new IllegalArgumentException("Transaction quantity must be greater than zero");
        }

        int previousStock = medicine.getQuantity();
        int newStock = previousStock;

        if (transactionType == TransactionType.STOCK_OUT) {
            if (medicine.getQuantity() < quantity) {
                throw new IllegalArgumentException("Insufficient stock available for stock out");
            }
            newStock = previousStock - quantity;
            medicine.setQuantity(newStock);
        } else if (transactionType == TransactionType.STOCK_IN) {
            newStock = previousStock + quantity;
            medicine.setQuantity(newStock);
        } else if (transactionType == TransactionType.ADJUSTMENT) {
            newStock = quantity;
            medicine.setQuantity(newStock);
        }

        Medicine updated = medicineRepository.save(medicine);
        stockTransactionService.recordTransaction(updated, transactionType, quantity, notes, previousStock, newStock, username);
        alertService.checkMedicineAlerts(updated);
        return updated;
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = {"categories", "suppliers"}, allEntries = true)
    public void deleteMedicine(Long id) {
        if (!medicineRepository.existsById(id)) {
            throw new RuntimeException("Medicine not found with ID: " + id);
        }
        alertService.deleteAlertsForMedicine(id);
        medicineRepository.deleteById(id);
    }

    public List<Medicine> searchMedicines(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return medicineRepository.findAll();
        }
        return medicineRepository.findByMedicineNameContainingIgnoreCase(keyword.trim());
    }

    public org.springframework.data.domain.Page<Medicine> getMedicinesPaginated(org.springframework.data.domain.Pageable pageable) {
        return medicineRepository.findAll(pageable);
    }

    public org.springframework.data.domain.Page<Medicine> searchMedicinesPaginated(String keyword, org.springframework.data.domain.Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return medicineRepository.findAll(pageable);
        }
        return medicineRepository.findByMedicineNameContainingIgnoreCase(keyword.trim(), pageable);
    }

    public List<Medicine> filterInventory(
            String keyword,
            String category,
            String stockStatus,
            String expiryStatus,
            String supplier,
            String sortBy) {

        List<Medicine> list = medicineRepository.findAll();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate d7 = today.plusDays(7);
        java.time.LocalDate d30 = today.plusDays(30);
        java.time.LocalDate d90 = today.plusDays(90);

        return list.stream()
                .filter(m -> {
                    if (keyword == null || keyword.trim().isEmpty()) return true;
                    String k = keyword.trim().toLowerCase();
                    boolean nameMatch = m.getMedicineName() != null && m.getMedicineName().toLowerCase().contains(k);
                    boolean genMatch = m.getGenericName() != null && m.getGenericName().toLowerCase().contains(k);
                    boolean mfgMatch = m.getManufacturer() != null && m.getManufacturer().toLowerCase().contains(k);
                    boolean batchMatch = m.getBatchNumber() != null && m.getBatchNumber().toLowerCase().contains(k);
                    boolean catMatch = m.getCategory() != null && m.getCategory().toLowerCase().contains(k);
                    boolean suppMatch = m.getSupplierName() != null && m.getSupplierName().toLowerCase().contains(k);
                    boolean locMatch = m.getStorageLocation() != null && m.getStorageLocation().toLowerCase().contains(k);
                    boolean barcodeMatch = m.getBarcode() != null && m.getBarcode().toLowerCase().contains(k);
                    return nameMatch || genMatch || mfgMatch || batchMatch || catMatch || suppMatch || locMatch || barcodeMatch;
                })
                .filter(m -> {
                    if (category == null || category.trim().isEmpty() || "All".equalsIgnoreCase(category)) return true;
                    return m.getCategory() != null && m.getCategory().equalsIgnoreCase(category.trim());
                })
                .filter(m -> {
                    if (stockStatus == null || stockStatus.trim().isEmpty() || "All".equalsIgnoreCase(stockStatus)) return true;
                    if ("Available".equalsIgnoreCase(stockStatus) || "Normal Stock".equalsIgnoreCase(stockStatus)) {
                        return m.getQuantity() > m.getReorderLevel();
                    } else if ("Low Stock".equalsIgnoreCase(stockStatus)) {
                        return m.getQuantity() <= m.getReorderLevel() && m.getQuantity() > 0;
                    } else if ("Out of Stock".equalsIgnoreCase(stockStatus)) {
                        return m.getQuantity() == 0;
                    }
                    return true;
                })
                .filter(m -> {
                    if (expiryStatus == null || expiryStatus.trim().isEmpty() || "All".equalsIgnoreCase(expiryStatus)) return true;
                    if (m.getExpiryDate() == null) return true;
                    if ("Expired".equalsIgnoreCase(expiryStatus)) {
                        return m.getExpiryDate().isBefore(today);
                    } else if ("7 Days".equalsIgnoreCase(expiryStatus) || "Expiring 7 Days".equalsIgnoreCase(expiryStatus)) {
                        return !m.getExpiryDate().isBefore(today) && !m.getExpiryDate().isAfter(d7);
                    } else if ("30 Days".equalsIgnoreCase(expiryStatus) || "Expiring Soon".equalsIgnoreCase(expiryStatus) || "Expiring 30 Days".equalsIgnoreCase(expiryStatus)) {
                        return !m.getExpiryDate().isBefore(today) && !m.getExpiryDate().isAfter(d30);
                    } else if ("90 Days".equalsIgnoreCase(expiryStatus) || "Expiring 90 Days".equalsIgnoreCase(expiryStatus)) {
                        return !m.getExpiryDate().isBefore(today) && !m.getExpiryDate().isAfter(d90);
                    } else if ("Safe".equalsIgnoreCase(expiryStatus)) {
                        return m.getExpiryDate().isAfter(d30);
                    }
                    return true;
                })
                .filter(m -> {
                    if (supplier == null || supplier.trim().isEmpty() || "All".equalsIgnoreCase(supplier)) return true;
                    return m.getSupplierName() != null && m.getSupplierName().equalsIgnoreCase(supplier.trim());
                })
                .sorted((m1, m2) -> {
                    if ("Quantity".equalsIgnoreCase(sortBy)) {
                        return Integer.compare(m2.getQuantity(), m1.getQuantity());
                    } else if ("Expiry Date".equalsIgnoreCase(sortBy)) {
                        if (m1.getExpiryDate() == null) return 1;
                        if (m2.getExpiryDate() == null) return -1;
                        return m1.getExpiryDate().compareTo(m2.getExpiryDate());
                    } else if ("Recently Added".equalsIgnoreCase(sortBy)) {
                        if (m1.getId() == null) return 1;
                        if (m2.getId() == null) return -1;
                        return Long.compare(m2.getId(), m1.getId());
                    } else {
                        // Default: Medicine Name
                        if (m1.getMedicineName() == null) return 1;
                        if (m2.getMedicineName() == null) return -1;
                        return m1.getMedicineName().compareToIgnoreCase(m2.getMedicineName());
                    }
                })
                .toList();
    }

    public long getTotalStock() {
        return medicineRepository.getTotalStock();
    }

    public long getTotalMedicines() {
        return medicineRepository.count();
    }

    public long getLowStockCount() {
        return medicineRepository.countLowStockMedicines();
    }

    public long getExpiringSoonCount() {
        java.time.LocalDate today = java.time.LocalDate.now();
        return medicineRepository.countExpiringSoonMedicines(today, today.plusDays(30));
    }

    public long getExpiredCount() {
        return medicineRepository.countExpiredMedicines(java.time.LocalDate.now());
    }

    public long getOutOfStockCount() {
        return medicineRepository.countOutOfStockMedicines();
    }

    @org.springframework.cache.annotation.Cacheable(value = "categories")
    public List<String> getDistinctCategories() {
        return medicineRepository.findDistinctCategories();
    }

    public List<String> getAllCategories() {
        return getDistinctCategories();
    }

    @org.springframework.cache.annotation.Cacheable(value = "suppliers")
    public List<String> getDistinctSuppliers() {
        return medicineRepository.findDistinctSuppliers();
    }

    public List<String> getAllSuppliers() {
        return getDistinctSuppliers();
    }

    public boolean isDuplicateBatch(String batchNumber, Long id) {
        return medicineRepository.countDuplicateBatch(batchNumber, id) > 0;
    }
}