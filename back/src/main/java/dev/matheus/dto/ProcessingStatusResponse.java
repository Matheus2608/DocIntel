package dev.matheus.dto;

import dev.matheus.entity.ProcessingStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for document processing status.
 */
public record ProcessingStatusResponse(
        String documentId,
        ProcessingStatus status,
        Integer chunkCount,
        String processingError,
        LocalDateTime processedAt,
        String processorVersion
) {}
