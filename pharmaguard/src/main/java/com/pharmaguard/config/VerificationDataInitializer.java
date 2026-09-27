package com.pharmaguard.config;

import com.pharmaguard.entity.MedicineBatch;
import com.pharmaguard.repository.MedicineBatchRepository;
import com.pharmaguard.service.VerificationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class VerificationDataInitializer implements CommandLineRunner {

    private final MedicineBatchRepository batchRepository;
    private final VerificationService verificationService;

    public VerificationDataInitializer(MedicineBatchRepository batchRepository, VerificationService verificationService) {
        this.batchRepository = batchRepository;
        this.verificationService = verificationService;
    }

    @Override
    public void run(String... args) {
        if (batchRepository.count() == 0) {
            // Seed Demo Central Manufacturer Batches
            MedicineBatch b1 = new MedicineBatch("PCM20260812", "Paracetamol 500mg", "ABC Pharmaceuticals Ltd.", LocalDate.of(2025, 8, 12), LocalDate.of(2028, 7, 31), "8901234567890");
            b1.setGenericName("Paracetamol / Acetaminophen");
            b1.setStrength("500 mg");
            b1.setDosageForm("Tablet");
            b1.setGtin("08901234567890");
            b1.setLicenseNumber("MFG-2024-99881");
            batchRepository.save(b1);

            MedicineBatch b2 = new MedicineBatch("AMX-2026-01", "Amoxicillin 500mg", "Apex Global Pharma", LocalDate.of(2025, 1, 15), LocalDate.of(2027, 12, 31), "8909876543210");
            b2.setGenericName("Amoxicillin Trihydrate");
            b2.setStrength("500 mg");
            b2.setDosageForm("Capsule");
            b2.setGtin("08909876543210");
            b2.setLicenseNumber("MFG-2023-44112");
            batchRepository.save(b2);

            MedicineBatch b3 = new MedicineBatch("MET-8890", "Metformin HCl 850mg", "Sun Health Sciences", LocalDate.of(2024, 6, 10), LocalDate.of(2026, 5, 31), "8905544332211");
            b3.setGenericName("Metformin Hydrochloride");
            b3.setStrength("850 mg");
            b3.setDosageForm("Extended Release Tablet");
            b3.setGtin("08905544332211");
            b3.setLicenseNumber("MFG-2022-10045");
            batchRepository.save(b3);

            MedicineBatch b4 = new MedicineBatch("ATV-2023-X", "Atorvastatin 20mg", "Bayer BioCare", LocalDate.of(2023, 2, 1), LocalDate.of(2025, 1, 31), "8901122334455");
            b4.setGenericName("Atorvastatin Calcium");
            b4.setStrength("20 mg");
            b4.setDosageForm("Film-Coated Tablet");
            b4.setGtin("08901122334455");
            b4.setLicenseNumber("MFG-2021-00998");
            batchRepository.save(b4);

            System.out.println("Pharma Guard: Seeded 4 central development batch records.");
        }

        if (batchRepository.findByBatchNumberIgnoreCase("PG240501").isEmpty()) {
            MedicineBatch bTest = new MedicineBatch("PG240501", "P-500", "Pharma Guard Certified Labs", LocalDate.of(2024, 5, 1), LocalDate.of(2028, 12, 31), "PGH-PRC-2024-000123");
            bTest.setGenericName("Paracetamol P-500");
            bTest.setStrength("500 mg");
            bTest.setDosageForm("Tablet");
            bTest.setGtin("PGH-PRC-2024-000123");
            bTest.setLicenseNumber("MFG-PG-2024-001");
            batchRepository.save(bTest);
            System.out.println("Pharma Guard: Seeded test QR record P-500 / PG240501 / PGH-PRC-2024-000123.");
        }
    }
}
