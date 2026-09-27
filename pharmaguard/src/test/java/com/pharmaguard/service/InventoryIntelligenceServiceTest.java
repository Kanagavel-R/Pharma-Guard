package com.pharmaguard.service;

import com.pharmaguard.dto.MedicineIntelligenceDTO;
import com.pharmaguard.entity.Medicine;
import com.pharmaguard.entity.StockTransaction;
import com.pharmaguard.enums.TransactionType;
import com.pharmaguard.repository.MedicineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryIntelligenceServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private StockTransactionService stockTransactionService;

    @InjectMocks
    private InventoryIntelligenceService intelligenceService;

    private Medicine testMedicine;

    @BeforeEach
    void setUp() {
        testMedicine = new Medicine(
                "Paracetamol",
                "Analgesic",
                "BATCH-001",
                20,
                15.50,
                LocalDate.now().plusDays(10),
                15,
                "PharmaCorp"
        );
        testMedicine.setId(1L);
        testMedicine.setLeadTimeDays(7);
        testMedicine.setSafetyStock(10);
    }

    @Test
    void testScenario1_NoTransactionHistory() {
        when(stockTransactionService.getTotalStockOutQuantity(1L)).thenReturn(0L);

        MedicineIntelligenceDTO dto = intelligenceService.evaluateMedicineIntelligence(testMedicine);

        assertFalse(dto.isHasSufficientData());
        assertEquals(0.0, dto.getAverageDailyConsumption());
        assertNull(dto.getEstimatedStockOutDays());
    }

    @Test
    void testScenario2_NormalConsumption() {
        when(stockTransactionService.getTotalStockOutQuantity(1L)).thenReturn(60L);
        StockTransaction firstTx = new StockTransaction(testMedicine, TransactionType.STOCK_OUT, 10, LocalDateTime.now().minusDays(15), "Sales");
        when(stockTransactionService.getFirstTransaction(1L)).thenReturn(Optional.of(firstTx));

        MedicineIntelligenceDTO dto = intelligenceService.evaluateMedicineIntelligence(testMedicine);

        assertTrue(dto.isHasSufficientData());
        assertEquals(4.0, dto.getAverageDailyConsumption());
        assertEquals(5.0, dto.getEstimatedStockOutDays()); // 20 / 4 = 5 days
        assertEquals("STOCK-OUT RISK", dto.getStockOutStatus()); // < 7 days
    }

    @Test
    void testScenario3_LowStockAndReorderRecommendation() {
        when(stockTransactionService.getTotalStockOutQuantity(1L)).thenReturn(60L);
        StockTransaction firstTx = new StockTransaction(testMedicine, TransactionType.STOCK_OUT, 10, LocalDateTime.now().minusDays(15), "Sales");
        when(stockTransactionService.getFirstTransaction(1L)).thenReturn(Optional.of(firstTx));

        MedicineIntelligenceDTO dto = intelligenceService.evaluateMedicineIntelligence(testMedicine);

        // Daily = 4, LeadTime = 7, SafetyStock = 10 -> ReorderPoint = (4 * 7) + 10 = 38
        assertEquals(38, dto.getReorderPoint());
        assertTrue(dto.isReorderRecommended());
        assertTrue(dto.getRecommendedReorderQuantity() > 0);
        assertTrue(dto.getReorderExplanation().contains("below the calculated reorder point"));
    }

    @Test
    void testScenario4_ExpiryWastePrediction() {
        Medicine highStockMedicine = new Medicine("Amoxicillin", "Antibiotic", "BATCH-002", 100, 25.0, LocalDate.now().plusDays(12), 10, "MedSupply");
        highStockMedicine.setId(2L);

        when(stockTransactionService.getTotalStockOutQuantity(2L)).thenReturn(60L);
        StockTransaction firstTx = new StockTransaction(highStockMedicine, TransactionType.STOCK_OUT, 10, LocalDateTime.now().minusDays(12), "Sales");
        when(stockTransactionService.getFirstTransaction(2L)).thenReturn(Optional.of(firstTx));

        MedicineIntelligenceDTO dto = intelligenceService.evaluateMedicineIntelligence(highStockMedicine);

        // Daily consumption = 60 / 12 = 5 units/day. Days to expiry = 12. Expected consumption = 5 * 12 = 60 units.
        // Potential waste = 100 - 60 = 40 units!
        assertTrue(dto.isExpiryWasteRisk());
        assertEquals(40, dto.getPotentialExpiryWaste());
    }

    @Test
    void testScenario5_FEFORecommendation() {
        Medicine batchA = new Medicine("Paracetamol", "Analgesic", "BATCH-A", 50, 15.0, LocalDate.now().plusYears(2), 10, "PharmaCorp");
        batchA.setId(10L);

        Medicine batchB = new Medicine("Paracetamol", "Analgesic", "BATCH-B", 40, 15.0, LocalDate.now().plusYears(1), 10, "PharmaCorp");
        batchB.setId(11L);

        when(medicineRepository.findAll()).thenReturn(List.of(batchA, batchB));

        MedicineIntelligenceDTO dtoA = intelligenceService.evaluateMedicineIntelligence(batchA);

        assertNotNull(dtoA.getFefoRecommendedBatch());
        assertEquals("BATCH-B", dtoA.getFefoRecommendedBatch().getBatchNumber());
        assertTrue(dtoA.getFefoRecommendationMessage().contains("should be used before"));
    }
}
