package com.pharmaguard.controller;

import com.pharmaguard.service.AlertService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public String alerts(Model model) {
        // Refresh alerts real-time before displaying active alerts list
        alertService.refreshAllMedicineAlerts();

        model.addAttribute("alerts", alertService.getActiveAlerts());
        return "alerts";
    }

    @GetMapping("/resolve/{id}")
    public String resolveAlert(@PathVariable Long id) {
        alertService.resolveAlert(id);
        return "redirect:/alerts";
    }
}