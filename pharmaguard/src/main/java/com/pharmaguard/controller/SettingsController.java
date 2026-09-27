package com.pharmaguard.controller;

import com.pharmaguard.entity.Medicine;
import com.pharmaguard.entity.PharmacySettings;
import com.pharmaguard.entity.StockTransaction;
import com.pharmaguard.entity.User;
import com.pharmaguard.service.AlertService;
import com.pharmaguard.service.MedicineService;
import com.pharmaguard.service.SettingsService;
import com.pharmaguard.service.StockTransactionService;
import com.pharmaguard.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final SettingsService settingsService;
    private final UserService userService;
    private final AlertService alertService;
    private final MedicineService medicineService;
    private final StockTransactionService stockTransactionService;

    public SettingsController(
            SettingsService settingsService,
            UserService userService,
            AlertService alertService,
            MedicineService medicineService,
            StockTransactionService stockTransactionService) {
        this.settingsService = settingsService;
        this.userService = userService;
        this.alertService = alertService;
        this.medicineService = medicineService;
        this.stockTransactionService = stockTransactionService;
    }

    @GetMapping
    public String viewSettings(Principal principal, Model model) {
        String username = (principal != null) ? principal.getName() : "admin";
        Optional<User> currentUserOpt = userService.findByUsername(username);

        User currentUser = currentUserOpt.orElseGet(() -> {
            User dummy = new User();
            dummy.setUsername(username);
            dummy.setFullName("Administrator");
            dummy.setEmail(username + "@pharmaguard.com");
            dummy.setRole("ADMIN");
            return dummy;
        });

        PharmacySettings settings = settingsService.getSettings();
        List<User> systemUsers = userService.getAllUsers();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("settings", settings);
        model.addAttribute("systemUsers", systemUsers);
        model.addAttribute("activeAlertCount", alertService.getActiveAlertCount());

        return "settings";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            String username = (principal != null) ? principal.getName() : "admin";
            userService.updateProfile(username, fullName, email);
            redirectAttributes.addFlashAttribute("successMsg", "✓ Profile updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "⚠️ Failed to update profile: " + e.getMessage());
        }

        return "redirect:/settings";
    }

    @PostMapping("/password/update")
    public String changePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            if (!newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException("New passwords do not match.");
            }
            String username = (principal != null) ? principal.getName() : "admin";
            userService.changePassword(username, currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMsg", "✓ Password updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "⚠️ Failed to change password: " + e.getMessage());
        }

        return "redirect:/settings";
    }

    @PostMapping("/notifications/update")
    public String updateNotificationPreferencesForm(
            @ModelAttribute("settings") PharmacySettings settings,
            RedirectAttributes redirectAttributes) {

        try {
            settingsService.saveSettings(settings);
            redirectAttributes.addFlashAttribute("successMsg", "✓ Notification preferences saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "⚠️ Failed to save notification preferences: " + e.getMessage());
        }

        return "redirect:/settings";
    }

    @PostMapping("/api/settings/notifications/update")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> updateNotificationPreferencesApi(
            @RequestBody java.util.Map<String, Boolean> payload) {

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            PharmacySettings current = settingsService.getSettings();

            if (payload.containsKey("expiryAlerts")) current.setExpiryAlerts(payload.get("expiryAlerts"));
            if (payload.containsKey("lowStockAlerts")) current.setLowStockAlerts(payload.get("lowStockAlerts"));
            if (payload.containsKey("verificationAlerts")) current.setVerificationAlerts(payload.get("verificationAlerts"));
            if (payload.containsKey("supplierAlerts")) current.setSupplierAlerts(payload.get("supplierAlerts"));
            if (payload.containsKey("securityNotifications")) current.setSecurityNotifications(payload.get("securityNotifications"));
            if (payload.containsKey("emailNotifications")) current.setEmailNotifications(payload.get("emailNotifications"));
            if (payload.containsKey("inAppNotifications")) current.setInAppNotifications(payload.get("inAppNotifications"));

            PharmacySettings saved = settingsService.saveSettings(current);

            response.put("success", true);
            response.put("message", "✓ Notification preferences saved successfully.");
            response.put("settings", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Unable to save preferences: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/save")
    public String saveSettings(
            @ModelAttribute("settings") PharmacySettings settings,
            RedirectAttributes redirectAttributes) {

        try {
            settingsService.saveSettings(settings);
            redirectAttributes.addFlashAttribute("successMsg", "✓ Settings saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "⚠️ Unable to save settings: " + e.getMessage());
        }

        return "redirect:/settings";
    }

    @PostMapping("/reset")
    public String resetSettings(RedirectAttributes redirectAttributes) {
        try {
            settingsService.resetToDefault();
            redirectAttributes.addFlashAttribute("successMsg", "✓ Settings reset to default configuration.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "⚠️ Unable to reset settings: " + e.getMessage());
        }

        return "redirect:/settings";
    }

    @GetMapping("/export/medicines")
    @ResponseBody
    public ResponseEntity<byte[]> exportMedicinesCsv() {
        List<Medicine> list = medicineService.getAllMedicines();
        StringBuilder csv = new StringBuilder("ID,Medicine Name,Batch Number,Category,Quantity,Price,Expiry Date,Supplier,Storage Location\n");

        for (Medicine m : list) {
            csv.append(m.getId()).append(",")
               .append("\"").append(m.getMedicineName() != null ? m.getMedicineName().replace("\"", "\"\"") : "").append("\",")
               .append("\"").append(m.getBatchNumber() != null ? m.getBatchNumber().replace("\"", "\"\"") : "").append("\",")
               .append("\"").append(m.getCategory() != null ? m.getCategory().replace("\"", "\"\"") : "").append("\",")
               .append(m.getQuantity()).append(",")
               .append(m.getPrice()).append(",")
               .append(m.getExpiryDate() != null ? m.getExpiryDate().toString() : "").append(",")
               .append("\"").append(m.getSupplierName() != null ? m.getSupplierName().replace("\"", "\"\"") : "").append("\",")
               .append("\"").append(m.getStorageLocation() != null ? m.getStorageLocation().replace("\"", "\"\"") : "").append("\"\n");
        }

        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pharmaguard_medicines.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @GetMapping("/export/transactions")
    @ResponseBody
    public ResponseEntity<byte[]> exportTransactionsCsv() {
        List<StockTransaction> list = stockTransactionService.getAllTransactions();
        StringBuilder csv = new StringBuilder("ID,Date,Medicine,Type,Quantity,Previous Stock,New Stock,User,Notes\n");

        for (StockTransaction tx : list) {
            csv.append(tx.getId()).append(",")
               .append(tx.getTransactionDate() != null ? tx.getTransactionDate().toString() : "").append(",")
               .append("\"").append(tx.getMedicine() != null ? tx.getMedicine().getMedicineName().replace("\"", "\"\"") : "").append("\",")
               .append(tx.getTransactionType() != null ? tx.getTransactionType().name() : "").append(",")
               .append(tx.getQuantity()).append(",")
               .append(tx.getPreviousStock() != null ? tx.getPreviousStock() : "").append(",")
               .append(tx.getNewStock() != null ? tx.getNewStock() : "").append(",")
               .append("\"").append(tx.getPerformedBy() != null ? tx.getPerformedBy().replace("\"", "\"\"") : "Admin").append("\",")
               .append("\"").append(tx.getNotes() != null ? tx.getNotes().replace("\"", "\"\"") : "").append("\"\n");
        }

        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pharmaguard_transactions.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }
}
