package dev.matheus.splitter;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmartDocumentSplitter implements DocumentSplitter {

    private static final int MAX_CHUNK_SIZE = 500; // caracteres
    private static final int MIN_CHUNK_SIZE = 150;
    private static final int OVERLAP = 50;

    @Override
    public List<TextSegment> split(Document document) {
        String text = document.text();

        // Limpa o texto removendo formatações estranhas
        text = cleanText(text);

        // Detecta se é tabela ou texto normal
        if (isTableContent(text)) {
            return splitTableContent(text);
        } else {
            return splitNormalText(text);
        }
    }

    private String cleanText(String text) {
        return text
                // Remove múltiplas quebras de linha
                .replaceAll("\n{3,}", "\n\n")
                // Remove espaços múltiplos
                .replaceAll(" {2,}", " ")
                // Remove quebras de linha dentro de palavras
                .replaceAll("(?<=\\S)\n(?=\\S)", " ")
                // Normaliza quebras de linha
                .replaceAll("\\r\\n", "\n")
                .trim();
    }

    private boolean isTableContent(String text) {
        // Detecta se tem muitas quebras de linha sem pontuação
        long lineBreaks = text.chars().filter(ch -> ch == '\n').count();
        long sentences = text.split("[.!?]").length;

        return lineBreaks > sentences * 2 ||
                text.contains("km)") ||
                text.matches(".*\\d+h\\d+m.*");
    }

    private List<TextSegment> splitTableContent(String text) {
        List<TextSegment> segments = new ArrayList<>();

        // Divide por blocos lógicos (seções separadas por linhas vazias duplas)
        String[] blocks = text.split("\n\n+");

        StringBuilder currentChunk = new StringBuilder();

        for (String block : blocks) {
            block = block.trim();
            if (block.isEmpty()) continue;

            // Se adicionar este bloco ultrapassar o limite, finaliza o chunk atual
            if (currentChunk.length() + block.length() > MAX_CHUNK_SIZE &&
                    currentChunk.length() > MIN_CHUNK_SIZE) {

                segments.add(TextSegment.from(currentChunk.toString().trim()));

                // Mantém overlap pegando últimas linhas
                String[] lines = currentChunk.toString().split("\n");
                currentChunk = new StringBuilder();
                if (lines.length > 2) {
                    currentChunk.append(lines[lines.length - 2]).append("\n");
                    currentChunk.append(lines[lines.length - 1]).append("\n");
                }
            }

            currentChunk.append(block).append("\n\n");
        }

        // Adiciona último chunk
        if (currentChunk.length() > 0) {
            segments.add(TextSegment.from(currentChunk.toString().trim()));
        }

        return segments;
    }

    private List<TextSegment> splitNormalText(String text) {
        List<TextSegment> segments = new ArrayList<>();

        // Divide em parágrafos
        String[] paragraphs = text.split("\n\n+");

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            // Se o parágrafo é pequeno, mantém inteiro
            if (paragraph.length() <= MAX_CHUNK_SIZE) {
                segments.add(TextSegment.from(paragraph));
                continue;
            }

            // Se é grande, divide por sentenças
            List<String> sentences = splitIntoSentences(paragraph);
            StringBuilder currentChunk = new StringBuilder();

            for (String sentence : sentences) {
                if (currentChunk.length() + sentence.length() > MAX_CHUNK_SIZE &&
                        currentChunk.length() > MIN_CHUNK_SIZE) {

                    segments.add(TextSegment.from(currentChunk.toString().trim()));
                    currentChunk = new StringBuilder();
                }

                currentChunk.append(sentence).append(" ");
            }

            if (!currentChunk.isEmpty()) {
                segments.add(TextSegment.from(currentChunk.toString().trim()));
            }
        }

        return segments;
    }

    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();

        // Regex melhorado para detectar fim de sentença
        Pattern pattern = Pattern.compile(
                "[^.!?\\n]+[.!?]+|[^.!?\\n]+$",
                Pattern.MULTILINE
        );

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String sentence = matcher.group().trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }

        return sentences;
    }
}

