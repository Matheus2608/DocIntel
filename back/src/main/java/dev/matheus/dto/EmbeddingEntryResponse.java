package dev.matheus.dto;

import java.util.Map;

/**
 * Response DTO representing a single embedding entry
 */
public record EmbeddingEntryResponse(
        String id,
        String text,
        Double similarity,
        Map<String, String> metadata
) {
}
