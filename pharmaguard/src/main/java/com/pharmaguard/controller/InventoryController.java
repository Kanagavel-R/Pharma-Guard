package com.pharmaguard.controller;

import com.pharmaguard.dto.DashboardIntelligenceDTO;
import com.pharmaguard.dto.MedicineIntelligenceDTO;
import com.pharmaguard.entity.Medicine;
import com.pharmaguard.entity.StockTransaction;
import com.pharmaguard.enums.TransactionType;
import com.pharmaguard.service.AlertService;
import com.pharmaguard.service.InventoryIntelligenceService;
import com.pharmaguard.service.MedicineService;
import com.pharmaguard.service.StockTransactionService;
import com.pharmaguard.service.SupplierService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class InventoryController {

    private final MedicineService medicineService;
    private final AlertService alertService;
    private final InventoryIntelligenceService intelligenceService;
    private final SupplierService supplierService;
    private final StockTransactionService stockTransactionService;

    public InventoryController(
            MedicineService medicineService,
            AlertService alertService,
            InventoryIntelligenceService intelligenceService,
            SupplierService supplierService,
            StockTransactionService stockTransactionService) {
        this.medicineService = medicineService;
        this.alertService = alertService;
        this.intelligenceService = intelligenceService;
        this.supplierService = supplierService;
        this.stockTransactionService = stockTransactionService;
    }

    @GetMapping("/inventory")
    public String viewInventory(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "stockStatus", required = false) String stockStatus,
            @RequestParam(value = "expiryStatus", required = false) String expiryStatus,
            @RequestParam(value = "supplier", required = false) String supplier,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            Model model) {

        alertService.refreshAllMedicineAlerts();

        DashboardIntelligenceDTO intel = intelligenceService.getDashboardIntelligence();
        List<Medicine> filteredMedicines = medicineService.filterInventory(keyword, category, stockStatus, expiryStatus, supplier, sortBy);

        // Calculate FEFO & days remaining details for list items
        Map<Long, String> fefoMap = new HashMap<>();
        Map<Long, Long> daysToExpiryMap = new HashMap<>();
        LocalDate today = LocalDate.now();

        for (Medicine m : filteredMedicines) {
            MedicineIntelligenceDTO mIntel = intelligenceService.evaluateMedicineIntelligence(m);
            if (mIntel.getFefoRecommendedBatch() != null) {
                fefoMap.put(m.getId(), "Use Batch " + mIntel.getFefoRecommendedBatch().getBatchNumber() + " first");
            }
            if (m.getExpiryDate() != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, m.getExpiryDate());
                daysToExpiryMap.put(m.getId(), days);
            }
        }

        // Summary Cards Data
        model.addAttribute("totalMedicines", medicineService.getTotalMedicines());
        model.addAttribute("totalStockUnits", medicineService.getTotalStock());
        model.addAttribute("lowStockCount", medicineService.getLowStockCount());
        model.addAttribute("expiringSoonCount", medicineService.getExpiringSoonCount());
        model.addAttribute("expiredCount", medicineService.getExpiredCount());
        model.addAttribute("outOfStockCount", medicineService.getOutOfStockCount());
        model.addAttribute("activeAlertCount", alertService.getActiveAlertCount());

        // Filter state & dropdown lists
        model.addAttribute("medicines", filteredMedicines);
        model.addAttribute("fefoMap", fefoMap);
        model.addAttribute("daysToExpiryMap", daysToExpiryMap);

        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("stockStatus", stockStatus);
        model.addAttribute("expiryStatus", expiryStatus);
        model.addAttribute("supplier", supplier);
        model.addAttribute("sortBy", sortBy);

        model.addAttribute("categories", medicineService.getDistinctCategories());
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        model.addAttribute("intelligence", intel);

        model.addAttribute("newMedicine", new Medicine());

        return "inventory";
    }

    @PostMapping("/inventory/stock-in")
    public String stockIn(
            @RequestParam("medicineId") Long medicineId,
            @RequestParam("quantity") int quantity,
            @RequestParam(value = "notes", required = false) String notes,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            String username = (principal != null) ? principal.getName() : "Admin";
            medicineService.recordStockTransaction(medicineId, TransactionType.STOCK_IN, quantity, notes != null ? notes : "Stock In addition", username);
            redirectAttributes.addFlashAttribute("successMsg", "Successfully added +" + quantity + " units to stock.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to add stock: " + e.getMessage());
        }

        return "redirect:/inventory";
    }

    @PostMapping("/inventory/stock-out")
    public String stockOut(
            @RequestParam("medicineId") Long medicineId,
            @RequestParam("quantity") int quantity,
            @RequestParam(value = "notes", required = false) String notes,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            String username = (principal != null) ? principal.getName() : "Admin";
            medicineService.recordStockTransaction(medicineId, TransactionType.STOCK_OUT, quantity, notes != null ? notes : "Stock Out reduction", username);
            redirectAttributes.addFlashAttribute("successMsg", "Successfully removed -" + quantity + " units from stock.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to reduce stock: " + e.getMessage());
        }

        return "redirect:/inventory";
    }

    @PostMapping("/inventory/save")
    public String saveInventoryStock(
            @ModelAttribute("newMedicine") Medicine medicine,
            RedirectAttributes redirectAttributes) {

        try {
            if (medicineService.isDuplicateBatch(medicine.getBatchNumber(), medicine.getId())) {
                redirectAttributes.addFlashAttribute("errorMsg", "Batch Number " + medicine.getBatchNumber() + " already exists!");
                return "redirect:/inventory";
            }
            medicineService.saveMedicine(medicine);
            redirectAttributes.addFlashAttribute("successMsg", "Medicine stock batch saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error saving stock: " + e.getMessage());
        }

        return "redirect:/inventory";
    }

    @GetMapping("/inventory/delete/{id}")
    public String deleteInventoryItem(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        try {
            medicineService.deleteMedicine(id);
            redirectAttributes.addFlashAttribute("successMsg", "Inventory record deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error deleting inventory: " + e.getMessage());
        }

        return "redirect:/inventory";
    }

    @GetMapping("/inventory/details/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getInventoryDetails(@PathVariable("id") Long id) {
        try {
            Medicine m = medicineService.getMedicineById(id);
            MedicineIntelligenceDTO intel = intelligenceService.evaluateMedicineIntelligence(m);

            Map<String, Object> data = new HashMap<>();
            data.put("id", m.getId());
            data.put("medicineName", m.getMedicineName());
            data.put("category", m.getCategory());
            data.put("batchNumber", m.getBatchNumber());
            data.put("quantity", m.getQuantity());
            data.put("price", m.getPrice());
            data.put("expiryDate", m.getExpiryDate() != null ? m.getExpiryDate().toString() : "N/A");
            data.put("reorderLevel", m.getReorderLevel());
            data.put("supplierName", m.getSupplierName() != null ? m.getSupplierName() : "N/A");
            data.put("storageLocation", m.getStorageLocation() != null ? m.getStorageLocation() : "Main Rack");

            data.put("daysUntilExpiry", intel.getDaysUntilExpiry());
            data.put("stockOutStatus", intel.getStockOutStatus());
            data.put("fefoMessage", intel.getFefoRecommendationMessage());
            data.put("reorderRecommended", intel.isReorderRecommended());
            data.put("recommendedReorderQuantity", intel.getRecommendedReorderQuantity());

            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/inventory/history/{id}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getInventoryHistory(@PathVariable("id") Long id) {
        try {
            List<StockTransaction> txList = stockTransactionService.getTransactionsByMedicine(id);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

            List<Map<String, Object>> result = txList.stream().map(tx -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", tx.getId());
                map.put("date", tx.getTransactionDate() != null ? tx.getTransactionDate().format(formatter) : "N/A");
                map.put("medicine", tx.getMedicine() != null ? tx.getMedicine().getMedicineName() : "N/A");
                map.put("batch", tx.getMedicine() != null ? tx.getMedicine().getBatchNumber() : "N/A");
                map.put("type", tx.getTransactionType().name());
                map.put("quantity", (tx.getTransactionType() == TransactionType.STOCK_OUT ? "-" : "+") + tx.getQuantity());
                map.put("previousStock", tx.getPreviousStock() != null ? tx.getPreviousStock() : "—");
                map.put("newStock", tx.getNewStock() != null ? tx.getNewStock() : "—");
                map.put("user", tx.getPerformedBy() != null ? tx.getPerformedBy() : "Admin");
                map.put("notes", tx.getNotes() != null ? tx.getNotes() : "N/A");
                return map;
            }).toList();

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
