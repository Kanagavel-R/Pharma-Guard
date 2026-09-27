package com.pharmaguard.controller;

import com.pharmaguard.dto.DashboardIntelligenceDTO;
import com.pharmaguard.service.AlertService;
import com.pharmaguard.service.InventoryIntelligenceService;
import com.pharmaguard.service.MedicineService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final MedicineService medicineService;
    private final AlertService alertService;
    private final InventoryIntelligenceService intelligenceService;
    private final com.pharmaguard.service.VerificationService verificationService;
    private final com.pharmaguard.service.ExpiryAlertService expiryAlertService;
    private final com.pharmaguard.service.StockTransactionService stockTransactionService;

    public DashboardController(
            MedicineService medicineService,
            AlertService alertService,
            InventoryIntelligenceService intelligenceService,
            com.pharmaguard.service.VerificationService verificationService,
            com.pharmaguard.service.ExpiryAlertService expiryAlertService,
            com.pharmaguard.service.StockTransactionService stockTransactionService) {

        this.medicineService = medicineService;
        this.alertService = alertService;
        this.intelligenceService = intelligenceService;
        this.verificationService = verificationService;
        this.expiryAlertService = expiryAlertService;
        this.stockTransactionService = stockTransactionService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalMedicines = medicineService.getTotalMedicines();
        long totalStock = medicineService.getTotalStock();
        long lowStockCount = medicineService.getLowStockCount();
        long activeAlertCount = alertService.getActiveAlertCount();

        DashboardIntelligenceDTO intel = intelligenceService.getDashboardIntelligence();

        model.addAttribute("totalMedicines", totalMedicines);
        model.addAttribute("totalStock", totalStock);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("activeAlertCount", activeAlertCount);
        model.addAttribute("todaysActivityCount", stockTransactionService.getTodaysActivityCount());
        model.addAttribute("recentTransactions", stockTransactionService.getRecentTransactions());
        model.addAttribute("recentAlerts", alertService.getActiveAlerts());

        model.addAttribute("stockOutRiskCount", intel.getStockOutRiskCount());
        model.addAttribute("reorderRecommendedCount", intel.getReorderRecommendedCount());
        model.addAttribute("expiryWasteRiskCount", intel.getExpiryWasteRiskCount());
        model.addAttribute("fefoRecommendationCount", intel.getFefoRecommendationCount());
        model.addAttribute("smartRecommendations", intel.getSmartRecommendations());

        // Verification Safety Intelligence
        model.addAttribute("totalVerifications", verificationService.getTotalVerificationsCount());
        model.addAttribute("verifLowRiskCount", verificationService.getCountByRisk("LOW"));
        model.addAttribute("verifModerateRiskCount", verificationService.getCountByRisk("MODERATE"));
        model.addAttribute("verifHighRiskCount", verificationService.getCountByRisk("HIGH") + verificationService.getCountByRisk("CRITICAL"));
        model.addAttribute("recentVerifications", verificationService.getRecentVerifications());

        // Expiry Alert System Intelligence
        long expiringSoonCount = expiryAlertService.countCriticalExpiring(7);
        long expiredCount = expiryAlertService.countExpired();
        long safeCount = Math.max(0, totalMedicines - (expiringSoonCount + expiredCount));

        model.addAttribute("expiry3DaysCount", expiryAlertService.countCriticalExpiring(3));
        model.addAttribute("expiry7DaysCount", expiringSoonCount);
        model.addAttribute("expiringSoonCount", expiringSoonCount);
        model.addAttribute("expiry30DaysCount", expiryAlertService.countCriticalExpiring(30));
        model.addAttribute("expiredCount", expiredCount);
        model.addAttribute("safeCount", safeCount);

        return "dashboard";
    }

    @GetMapping("/api/dashboard/summary")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> getDashboardSummary() {
        long totalMedicines = medicineService.getTotalMedicines();
        long totalStock = medicineService.getTotalStock();
        long lowStockCount = medicineService.getLowStockCount();
        long activeAlertCount = alertService.getActiveAlertCount();
        long unreadNotifs = expiryAlertService.getUnreadCount();
        long expiredCount = expiryAlertService.countExpired();
        long expiringSoonCount = expiryAlertService.countCriticalExpiring(7);

        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("totalMedicines", totalMedicines);
        summary.put("totalStock", totalStock);
        summary.put("lowStock", lowStockCount);
        summary.put("expired", expiredCount);
        summary.put("expiringSoon", expiringSoonCount);
        summary.put("activeAlerts", activeAlertCount);
        summary.put("unreadNotifications", unreadNotifs);

        return org.springframework.http.ResponseEntity.ok(summary);
    }

    @GetMapping("/")
    public String home(org.springframework.security.core.Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            return "redirect:/dashboard";
        }
        return "landing";
    }
}