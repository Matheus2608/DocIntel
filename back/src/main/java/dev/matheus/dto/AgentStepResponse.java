package dev.matheus.dto;

import java.time.LocalDateTime;

public record AgentStepResponse(
        String id,
        String toolName,
        String status,
        String argumentsJson,
        String resultPreview,
        String errorMessage,
        int sequenceIdx,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {}
