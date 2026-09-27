package com.pharmaguard.service;

import com.pharmaguard.entity.Medicine;
import com.pharmaguard.repository.MedicineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicineMonitoringEngineTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private AlertService alertService;

    @Mock
    private ExpiryAlertService expiryAlertService;

    @InjectMocks
    private MedicineMonitoringEngine monitoringEngine;

    private Medicine testMedicine;

    @BeforeEach
    void setUp() {
        testMedicine = new Medicine(
                "Amoxicillin 500mg",
                "Antibiotics",
                "AMX-2026-B1",
                5,
                15.50,
                LocalDate.now().plusDays(2),
                10,
                "PharmaCorp"
        );
        testMedicine.setId(101L);
    }

    @Test
    void testRunFullMonitoringCycle() {
        when(medicineRepository.findAll()).thenReturn(List.of(testMedicine));

        monitoringEngine.runFullMonitoringCycle();

        verify(alertService, times(1)).checkMedicineAlerts(testMedicine);
        verify(expiryAlertService, times(1)).evaluateMedicineExpiry(eq(testMedicine), eq(1L));
    }

    @Test
    void testMonitorSingleMedicine() {
        monitoringEngine.monitorSingleMedicine(testMedicine);

        verify(alertService, times(1)).checkMedicineAlerts(testMedicine);
        verify(expiryAlertService, times(1)).evaluateMedicineExpiry(eq(testMedicine), eq(1L));
    }
}
