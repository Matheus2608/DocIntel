package dev.matheus.dto;

/**
 * Response DTO after successfully updating an embedding entry
 */
public record EmbeddingUpdateResponse(
        String entryId,
        String message,
        boolean reEmbedded
) {
}
