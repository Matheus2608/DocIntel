package dev.matheus.service.pdf;

/**
 * Utility class for cleaning extracted text from PDFs.
 */
public class TextNormalizer {

    private TextNormalizer() {
        // Utility class
    }

    /**
     * Normalizes text by:
     * 1. Removing garbage markers
     * 2. Fixing split words
     * 3. Joining broken lines
     * 4. Cleaning excessive whitespace
     */
    public static String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String cleaned = removeGarbageMarkers(text);
        cleaned = fixSplitWords(cleaned);
        cleaned = joinBrokenLines(cleaned);
        cleaned = cleanWhitespace(cleaned);

        return cleaned;
    }

    private static String removeGarbageMarkers(String text) {
        // Remove all text between garbage tags (including the tags themselves)
        return text.replaceAll("\\[\\[LIXO_INICIO]].*?\\[\\[LIXO_FIM]]", "");
    }

    private static String fixSplitWords(String text) {
        // Fixes words separated by spaces (e.g., "C a m p e o n a t o" -> "Campeonato")
        return text.replaceAll("(?<=\\b\\p{L}) (?=\\p{L}\\b)", "");
    }

    private static String joinBrokenLines(String text) {
        // Join lines where sentence continues (lowercase letter after newline)
        String joined = text.replaceAll("([^.!?:;])\\n+(\\p{Ll})", "$1 $2");

        // Remove remaining single line breaks in the middle of sentences
        return joined.replaceAll("(?<!\\n)\\n(?!\\n)", " ");
    }

    private static String cleanWhitespace(String text) {
        // Clean multiple spaces and trim
        return text.replaceAll("\\s{2,}", " ").trim();
    }
}

