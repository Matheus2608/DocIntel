package dev.matheus.service.docling;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;
import dev.matheus.entity.ContentType;
import dev.matheus.entity.DocumentChunk;
import dev.matheus.entity.DocumentFile;
import dev.matheus.entity.ProcessingStatus;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for DoclingDocumentParser with Testcontainers - User Story 1: PDF Table Extraction
 * 
 * TDD REFACTOR Phase: Tests now use real Docling Serve container via Testcontainers.
 * 
 * T019: Integration test for PDF processing with real Docling Serve container
 * 
 * Tests the full integration:
 * - Docling Serve container running
 * - DoclingDocumentParser calling Docling API
 * - PDF with tables processed end-to-end
 * - Markdown table output validated
 */
@QuarkusTest
@Testcontainers // Re-enabled for REFACTOR phase - real Docling integration
class DoclingIntegrationTest {

    @Container // Re-enabled for REFACTOR phase
    static DoclingServeContainer doclingServeContainer = new DoclingServeContainer(
            DoclingServeContainerConfig.builder().build()
    );

    @Inject
    DoclingDocumentParser doclingDocumentParser;

    @BeforeAll
    static void setUp() {
        // REFACTOR phase: Start actual Docling Serve container
        // Container is started automatically by @Testcontainers annotation
        // Wait for container to be ready
        if (doclingServeContainer != null && doclingServeContainer.isRunning()) {
            // Configure Quarkus to use the container's URL
            String doclingUrl = doclingServeContainer.getApiUrl();
            
            System.setProperty("quarkus.rest-client.docling-api.url", doclingUrl);
            System.setProperty("docling.serve.url", doclingUrl);
        }
    }

    /**
     * T019: Full integration test with real PDF and Docling Serve container
     * 
     * User Story 1 - End-to-End Scenario:
     * Given a PDF with pricing table is uploaded,
     * When the system processes it through Docling,
     * Then the output chunks contain valid markdown tables with preserved structure
     */
    @Test
    void shouldProcessPdfWithTablesEndToEnd() {
        // Arrange
        DocumentFile documentFile = createTestDocumentFile("integration-test.pdf");
        byte[] pdfContent = loadRealPdfWithTable();

        // Act
        List<DocumentChunk> chunks = doclingDocumentParser.parse(documentFile, pdfContent);

        // Assert - Basic chunk validation
        assertThat(chunks)
                .as("Parser should return at least one chunk")
                .isNotEmpty();

        // Assert - All chunks have required fields
        for (DocumentChunk chunk : chunks) {
            assertThat(chunk.content)
                    .as("Chunk content must not be null or empty")
                    .isNotBlank();
            
            assertThat(chunk.contentType)
                    .as("Chunk must have content type")
                    .isNotNull();
            
            assertThat(chunk.position)
                    .as("Chunk must have position")
                    .isNotNull()
                    .isGreaterThanOrEqualTo(0);
            
            assertThat(chunk.tokenCount)
                    .as("Chunk must have token count")
                    .isNotNull()
                    .isGreaterThan(0);
        }

        // Assert - Find table chunk
        boolean hasTableChunk = chunks.stream()
                .anyMatch(chunk -> chunk.contentType == ContentType.TABLE || 
                                   chunk.content.contains("|"));

        assertThat(hasTableChunk)
                .as("Processed chunks should contain at least one table")
                .isTrue();

        // Assert - Validate table structure
        DocumentChunk tableChunk = chunks.stream()
                .filter(chunk -> chunk.content.contains("|"))
                .findFirst()
                .orElseThrow();

        String tableMarkdown = tableChunk.content;
        
        // Must have pipe-delimited format
        assertThat(tableMarkdown)
                .as("Table must use markdown pipe syntax")
                .contains("|");

        // Must have header separator row
        assertThat(tableMarkdown)
                .as("Table must have markdown separator row")
                .containsPattern("\\|[-:| ]+\\|");

        // Must have multiple rows
        long tableRows = tableMarkdown.lines()
                .filter(line -> line.trim().startsWith("|"))
                .count();
        
        assertThat(tableRows)
                .as("Table must have at least header, separator, and data rows")
                .isGreaterThanOrEqualTo(3);
    }

    /**
     * Integration test verifying table structure preservation
     */
    @Test
    void shouldPreserveTableStructureInMarkdown() {
        // Arrange
        DocumentFile documentFile = createTestDocumentFile("table-structure-test.pdf");
        byte[] pdfContent = loadRealPdfWithTable();

        // Act
        List<DocumentChunk> chunks = doclingDocumentParser.parse(documentFile, pdfContent);

        // Assert - Find table chunk
        DocumentChunk tableChunk = chunks.stream()
                .filter(chunk -> chunk.content.contains("|"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No table found in output"));

        String[] lines = tableChunk.content.split("\n");
        
        // Verify consistent column count (all rows should have same number of pipes)
        List<String> tableRows = tableChunk.content.lines()
                .filter(line -> line.trim().contains("|"))
                .toList();

        if (tableRows.size() > 2) {
            int firstRowColumns = countPipes(tableRows.get(0));
            
            for (String row : tableRows.subList(1, tableRows.size())) {
                int rowColumns = countPipes(row);
                assertThat(rowColumns)
                        .as("All table rows must have consistent column count")
                        .isEqualTo(firstRowColumns);
            }
        }
    }

    /**
     * Integration test verifying chunk metadata is populated
     */
    @Test
    void shouldPopulateChunkMetadataCorrectly() {
        // Arrange
        DocumentFile documentFile = createTestDocumentFile("metadata-test.pdf");
        byte[] pdfContent = loadRealPdfWithTable();

        // Act
        List<DocumentChunk> chunks = doclingDocumentParser.parse(documentFile, pdfContent);

        // Assert - Verify chunks are ordered
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            
            assertThat(chunk.position)
                    .as("Chunk position should be sequential")
                    .isEqualTo(i);
            
            assertThat(chunk.documentFile)
                    .as("Chunk should reference document file")
                    .isEqualTo(documentFile);
        }

        // Verify at least one chunk has content type classification
        boolean hasClassifiedContent = chunks.stream()
                .anyMatch(chunk -> chunk.contentType != null);
        
        assertThat(hasClassifiedContent)
                .as("At least one chunk should have content type classification")
                .isTrue();
    }

    /**
     * Integration test verifying error handling for corrupted PDF
     */
    @Test
    void shouldHandleCorruptedPdfGracefully() {
        // Arrange
        DocumentFile documentFile = createTestDocumentFile("corrupted.pdf");
        byte[] corruptedPdf = "This is not a valid PDF content".getBytes();

        // Act & Assert
        try {
            List<DocumentChunk> chunks = doclingDocumentParser.parse(documentFile, corruptedPdf);
            
            // If no exception thrown, should return empty list
            assertThat(chunks)
                    .as("Corrupted PDF should return empty list or throw exception")
                    .isEmpty();
        } catch (Exception e) {
            // Expected behavior - exception for invalid PDF
            assertThat(e)
                    .as("Exception should have meaningful message")
                    .hasMessageContaining("PDF");
        }
    }

    /**
     * Integration test verifying semantic chunking respects table boundaries
     */
    @Test
    void shouldNotSplitTablesAcrossChunks() {
        // Arrange
        DocumentFile documentFile = createTestDocumentFile("table-boundary-test.pdf");
        byte[] pdfContent = loadRealPdfWithTable();

        // Act
        List<DocumentChunk> chunks = doclingDocumentParser.parse(documentFile, pdfContent);

        // Assert - Check that tables are complete within chunks
        for (DocumentChunk chunk : chunks) {
            if (chunk.content.contains("|")) {
                String tableContent = chunk.content;
                
                // If chunk contains table start, it should also contain table end
                boolean hasTableStart = tableContent.lines()
                        .anyMatch(line -> line.trim().startsWith("|") && 
                                         line.trim().endsWith("|"));
                
                if (hasTableStart) {
                    // Count header separators (should be 1 per complete table)
                    long separatorCount = tableContent.lines()
                            .filter(line -> line.matches("^\\s*\\|[-:| ]+\\|\\s*$"))
                            .count();
                    
                    assertThat(separatorCount)
                            .as("Table in chunk should be complete (have separator)")
                            .isGreaterThanOrEqualTo(1);
                }
            }
        }
    }

    // Helper methods

    private DocumentFile createTestDocumentFile(String filename) {
        DocumentFile documentFile = new DocumentFile();
        documentFile.id = "test-integration-" + System.currentTimeMillis();
        documentFile.fileName = filename;
        documentFile.fileType = "application/pdf";
        documentFile.fileSize = 1024L;
        documentFile.processingStatus = ProcessingStatus.PENDING;
        return documentFile;
    }

    private byte[] loadRealPdfWithTable() {
        // Try to load real test fixture
        try {
            Path fixturePath = Path.of("src/test/resources/fixtures/test-pdf-with-tables.pdf");
            if (Files.exists(fixturePath)) {
                return Files.readAllBytes(fixturePath);
            }
            
            // Fallback to placeholder
            Path placeholderPath = Path.of("src/test/resources/fixtures/test-pdf-with-tables.pdf.placeholder");
            if (Files.exists(placeholderPath)) {
                // For integration test, we need a real PDF
                // If no real PDF available, create a minimal valid one
                return createMinimalValidPdfWithTable();
            }
        } catch (Exception e) {
            // Fall through to placeholder
        }
        
        return createMinimalValidPdfWithTable();
    }

    private byte[] createMinimalValidPdfWithTable() {
        // For integration testing, we'd ideally have a real PDF
        // This is a placeholder that returns minimal PDF structure
        // In real scenario, test fixture PDF files should be committed to repo
        String minimalPdf = "%PDF-1.4\n" +
                "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n" +
                "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n" +
                "3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj\n" +
                "xref\n0 4\n" +
                "0000000000 65535 f\n" +
                "0000000009 00000 n\n" +
                "0000000052 00000 n\n" +
                "0000000101 00000 n\n" +
                "trailer<</Size 4/Root 1 0 R>>\n" +
                "startxref\n190\n%%EOF";
        
        return minimalPdf.getBytes();
    }

    private int countPipes(String line) {
        return (int) line.chars().filter(ch -> ch == '|').count();
    }
}
