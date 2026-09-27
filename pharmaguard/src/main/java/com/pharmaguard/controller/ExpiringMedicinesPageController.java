package com.pharmaguard.controller;

import com.pharmaguard.service.AlertService;
import com.pharmaguard.service.ExpiryAlertService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/medicines/expiring")
public class ExpiringMedicinesPageController {

    private final ExpiryAlertService expiryAlertService;
    private final AlertService alertService;

    public ExpiringMedicinesPageController(ExpiryAlertService expiryAlertService, AlertService alertService) {
        this.expiryAlertService = expiryAlertService;
        this.alertService = alertService;
    }

    @GetMapping
    public String showExpiringMedicinesPage(Model model, Authentication authentication) {
        expiryAlertService.evaluateAllMedicinesExpiry(1L);

        String username = authentication != null ? authentication.getName() : "Admin";

        model.addAttribute("username", username);
        model.addAttribute("activeAlertCount", alertService.getActiveAlertCount());

        model.addAttribute("criticalExpiring", expiryAlertService.getExpiringMedicines(3));
        model.addAttribute("highExpiring", expiryAlertService.getExpiringMedicines(7));
        model.addAttribute("warningExpiring", expiryAlertService.getExpiringMedicines(30));
        model.addAttribute("expiredMedicines", expiryAlertService.getExpiredMedicines());

        model.addAttribute("criticalCount", expiryAlertService.countCriticalExpiring(3));
        model.addAttribute("highCount", expiryAlertService.countCriticalExpiring(7) - expiryAlertService.countCriticalExpiring(3));
        model.addAttribute("warningCount", expiryAlertService.countCriticalExpiring(30) - expiryAlertService.countCriticalExpiring(7));
        model.addAttribute("expiredCount", expiryAlertService.countExpired());

        return "expiring-medicines";
    }
}
