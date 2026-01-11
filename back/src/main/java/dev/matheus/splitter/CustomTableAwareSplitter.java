package dev.matheus.splitter;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class CustomTableAwareSplitter implements DocumentSplitter {

    private static final Logger logger = Logger.getLogger(CustomTableAwareSplitter.class);

    private final int maxSegmentSize;
    private final int overlapSize;

    @Inject
    private TwoToThreeSentenceSplitter twoToThreeSentenceSplitter;

    // Use constructor injection with ConfigProperty so Quarkus can resolve the int values.
    @Inject
    public CustomTableAwareSplitter(
            @ConfigProperty(name = "splitter.max-segment-size", defaultValue = "1000") int maxSegmentSize,
            @ConfigProperty(name = "splitter.overlap-size", defaultValue = "200") int overlapSize) {
        this.maxSegmentSize = maxSegmentSize;
        this.overlapSize = overlapSize;
    }

    @Override
    public List<TextSegment> split(Document document) {
        String fullText = document.text();
        List<TextSegment> allSegments = new ArrayList<>();

        if (isBlank(fullText)) return allSegments;

        // 1. SEPARAÇÃO: Texto de um lado, Tabelas do outro
        int firstTableIndex = fullText.indexOf("[START_TABLE]");

        String textPart;
        String tablesPart;

        if (firstTableIndex != -1) {
            textPart = fullText.substring(0, firstTableIndex);
            tablesPart = fullText.substring(firstTableIndex);
        } else {
            textPart = fullText;
            tablesPart = "";
        }

        // 2. PROCESSAR PARTE DE TEXTO (Com Overlap e parágrafos)
        if (!isBlank(textPart)) {
            allSegments.addAll(twoToThreeSentenceSplitter.split(Document.from(textPart, document.metadata())));
        }

        // 3. PROCESSAR PARTE DE TABELAS (Atômico por tabela)
        if (!isBlank(tablesPart)) {
            allSegments.addAll(splitTablesPart(tablesPart, document.metadata()));
        }

        return allSegments;
    }

    /**
     * Divide o texto puro em parágrafos respeitando o tamanho máximo e overlap.
     */
    private List<TextSegment> splitTextPart(String text, Metadata metadata) {
        List<TextSegment> segments = new ArrayList<>();
        // Limpa as tags de LIXO que podem ter sobrado no texto
        // Only escape the opening brackets; closing brackets don't need backslashes in Java string literals
        String cleanText = text.replaceAll("\\[\\[LIXO_INICIO]].*?\\[\\[LIXO_FIM]]", "").trim();

        // Dividimos por quebras de linha duplas (parágrafos) ou simples
        String[] paragraphs = cleanText.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String p : paragraphs) {
            if (currentChunk.length() + p.length() > maxSegmentSize && currentChunk.length() > 0) {
                segments.add(TextSegment.from(currentChunk.toString().trim(), metadata));

                // Cálculo de Overlap
                String lastText = currentChunk.toString();
                int overlapStart = Math.max(0, lastText.length() - overlapSize);
                currentChunk = new StringBuilder(lastText.substring(overlapStart));
            }
            currentChunk.append(p).append("\n\n");
        }

        if (currentChunk.length() > 0 && !isBlank(currentChunk.toString())) {
            segments.add(TextSegment.from(currentChunk.toString().trim(), metadata));
        }

        return segments;
    }

    /**
     * Extrai cada tabela individualmente usando Regex, ignorando sujeira fora das tags.
     */
    private List<TextSegment> splitTablesPart(String tablesText, Metadata metadata) {
        List<TextSegment> segments = new ArrayList<>();

        // Regex para pegar tudo que está entre [START_TABLE] e [END_TABLE]
        // Escape only opening brackets; closing ones are literal ']' and don't need escaping in the Java string
        Pattern pattern = Pattern.compile("\\[START_TABLE](.*?)\\[END_TABLE]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(tablesText);

        while (matcher.find()) {
            String tableContent = matcher.group(0).trim(); // group(0) inclui as tags
            if (!isBlank(tableContent)) {
                // Limpa espaços em branco excessivos dentro da tabela (comum em PDF)
                tableContent = tableContent.replaceAll(" {2,}", " ");
                segments.add(TextSegment.from(tableContent, metadata));
            }
        }

        return segments;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}