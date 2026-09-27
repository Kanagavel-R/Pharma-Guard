package com.pharmaguard.controller;

import com.pharmaguard.service.MedicineService;
import com.pharmaguard.service.StockTransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final StockTransactionService stockTransactionService;
    private final MedicineService medicineService;

    public OrderController(StockTransactionService stockTransactionService, MedicineService medicineService) {
        this.stockTransactionService = stockTransactionService;
        this.medicineService = medicineService;
    }

    @GetMapping
    public String listOrders(Model model) {
        model.addAttribute("transactions", stockTransactionService.getAllTransactions());
        model.addAttribute("medicines", medicineService.getAllMedicines());
        return "transactions";
    }
}
