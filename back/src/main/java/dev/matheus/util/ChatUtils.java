package dev.matheus.util;

import com.google.gson.Gson;
import dev.langchain4j.data.document.Document;

import java.nio.charset.StandardCharsets;

public class ChatUtils {

    private static final Gson gson = new Gson();


    public static int getNumberQuestions(String paragraph) {
        int phraseCount = getNumberOfPhrases(paragraph);
        int wordCount = countWords(paragraph);
        int sentenceComplexity = calculateSentenceComplexity(paragraph);

        // Base: 1 pergunta a cada 2-3 frases
        double baseQuestions = phraseCount / 2.4;

        // Ajuste por tamanho: parágrafos maiores podem gerar mais perguntas
        double lengthBonus = wordCount > 150 ? 1.5 : (wordCount > 80 ? 1.0 : 0.5);

        // Ajuste por complexidade: frases mais complexas sugerem mais conteúdo
        double complexityMultiplier = 1.0 + (sentenceComplexity / 10.0);

        int totalQuestions = (int) Math.ceil(baseQuestions * complexityMultiplier + lengthBonus);

        // Limites: mínimo 2, máximo 10 perguntas por parágrafo
        return Math.max(2, Math.min(10, totalQuestions));
    }

    static private int getNumberOfPhrases(String paragraph) {
        return paragraph.split("[.!?]+").length;
    }

    static private int countWords(String paragraph) {
        return paragraph.trim().split("\\s+").length;
    }

    static private int calculateSentenceComplexity(String paragraph) {
        String[] sentences = paragraph.split("[.!?]+");
        return (int) Math.round(
                java.util.Arrays.stream(sentences)
                        .mapToInt(s -> s.split("[,;:]").length) // cláusulas por frase
                        .average()
                        .orElse(1.0)
        );
    }

    public static Document toDocument(byte[] docBytes) {
        String documentText = new String(docBytes, StandardCharsets.UTF_8);
        return Document.from(documentText);
    }

    public static String[] toCleanedQuestions(String responseText) {
        // Remover markdown se houver
        String cleanedText = responseText.strip();
        if (cleanedText.startsWith("```json")) {
            cleanedText = cleanedText.substring(7).strip();
        }
        if (cleanedText.endsWith("```")) {
            cleanedText = cleanedText.substring(0, cleanedText.length() - 3).strip();
        }
        return gson.fromJson(cleanedText, String[].class);
    }

}
