package dev.matheus.service.docling;

/**
 * Estimates token count for text content.
 * Uses approximation: 1 token ≈ 4 characters (English text average).
 */
public class TokenEstimator {

    private static final int CHARACTERS_PER_TOKEN = 4;

    /**
     * Estimate token count for given content.
     * Uses rough approximation: 1 token ≈ 4 characters.
     *
     * @param content The text content to estimate
     * @return Estimated token count (minimum 1 for non-empty content)
     */
    public int estimate(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        // Rough estimation: 1 token ≈ 4 characters
        // Ensure at least 1 token for non-empty content
        return Math.max(1, content.length() / CHARACTERS_PER_TOKEN);
    }
}
