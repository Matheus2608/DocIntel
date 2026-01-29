package dev.matheus.dto;

/**
 * Response DTO after successfully adding an embedding entry
 */
public record EmbeddingAddResponse(
        String entryId,
        String message
) {
}
