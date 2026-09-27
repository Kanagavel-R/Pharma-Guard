package com.pharmaguard.service;

import com.pharmaguard.entity.Medicine;
import com.pharmaguard.entity.StockTransaction;
import com.pharmaguard.enums.TransactionType;
import com.pharmaguard.repository.StockTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockTransactionServiceTest {

    @Mock
    private StockTransactionRepository stockTransactionRepository;

    @InjectMocks
    private StockTransactionService stockTransactionService;

    private Medicine medicine;

    @BeforeEach
    void setUp() {
        medicine = new Medicine(
                "Paracetamol 500mg",
                "Analgesic",
                "PCM-001",
                50,
                5.0,
                LocalDate.now().plusMonths(6),
                10,
                "Apex Pharma"
        );
        medicine.setId(1L);
    }

    @Test
    void testRecordTransaction_Success() {
        when(stockTransactionRepository.save(any(StockTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StockTransaction tx = stockTransactionService.recordTransaction(
                medicine,
                TransactionType.STOCK_IN,
                20,
                "Restock addition",
                50,
                70,
                "Pharmacist"
        );

        assertNotNull(tx);
        assertEquals(TransactionType.STOCK_IN, tx.getTransactionType());
        assertEquals(20, tx.getQuantity());
        assertEquals("Pharmacist", tx.getPerformedBy());
        verify(stockTransactionRepository, times(1)).save(any(StockTransaction.class));
    }

    @Test
    void testRecordTransaction_NullMedicineOrInvalidQuantity() {
        StockTransaction tx1 = stockTransactionService.recordTransaction(null, TransactionType.STOCK_IN, 10, "Test");
        assertNull(tx1);

        StockTransaction tx2 = stockTransactionService.recordTransaction(medicine, TransactionType.STOCK_IN, 0, "Test");
        assertNull(tx2);

        verify(stockTransactionRepository, never()).save(any());
    }
}
