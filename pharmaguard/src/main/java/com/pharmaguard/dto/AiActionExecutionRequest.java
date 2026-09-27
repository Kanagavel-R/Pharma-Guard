package com.pharmaguard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiActionExecutionRequest {
    private String actionId;
    private String actionType;
    private Long targetEntityId;
    private Map<String, Object> parameters;
}
