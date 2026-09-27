package com.pharmaguard.controller;

import com.pharmaguard.entity.Medicine;
import com.pharmaguard.enums.TransactionType;
import com.pharmaguard.service.AlertService;
import com.pharmaguard.service.MedicineService;
import com.pharmaguard.service.StockTransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/transactions")
public class StockTransactionController {

    private final StockTransactionService stockTransactionService;
    private final MedicineService medicineService;
    private final AlertService alertService;

    public StockTransactionController(StockTransactionService stockTransactionService, MedicineService medicineService, AlertService alertService) {
        this.stockTransactionService = stockTransactionService;
        this.medicineService = medicineService;
        this.alertService = alertService;
    }

    @GetMapping
    public String listTransactions(Model model) {
        model.addAttribute("transactions", stockTransactionService.getAllTransactions());
        return "transactions";
    }

    @PostMapping("/record")
    public String recordTransaction(
            @RequestParam("medicineId") Long medicineId,
            @RequestParam("transactionType") String typeStr,
            @RequestParam("quantity") int quantity,
            @RequestParam(value = "notes", required = false) String notes,
            Model model) {

        try {
            TransactionType type = TransactionType.valueOf(typeStr);
            Medicine updatedMedicine = medicineService.recordStockTransaction(medicineId, type, quantity, notes);
            alertService.checkMedicineAlerts(updatedMedicine);
            return "redirect:/medicines/details/" + medicineId + "?success=Stock+transaction+recorded+successfully";
        } catch (Exception e) {
            return "redirect:/medicines/details/" + medicineId + "?error=" + e.getMessage();
        }
    }
}
