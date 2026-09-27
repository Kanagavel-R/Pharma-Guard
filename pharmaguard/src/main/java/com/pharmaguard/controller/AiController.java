package com.pharmaguard.controller;

import com.pharmaguard.dto.AiChatResponse;
import com.pharmaguard.service.PharmaGuardAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai-legacy")
public class AiController {

    private final PharmaGuardAiService aiService;

    public AiController(PharmaGuardAiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request, Authentication auth) {
        String prompt = request != null ? request.get("prompt") : null;
        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("reply", "Prompt cannot be empty."));
        }

        AiChatResponse response = aiService.processChatRequest(prompt.trim(), auth);
        return ResponseEntity.ok(Map.of(
                "reply", response.getReply() != null ? response.getReply() : "No response generated.",
                "responseType", response.getResponseType() != null ? response.getResponseType() : "TEXT",
                "sourceData", response.getSourceData() != null ? response.getSourceData() : ""
        ));
    }
}
