package com.pharmaguard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {
    private String reply;
    private String responseType; // TEXT, TABLE, CARDS, WARNING, ACTION_REQUIRED
    private String sourceData;   // Data provenance string e.g. "Source: Pharma Guard Live Inventory DB"
    private List<String> tableHeaders;
    private List<List<String>> tableRows;
    private Map<String, Object> cardMetrics;
    private ActionProposal actionProposal;
    private List<String> quickActions;
    private boolean requiresConfirmation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionProposal {
        private String actionId;
        private String actionType; // REDUCE_STOCK, REORDER, DISMISS_ALERT, UPDATE_MEDICINE
        private Long targetEntityId;
        private String entityName;
        private String description;
        private String confirmationMessage;
        private Map<String, Object> parameters;
        private String confirmButtonText;
        private String cancelButtonText;
    }
}
