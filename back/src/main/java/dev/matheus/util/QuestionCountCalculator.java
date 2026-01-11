package dev.matheus.util;

/**
 * Utility class for calculating the optimal number of questions to generate
 * based on text segment characteristics.
 */
public class QuestionCountCalculator {

    private QuestionCountCalculator() {
        // Utility class
    }

    /**
     * Calculates the optimal number of questions for a given text chunk.
     * Takes into account phrase count, word count, and sentence complexity.
     */
    public static int calculateOptimalQuestionCount(String text) {
        int phraseCount = countPhrases(text);
        int wordCount = countWords(text);
        int sentenceComplexity = calculateSentenceComplexity(text);

        int baseQuestions = determineBaseQuestions(phraseCount, wordCount);

        // Adjust for information density
        if (sentenceComplexity > 2) {
            baseQuestions = Math.min(baseQuestions + 1, 4);
        }

        return Math.max(1, Math.min(4, baseQuestions));
    }

    private static int determineBaseQuestions(int phraseCount, int wordCount) {
        if (phraseCount <= 2) {
            return wordCount > 50 ? 2 : 1;
        } else if (phraseCount == 3) {
            return wordCount > 80 ? 3 : 2;
        } else {
            return Math.min(phraseCount, 4);
        }
    }

    private static int countPhrases(String text) {
        return text.split("[.!?]+").length;
    }

    private static int countWords(String text) {
        return text.trim().split("\\s+").length;
    }

    private static int calculateSentenceComplexity(String text) {
        String[] sentences = text.split("[.!?]+");
        return (int) Math.round(
                java.util.Arrays.stream(sentences)
                        .mapToInt(s -> s.split("[,;:]").length)
                        .average()
                        .orElse(1.0)
        );
    }
}

