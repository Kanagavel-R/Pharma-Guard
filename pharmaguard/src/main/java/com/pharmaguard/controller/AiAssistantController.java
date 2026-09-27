package com.pharmaguard.controller;

import com.pharmaguard.dto.AiActionExecutionRequest;
import com.pharmaguard.dto.AiChatRequest;
import com.pharmaguard.dto.AiChatResponse;
import com.pharmaguard.service.MedicineService;
import com.pharmaguard.service.PharmaGuardAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.pharmaguard.service.GeminiService;

@Controller
public class AiAssistantController {

    private final PharmaGuardAiService aiService;
    private final MedicineService medicineService;
    private final GeminiService geminiService;

    public AiAssistantController(PharmaGuardAiService aiService, MedicineService medicineService, GeminiService geminiService) {
        this.aiService = aiService;
        this.medicineService = medicineService;
        this.geminiService = geminiService;
    }

    @GetMapping("/ai-assistant")
    public String showAiAssistantPage(Model model, Authentication authentication) {
        model.addAttribute("userName", authentication != null ? authentication.getName() : "Pharmacist");
        model.addAttribute("totalMedicines", medicineService.getTotalMedicines());
        model.addAttribute("lowStockCount", medicineService.getLowStockCount());
        return "ai-assistant";
    }

    @GetMapping("/api/ai/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAiStatus() {
        boolean online = geminiService.isConfigured();
        return ResponseEntity.ok(Map.of(
                "online", online,
                "model", geminiService.getModelName(),
                "statusText", online ? "AI Online" : "AI Offline"
        ));
    }

    @PostMapping("/api/ai/chat")
    @ResponseBody
    public ResponseEntity<AiChatResponse> processChat(
            @RequestBody AiChatRequest request,
            Authentication authentication) {

        String prompt = request != null ? request.getPrompt() : null;
        AiChatResponse response = aiService.processChatRequest(prompt, authentication);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/ai/execute-action")
    @ResponseBody
    public ResponseEntity<AiChatResponse> executeAction(
            @RequestBody AiActionExecutionRequest request,
            Authentication authentication) {

        AiChatResponse response = aiService.executeConfirmedAction(request, authentication);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/ai/clear-history")
    @ResponseBody
    public ResponseEntity<Map<String, String>> clearHistory() {
        return ResponseEntity.ok(Map.of("message", "Conversation history cleared."));
    }
}
