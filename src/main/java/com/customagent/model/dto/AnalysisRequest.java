package com.customagent.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for triggering a new analysis run. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequest {

    @NotBlank(message = "Repository URL is required")
    private String repositoryUrl;

    @NotNull(message = "Agent ID is required")
    private Long agentId;
}
