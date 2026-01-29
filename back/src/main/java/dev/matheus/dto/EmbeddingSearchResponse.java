package dev.matheus.dto;

import java.util.List;

/**
 * Response DTO for embedding search results
 */
public record EmbeddingSearchResponse(
        List<EmbeddingEntryResponse> results,
        int totalResults
) {
}
