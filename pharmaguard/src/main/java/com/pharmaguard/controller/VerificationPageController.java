package com.pharmaguard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaguard.entity.VerificationHistory;
import com.pharmaguard.service.AlertService;
import com.pharmaguard.service.VerificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class VerificationPageController {

    private final VerificationService verificationService;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    public VerificationPageController(VerificationService verificationService, AlertService alertService) {
        this.verificationService = verificationService;
        this.alertService = alertService;
        this.objectMapper = new ObjectMapper();
    }

    @GetMapping("/verifications")
    public String showVerificationHistory(
            @RequestParam(value = "risk", required = false) String riskLevel,
            Model model,
            Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "Admin";
        List<VerificationHistory> historyList;

        if (riskLevel != null && !riskLevel.isBlank() && !"ALL".equalsIgnoreCase(riskLevel)) {
            historyList = verificationService.getVerificationsByRisk(riskLevel.toUpperCase());
        } else {
            historyList = verificationService.getAllVerifications();
        }

        model.addAttribute("username", username);
        model.addAttribute("activeAlertCount", alertService.getActiveAlertCount());
        model.addAttribute("verifications", historyList);
        model.addAttribute("selectedRisk", riskLevel != null ? riskLevel.toUpperCase() : "ALL");

        model.addAttribute("totalCount", verificationService.getAllVerifications().size());
        model.addAttribute("lowRiskCount", verificationService.getCountByRisk("LOW"));
        model.addAttribute("moderateRiskCount", verificationService.getCountByRisk("MODERATE"));
        model.addAttribute("highRiskCount", verificationService.getCountByRisk("HIGH"));
        model.addAttribute("criticalRiskCount", verificationService.getCountByRisk("CRITICAL"));

        return "verifications";
    }

    @GetMapping("/verification/{id}")
    public String showVerificationDetails(
            @PathVariable("id") Long id,
            Model model,
            Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "Admin";
        Optional<VerificationHistory> vhOpt = verificationService.getVerificationById(id);

        if (vhOpt.isEmpty()) {
            return "redirect:/verifications";
        }

        VerificationHistory vh = vhOpt.get();

        try {
            @SuppressWarnings("unchecked")
            List<String> detectedIssues = objectMapper.readValue(vh.getDetectedIssuesJson() != null ? vh.getDetectedIssuesJson() : "[]", List.class);
            model.addAttribute("detectedIssues", detectedIssues);

            @SuppressWarnings("unchecked")
            Map<String, Object> extractedInfo = objectMapper.readValue(vh.getExtractedInfoJson() != null ? vh.getExtractedInfoJson() : "{}", Map.class);
            model.addAttribute("extractedInfo", extractedInfo);
        } catch (Exception e) {
            model.addAttribute("detectedIssues", List.of());
            model.addAttribute("extractedInfo", Map.of());
        }

        model.addAttribute("username", username);
        model.addAttribute("activeAlertCount", alertService.getActiveAlertCount());
        model.addAttribute("verification", vh);

        return "verification-details";
    }
}
