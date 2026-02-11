package dev.matheus.dto;

import dev.matheus.entity.ProcessingStatus;
import java.time.LocalDateTime;

public record DocumentStatusDTO(
    ProcessingStatus status,
    String error,
    Integer chunkCount,
    LocalDateTime processedAt
) {}
