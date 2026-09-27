package com.pharmaguard.controller;

import com.pharmaguard.dto.MedicineIntelligenceDTO;
import com.pharmaguard.entity.Medicine;
import com.pharmaguard.service.AlertService;
import com.pharmaguard.service.InventoryIntelligenceService;
import com.pharmaguard.service.MedicineService;
import com.pharmaguard.service.StockTransactionService;

import jakarta.validation.Valid;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/medicines")
public class MedicineController {

    private final MedicineService medicineService;
    private final AlertService alertService;
    private final InventoryIntelligenceService intelligenceService;
    private final StockTransactionService stockTransactionService;
    private final com.pharmaguard.service.ExpiryAlertService expiryAlertService;
    private final com.pharmaguard.service.AuditLogService auditLogService;

    public MedicineController(
            MedicineService medicineService,
            AlertService alertService,
            InventoryIntelligenceService intelligenceService,
            StockTransactionService stockTransactionService,
            com.pharmaguard.service.ExpiryAlertService expiryAlertService,
            com.pharmaguard.service.AuditLogService auditLogService) {

        this.medicineService = medicineService;
        this.alertService = alertService;
        this.intelligenceService = intelligenceService;
        this.stockTransactionService = stockTransactionService;
        this.expiryAlertService = expiryAlertService;
        this.auditLogService = auditLogService;
    }


    // ==========================================
    // MEDICINES LIST
    // ==========================================

    @GetMapping
    public String medicines(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "supplier", required = false) String supplier,
            @RequestParam(value = "expiryStatus", required = false) String expiryStatus,
            @RequestParam(value = "stockStatus", required = false) String stockStatus,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            Model model) {

        List<Medicine> filtered = medicineService.filterInventory(keyword, category, stockStatus, expiryStatus, supplier, sortBy);
        model.addAttribute("medicines", filtered);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedSupplier", supplier);
        model.addAttribute("selectedExpiryStatus", expiryStatus);
        model.addAttribute("selectedStockStatus", stockStatus);
        model.addAttribute("categories", medicineService.getAllCategories());
        model.addAttribute("suppliers", medicineService.getAllSuppliers());

        return "medicines";
    }


    // ==========================================
    // MEDICINE INTELLIGENCE DETAILS
    // ==========================================

    @GetMapping("/details/{id}")
    public String viewMedicineDetails(
            @PathVariable Long id,
            Model model) {

        try {
            Medicine medicine = medicineService.getMedicineById(id);
            MedicineIntelligenceDTO intel = intelligenceService.evaluateMedicineIntelligence(medicine);

            model.addAttribute("medicine", medicine);
            model.addAttribute("intelligence", intel);
            model.addAttribute("transactions", stockTransactionService.getTransactionsByMedicine(id));

            return "medicine-details";
        } catch (RuntimeException e) {
            return "redirect:/medicines?error=Medicine+not+found";
        }
    }


    // ==========================================
    // ADD MEDICINE FORM
    // ==========================================

    @GetMapping("/add")
    public String addMedicine(Model model) {

        model.addAttribute(
                "medicine",
                new Medicine()
        );

        return "medicine-form";
    }


    // ==========================================
    // EDIT MEDICINE FORM
    // ==========================================

    @GetMapping("/edit/{id}")
    public String editMedicine(
            @PathVariable Long id,
            Model model) {

        try {

            Medicine medicine =
                    medicineService.getMedicineById(id);

            model.addAttribute(
                    "medicine",
                    medicine
            );

            return "medicine-form";

        } catch (RuntimeException e) {

            return "redirect:/medicines?error=Medicine+not+found";
        }
    }


    // ==========================================
    // SAVE MEDICINE
    // ==========================================

    @PostMapping("/save")
    public String saveMedicine(
            @Valid @ModelAttribute("medicine") Medicine medicine,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", "Please fix the highlighted validation errors.");
            return "medicine-form";
        }

        if (medicineService.isDuplicateBatch(medicine.getBatchNumber(), medicine.getId())) {
            bindingResult.rejectValue(
                    "batchNumber",
                    "duplicate",
                    "A medicine with batch number '" + medicine.getBatchNumber() + "' already exists."
            );
            model.addAttribute("errorMessage", "A medicine with batch number '" + medicine.getBatchNumber() + "' already exists.");
            return "medicine-form";
        }

        try {
            Medicine savedMedicine = medicineService.saveMedicine(medicine);
            alertService.checkMedicineAlerts(savedMedicine);
            expiryAlertService.evaluateMedicineExpiry(savedMedicine, 1L);
            auditLogService.logAction("Admin", medicine.getId() == null ? "ADD_MEDICINE" : "EDIT_MEDICINE", "Medicine", String.valueOf(savedMedicine.getId()), "Saved medicine: " + savedMedicine.getMedicineName() + " (Batch: " + savedMedicine.getBatchNumber() + ")");

            return "redirect:/medicines?success=Medicine+saved+successfully";

        } catch (Exception e) {
            model.addAttribute(
                    "errorMessage",
                    "Unable to save medicine: " + e.getMessage()
            );
            return "medicine-form";
        }
    }

    // ==========================================
    // REST API ENDPOINTS FOR AJAX SAVE / EDIT
    // ==========================================

    @GetMapping("/api/details/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<Medicine> getMedicineApi(@PathVariable Long id) {
        try {
            Medicine medicine = medicineService.getMedicineById(id);
            return org.springframework.http.ResponseEntity.ok(medicine);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
    }

    @PostMapping({"/api/save", "/api"})
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> saveMedicineApi(
            @RequestBody Medicine medicine) {

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            if (medicine.getMedicineName() == null || medicine.getMedicineName().isBlank()) {
                response.put("success", false);
                response.put("message", "Medicine name is required.");
                return org.springframework.http.ResponseEntity.badRequest().body(response);
            }

            if (medicine.getBatchNumber() == null || medicine.getBatchNumber().isBlank()) {
                response.put("success", false);
                response.put("message", "Batch number is required.");
                return org.springframework.http.ResponseEntity.badRequest().body(response);
            }

            if (medicine.getCategory() == null || medicine.getCategory().isBlank()) {
                response.put("success", false);
                response.put("message", "Category is required.");
                return org.springframework.http.ResponseEntity.badRequest().body(response);
            }

            if (medicine.getExpiryDate() == null) {
                response.put("success", false);
                response.put("message", "Expiry date is required.");
                return org.springframework.http.ResponseEntity.badRequest().body(response);
            }

            if (medicine.getPrice() <= 0) {
                response.put("success", false);
                response.put("message", "Price must be a valid number greater than 0.");
                return org.springframework.http.ResponseEntity.badRequest().body(response);
            }

            if (medicine.getQuantity() < 0) {
                response.put("success", false);
                response.put("message", "Quantity cannot be negative.");
                return org.springframework.http.ResponseEntity.badRequest().body(response);
            }

            if (medicineService.isDuplicateBatch(medicine.getBatchNumber(), medicine.getId())) {
                response.put("success", false);
                response.put("message", "A medicine with batch number '" + medicine.getBatchNumber() + "' already exists.");
                return org.springframework.http.ResponseEntity.badRequest().body(response);
            }

            Medicine saved = medicineService.saveMedicine(medicine);
            alertService.checkMedicineAlerts(saved);
            expiryAlertService.evaluateMedicineExpiry(saved, 1L);
            auditLogService.logAction("Admin", medicine.getId() == null ? "ADD_MEDICINE" : "EDIT_MEDICINE", "Medicine", String.valueOf(saved.getId()), "Saved medicine: " + saved.getMedicineName() + " (Batch: " + saved.getBatchNumber() + ")");

            response.put("success", true);
            response.put("message", "✓ Medicine saved successfully.");
            response.put("medicine", saved);
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return org.springframework.http.ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Unable to save medicine: " + e.getMessage());
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> handleJsonParseException(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", false);
        response.put("message", "Invalid JSON request payload or field data type mismatch.");
        return org.springframework.http.ResponseEntity.badRequest().body(response);
    }


    // ==========================================
    // DELETE MEDICINE
    // ==========================================

    @GetMapping("/delete/{id}")
    public String deleteMedicine(
            @PathVariable Long id) {

        try {

            medicineService.deleteMedicine(id);

            return "redirect:/medicines?success=Medicine+deleted+successfully";

        } catch (RuntimeException e) {

            return "redirect:/medicines?error=Medicine+not+found";
        }
    }
}
