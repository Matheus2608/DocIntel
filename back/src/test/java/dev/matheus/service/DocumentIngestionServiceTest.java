package dev.matheus.service;

import dev.matheus.test.util.TestFileUtils;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class DocumentIngestionServiceTest {

    @Inject
    DocumentIngestionService documentIngestionService;

    @Test
    void shouldParsePdfAndCleanText() throws IOException {
        byte[] pdfBytes = TestFileUtils.readTestFile("GuiaDoAtleta2025.pdf");
        String fileName = "GuiaDoAtleta2025.pdf";

        dev.langchain4j.data.document.Document document = documentIngestionService.parseCustomPdf(pdfBytes, fileName);

        assertNotNull(document);
        String content = document.text();

        // Check if garbage text is removed
        assertFalse(content.contains("[[LIXO_INICIO]]"));
        assertFalse(content.contains("[[LIXO_FIM]]"));

        // Check if page number from table is removed
        assertFalse(content.contains("### Tabela da Página"));

        // Check if duplicated START TABLE is removed
        long count = content.lines().filter(line -> line.contains("[START_TABLE]")).count();
        long tableCount = content.lines().filter(line -> line.contains("| --- |")).count();
        // Ensure that for each table header, there is only one START_TABLE
        // This is an approximation, a more robust check might be needed
        assert(count <= tableCount);

        System.out.println(content);
    }
}

