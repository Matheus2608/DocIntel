package dev.matheus.util;

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.junit.jupiter.api.Test;

import java.util.List;

class ChatUtilsTest {

    @Test
    void testGenerateNumberQuestions() {
        String fileName = "files/Manual.txt";
        String path = getClass().getClassLoader().getResource(fileName).getPath();

        var document = FileSystemDocumentLoader
                .loadDocument(path, new TextDocumentParser());

        var splitter = new DocumentByParagraphSplitter(1000, 20);
        List<TextSegment> paragraphs = splitter.split(document);

        for (TextSegment paragraphSegment : paragraphs) {
            yellowPrint("---- Paragraph ----");
            greenPrint(paragraphSegment.text());
            yellowPrint("---- Number of questions ----");
            greenPrint(String.valueOf(ChatUtils.getNumberQuestions(paragraphSegment.text())));
        }
    }


    private void yellowPrint(String text) {
        System.out.println("\u001B[33m" + text + "\u001B[0m");
    }

    private void greenPrint(String text) {
        System.out.println("\u001B[32m" + text + "\u001B[0m");
    }
}
