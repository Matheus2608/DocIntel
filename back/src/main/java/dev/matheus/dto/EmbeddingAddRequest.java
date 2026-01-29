package dev.matheus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Request DTO for adding a new embedding entry
 */
public record EmbeddingAddRequest(
        @NotNull(message = "Text is required")
        @NotBlank(message = "Text cannot be empty")
        @Size(max = 10000, message = "Text cannot exceed 10,000 characters")
        String text,

        String fileName,

        Map<String, String> customMetadata
) {
    /**
     * Constructor with default values
     */
    public EmbeddingAddRequest {
        if (fileName == null || fileName.isBlank()) {
            fileName = "manual-entry";
        }
        if (customMetadata == null) {
            customMetadata = Map.of();
        }
    }
}
