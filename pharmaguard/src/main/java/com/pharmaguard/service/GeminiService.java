package com.pharmaguard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.6-flash}")
    private String modelName;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent}")
    private String apiUrl;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiService() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public String getModelName() {
        return modelName != null && !modelName.isBlank() ? modelName : "gemini-3.6-flash";
    }

    public String askAiAssistant(String userPrompt) {
        if (!isConfigured()) {
            return "AI assistant is temporarily unavailable. Please configure 'GEMINI_API_KEY' on the backend server.";
        }

        try {
            String activeModel = getModelName();
            String targetUrl = (apiUrl != null && apiUrl.contains(activeModel)) ? apiUrl :
                    "https://generativelanguage.googleapis.com/v1beta/models/" + activeModel + ":generateContent";
            String fullUrl = targetUrl + "?key=" + apiKey.trim();

            String systemPrompt = "You are Pharma Guard AI, an enterprise pharmaceutical management assistant for licensed healthcare professionals and staff. " +
                    "Provide professional, concise, and structured guidance regarding medicine inventory, stock reordering, expiry policies, FEFO rules, and pharmacy operations. " +
                    "DO NOT use emojis in your responses. Rely strictly on provided application data when discussing stock quantities, expiry dates, or supplier details. " +
                    "For clinical medical diagnosis or treatment decisions, clearly advise consulting a qualified physician.";

            Map<String, Object> systemPart = Map.of("text", systemPrompt);
            Map<String, Object> systemInstruction = Map.of("parts", List.of(systemPart));

            Map<String, Object> userPart = Map.of("text", userPrompt);
            Map<String, Object> contentItem = Map.of("role", "user", "parts", List.of(userPart));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("systemInstruction", systemInstruction);
            requestBody.put("contents", List.of(contentItem));

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode textNode = candidates.get(0)
                            .path("content")
                            .path("parts")
                            .get(0)
                            .path("text");
                    if (!textNode.isMissingNode()) {
                        return textNode.asText();
                    }
                }
            } else {
                System.err.println("Pharma Guard Gemini API Error (HTTP " + response.statusCode() + "): " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error communicating with Pharma Guard Gemini AI: " + e.getMessage());
        }

        return "AI assistant is temporarily unavailable. Please try again in a few moments.";
    }

    public String explainVerificationRisk(String medicineName, String batchNumber, String manufacturer, int riskScore, String riskLevel, List<String> positiveFactors, List<String> warningFactors) {
        if (!isConfigured()) {
            return generateFallbackExplanation(riskScore);
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Medicine Verification Summary:\n")
              .append("- Medicine Name: ").append(medicineName).append("\n")
              .append("- Batch Number: ").append(batchNumber).append("\n")
              .append("- Manufacturer: ").append(manufacturer).append("\n")
              .append("- Risk Score: ").append(riskScore).append("/100 (Level: ").append(riskLevel).append(")\n")
              .append("Positive Signals:\n");
        for (String p : positiveFactors) {
            prompt.append("  ✓ ").append(p).append("\n");
        }
        prompt.append("Warning Signals:\n");
        for (String w : warningFactors) {
            prompt.append("  ⚠ ").append(w).append("\n");
        }
        prompt.append("\nInstructions: Provide a concise, clear 2-3 sentence AI explanation of why this risk score was assigned based ONLY on the evidence above. Do NOT invent facts or claim 100% genuine or fake.");

        String response = askAiAssistant(prompt.toString());
        if (response == null || response.contains("temporarily unavailable") || response.startsWith("Error") || response.contains("HTTP") || response.contains("404")) {
            return generateFallbackExplanation(riskScore);
        }
        return response;
    }

    private String generateFallbackExplanation(int riskScore) {
        if (riskScore <= 24) {
            return "Pharma Guard verified this medicine successfully against the available medicine and batch records. The current risk score is low. No immediate verification concerns were detected.";
        } else if (riskScore <= 49) {
            return "The product information matched basic records, but some packaging or batch details could not be fully verified. Additional verification is recommended.";
        } else if (riskScore <= 74) {
            return "Multiple verification signals require attention, including unverified batch numbers or unusual scan frequency. Professional verification before use is recommended.";
        } else {
            return "Significant risk factors were detected, such as expiration or batch record mismatches. Please consult a licensed pharmacist or authority before dispensing.";
        }
    }
}
