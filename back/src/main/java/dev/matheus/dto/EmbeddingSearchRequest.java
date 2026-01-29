package dev.matheus.dto;

import jakarta.validation.constraints.*;

/**
 * Request DTO for searching embeddings by text query
 */
public record EmbeddingSearchRequest(
        @NotNull(message = "Query text is required")
        @NotBlank(message = "Query text cannot be empty")
        String query,

        @Min(value = 1, message = "Max results must be at least 1")
        @Max(value = 100, message = "Max results cannot exceed 100")
        Integer maxResults,

        @DecimalMin(value = "0.0", message = "Min similarity must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Min similarity must be between 0.0 and 1.0")
        Double minSimilarity
) {
    /**
     * Constructor with default values
     */
    public EmbeddingSearchRequest {
        if (maxResults == null) {
            maxResults = 10;
        }
        if (minSimilarity == null) {
            minSimilarity = 0.7;
        }
    }
}
