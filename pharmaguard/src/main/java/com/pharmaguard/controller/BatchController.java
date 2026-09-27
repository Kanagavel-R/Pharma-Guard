package com.pharmaguard.controller;

import com.pharmaguard.entity.Medicine;
import com.pharmaguard.entity.StockTransaction;
import com.pharmaguard.entity.VerificationHistory;
import com.pharmaguard.service.MedicineService;
import com.pharmaguard.service.RecallService;
import com.pharmaguard.service.StockTransactionService;
import com.pharmaguard.repository.VerificationHistoryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
public class BatchController {

    private final MedicineService medicineService;
    private final StockTransactionService stockTransactionService;
    private final VerificationHistoryRepository verificationRepository;
    private final RecallService recallService;

    public BatchController(
            MedicineService medicineService,
            StockTransactionService stockTransactionService,
            VerificationHistoryRepository verificationRepository,
            RecallService recallService) {
        this.medicineService = medicineService;
        this.stockTransactionService = stockTransactionService;
        this.verificationRepository = verificationRepository;
        this.recallService = recallService;
    }

    @GetMapping({"/medicines/batch/{batchNumber}", "/medicines/batches/{batchNumber}"})
    public String viewBatchDetails(@PathVariable String batchNumber, Principal principal, Model model) {
        String username = (principal != null) ? principal.getName() : "Admin";
        model.addAttribute("username", username);

        Optional<Medicine> medOpt = medicineService.findFirstByBatchNumber(batchNumber);
        if (medOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Batch '" + batchNumber + "' not found in database.");
            return "redirect:/medicines?error=Batch+not+found";
        }

        Medicine medicine = medOpt.get();
        model.addAttribute("medicine", medicine);

        List<StockTransaction> transactions = stockTransactionService.getTransactionsByMedicine(medicine.getId());
        model.addAttribute("transactions", transactions);

        boolean isRecalled = recallService.isBatchRecalled(batchNumber);
        model.addAttribute("isRecalled", isRecalled);

        return "batch-details";
    }
}
