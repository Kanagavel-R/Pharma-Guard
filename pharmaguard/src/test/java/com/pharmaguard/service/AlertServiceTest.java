package com.pharmaguard.service;

import com.pharmaguard.dto.MedicineIntelligenceDTO;
import com.pharmaguard.entity.Alert;
import com.pharmaguard.entity.Medicine;
import com.pharmaguard.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AlertServiceTest {

    private AlertRepository alertRepository;
    private InventoryIntelligenceService intelligenceService;
    private AlertService.MedicineRepositoryProvider medicineRepositoryProvider;
    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertRepository = mock(AlertRepository.class);
        intelligenceService = mock(InventoryIntelligenceService.class);
        medicineRepositoryProvider = mock(AlertService.MedicineRepositoryProvider.class);
        alertService = new AlertService(alertRepository, intelligenceService, medicineRepositoryProvider);
    }

    @Test
    void testCheckMedicineAlerts_LowStockAlertCreated() {
        Medicine med = new Medicine();
        med.setId(1L);
        med.setMedicineName("Amoxicillin");
        med.setBatchNumber("AMX-001");
        med.setQuantity(5);
        med.setReorderLevel(10);
        med.setExpiryDate(LocalDate.now().plusMonths(6));

        MedicineIntelligenceDTO intel = new MedicineIntelligenceDTO();
        intel.setStockOutStatus("SAFE");
        intel.setReorderRecommended(false);
        intel.setExpiryWasteRisk(false);

        when(intelligenceService.evaluateMedicineIntelligence(any())).thenReturn(intel);
        when(alertRepository.findFirstByMedicineIdAndAlertTypeAndResolvedFalse(1L, "LOW_STOCK")).thenReturn(Optional.empty());

        alertService.checkMedicineAlerts(med);

        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void testClearStaleAlerts_ResolvesWhenStockSufficient() {
        Medicine med = new Medicine();
        med.setId(1L);
        med.setMedicineName("Paracetamol");
        med.setQuantity(100);
        med.setReorderLevel(10);

        Alert lowStockAlert = new Alert();
        lowStockAlert.setId(10L);
        lowStockAlert.setAlertType("LOW_STOCK");
        lowStockAlert.setMedicineId(1L);
        lowStockAlert.setResolved(false);

        MedicineIntelligenceDTO intel = new MedicineIntelligenceDTO();
        intel.setStockOutStatus("SAFE");

        when(alertRepository.findByMedicineIdAndResolvedFalse(1L)).thenReturn(List.of(lowStockAlert));
        when(intelligenceService.evaluateMedicineIntelligence(any())).thenReturn(intel);

        alertService.clearStaleAlerts(med);

        assertTrue(lowStockAlert.isResolved());
        verify(alertRepository, times(1)).save(lowStockAlert);
    }
}
