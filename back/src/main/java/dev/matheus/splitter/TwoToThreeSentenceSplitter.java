package dev.matheus.splitter;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class TwoToThreeSentenceSplitter implements DocumentSplitter {

    @Override
    public List<TextSegment> split(Document document) {
        String[] sentences = document.text().split("(?<=[.!?])\\s+");
        List<TextSegment> segments = new ArrayList<>();

        for (int i = 0; i < sentences.length; i += 2) {
            int endIndex = Math.min(i + 3, sentences.length);
            int actualSentences = endIndex - i;

            // Se sobrar apenas 1 frase, adiciona ao segmento anterior
            if (actualSentences == 1 && !segments.isEmpty()) {
                TextSegment last = segments.remove(segments.size() - 1);
                String combined = last.text() + " " + sentences[i];
                segments.add(TextSegment.from(combined, last.metadata()));
            } else {
                String segment = String.join(" ",
                        Arrays.copyOfRange(sentences, i, endIndex));
                segments.add(TextSegment.from(segment,
                        document.metadata()));
            }
        }

        return segments;
    }
}