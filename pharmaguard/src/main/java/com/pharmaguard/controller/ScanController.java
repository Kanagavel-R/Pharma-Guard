package com.pharmaguard.controller;

import com.pharmaguard.service.MedicineService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ScanController {

    private final MedicineService medicineService;

    public ScanController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @GetMapping("/scan")
    public String showScanPage(@RequestParam(value = "barcode", required = false) String barcode, Model model) {
        model.addAttribute("allMedicines", medicineService.getAllMedicines());
        model.addAttribute("initialBarcode", barcode);
        return "scan";
    }
}
