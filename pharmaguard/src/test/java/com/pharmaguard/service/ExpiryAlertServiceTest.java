package com.pharmaguard.service;

import com.pharmaguard.entity.ExpiryNotification;
import com.pharmaguard.entity.Medicine;
import com.pharmaguard.repository.ExpiryNotificationRepository;
import com.pharmaguard.repository.MedicineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ExpiryAlertServiceTest {

    private MedicineRepository medicineRepository;
    private ExpiryNotificationRepository notificationRepository;
    private EmailNotificationService emailNotificationService;
    private ExpiryAlertService expiryAlertService;

    @BeforeEach
    public void setUp() {
        medicineRepository = Mockito.mock(MedicineRepository.class);
        notificationRepository = Mockito.mock(ExpiryNotificationRepository.class);
        emailNotificationService = Mockito.mock(EmailNotificationService.class);

        expiryAlertService = new ExpiryAlertService(
                medicineRepository,
                notificationRepository,
                emailNotificationService
        );
    }

    @Test
    public void testEvaluateMedicineExpiryExpired() {
        Medicine expiredMed = new Medicine();
        expiredMed.setId(101L);
        expiredMed.setMedicineName("Expired Amoxicillin");
        expiredMed.setBatchNumber("AMX-EXP-1");
        expiredMed.setExpiryDate(LocalDate.now().minusDays(5));

        when(notificationRepository.existsByAlertKey(anyString())).thenReturn(false);

        expiryAlertService.evaluateMedicineExpiry(expiredMed, 1L);

        verify(notificationRepository, times(1)).save(any(ExpiryNotification.class));
    }

    @Test
    public void testEvaluateMedicineExpirySoon() {
        Medicine expiringSoonMed = new Medicine();
        expiringSoonMed.setId(102L);
        expiringSoonMed.setMedicineName("Paracetamol 500mg");
        expiringSoonMed.setBatchNumber("PCM-SOON-1");
        expiringSoonMed.setExpiryDate(LocalDate.now().plusDays(3));

        when(notificationRepository.existsByAlertKey(anyString())).thenReturn(false);

        expiryAlertService.evaluateMedicineExpiry(expiringSoonMed, 1L);

        verify(notificationRepository, times(1)).save(any(ExpiryNotification.class));
    }
}
