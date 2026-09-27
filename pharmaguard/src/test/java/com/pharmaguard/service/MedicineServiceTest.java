package com.pharmaguard.service;

import com.pharmaguard.entity.Medicine;
import com.pharmaguard.repository.MedicineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class MedicineServiceTest {

    private MedicineRepository medicineRepository;
    private StockTransactionService stockTransactionService;
    private AlertService alertService;
    private MedicineService medicineService;

    @BeforeEach
    public void setUp() {
        medicineRepository = Mockito.mock(MedicineRepository.class);
        stockTransactionService = Mockito.mock(StockTransactionService.class);
        alertService = Mockito.mock(AlertService.class);

        medicineService = new MedicineService(medicineRepository, stockTransactionService, alertService);
    }

    @Test
    public void testSaveMedicineSuccess() {
        Medicine sample = new Medicine();
        sample.setMedicineName("Ibuprofen 400mg");
        sample.setBatchNumber("IBU-2026-X");
        sample.setQuantity(100);
        sample.setExpiryDate(LocalDate.now().plusMonths(12));

        when(medicineRepository.countDuplicateBatch("IBU-2026-X", null)).thenReturn(0L);
        when(medicineRepository.save(any(Medicine.class))).thenReturn(sample);

        Medicine saved = medicineService.saveMedicine(sample);
        assertNotNull(saved);
        assertEquals("Ibuprofen 400mg", saved.getMedicineName());
    }

    @Test
    public void testDuplicateBatchThrowsException() {
        Medicine duplicate = new Medicine();
        duplicate.setMedicineName("Paracetamol 500mg");
        duplicate.setBatchNumber("PCM-DUP-1");

        when(medicineRepository.countDuplicateBatch("PCM-DUP-1", null)).thenReturn(1L);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            medicineService.saveMedicine(duplicate);
        });

        assertTrue(exception.getMessage().contains("already exists"));
    }
}
