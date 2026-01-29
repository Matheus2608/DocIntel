package dev.matheus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Request DTO for updating an existing embedding entry
 */
public record EmbeddingUpdateRequest(
        @NotNull(message = "Entry ID is required")
        @NotBlank(message = "Entry ID cannot be empty")
        String entryId,

        @Size(max = 10000, message = "Text cannot exceed 10,000 characters")
        String text,

        String fileName,

        Map<String, String> customMetadata
) {
    /**
     * Validate that at least one field is provided for update
     */
    public boolean hasUpdates() {
        return (text != null && !text.isBlank()) ||
               (fileName != null && !fileName.isBlank()) ||
               (customMetadata != null && !customMetadata.isEmpty());
    }
}
