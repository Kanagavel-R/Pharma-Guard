package com.pharmaguard.controller;

import com.pharmaguard.entity.MedicineRecall;
import com.pharmaguard.service.RecallService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class RecallController {

    private final RecallService recallService;

    public RecallController(RecallService recallService) {
        this.recallService = recallService;
    }

    @GetMapping({"/recalls", "/medicines/recalls"})
    public String viewRecalls(Principal principal, Model model) {
        String username = (principal != null) ? principal.getName() : "Admin";
        model.addAttribute("username", username);

        List<MedicineRecall> recalls = recallService.getAllRecalls();
        model.addAttribute("recalls", recalls);
        model.addAttribute("activeCount", recallService.getActiveRecallCount());

        return "recalls";
    }

    @PostMapping({"/api/recalls/save", "/recalls/save"})
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveRecallApi(@RequestBody MedicineRecall recall, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = (principal != null) ? principal.getName() : "Admin";
            MedicineRecall saved = recallService.createRecall(recall, username);

            response.put("success", true);
            response.put("message", "✓ Medicine recall created successfully.");
            response.put("recall", saved);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Unable to create recall: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/api/recalls/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateRecallStatus(
            @PathVariable Long id,
            @RequestParam("status") String status,
            Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = (principal != null) ? principal.getName() : "Admin";
            MedicineRecall updated = recallService.updateStatus(id, status, username);

            response.put("success", true);
            response.put("message", "✓ Recall status updated to " + status);
            response.put("recall", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Unable to update recall status: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
