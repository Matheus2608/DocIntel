package dev.matheus.dto;

import dev.matheus.entity.ContentType;

import java.time.LocalDateTime;

/**
 * Response DTO for document chunk information.
 */
public record ChunkResponse(
        String id,
        String content,
        ContentType contentType,
        Integer position,
        String sectionHeading,
        Integer headingLevel,
        Integer tokenCount,
        LocalDateTime createdAt
) {}
