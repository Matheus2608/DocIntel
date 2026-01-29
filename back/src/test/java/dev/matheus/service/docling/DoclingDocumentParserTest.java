package dev.matheus.service.docling;

import dev.matheus.entity.ContentType;
import dev.matheus.entity.DocumentChunk;
import dev.matheus.entity.DocumentFile;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for DoclingDocumentParser - User Story 1: PDF Table Extraction
 * 
 * TDD RED Phase: These tests MUST FAIL because DoclingDocumentParser does not exist yet.
 * 
 * Tests verify:
 * - T017: PDF table extraction with structure preserved
 * - T018: Markdown table syntax validation
 */
@QuarkusTest
class DoclingDocumentParserTest {

    @Inject
    DoclingDocumentParser doclingDocumentParser;

    /**
     * T017: Unit test for PDF table extraction
     * 
     * User Story 1, Acceptance Scenario 1:
     * Given a user uploads a PDF with a pricing table,
     * When the system processes it,
     * Then the markdown output MUST contain valid markdown table syntax with headers and rows preserved
     */
    @Test
    void shouldExtractTablesFromPdfWithValidMarkdownSyntax() {
        // Arrange
        DocumentFile documentFile = createTestDocumentFile("test-pdf-with-tables.pdf");
        byte[] pdfContent = loadTestPdfWithTables();

        // Act
        List<DocumentChunk> chunks = doclingDocumentParser.parse(documentFile, pdfContent);

        // Assert
        assertThat(chunks).isNotEmpty();
        
        // Find chunk containing table
        DocumentChunk tableChunk = chunks.stream()
                .filter(chunk -> chunk.contentType == ContentType.TABLE || 
                                 chunk.content.contains("|"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No table chunk found"));

        // Verify table has markdown pipe syntax
        assertThat(tableChunk.content)
                .as("Table chunk must contain pipe-delimited markdown table")
                .contains("|");

        // Verify table has header separator (e.g., |---|---|)
        assertThat(tableChunk.content)
                .as("Table must have markdown header separator")
                .containsPattern("\\|[-:]+\\|");

        // Verify table has at least 2 rows (header + data)
        long pipeRowCount = tableChunk.content.lines()
                .filter(line -> line.contains("|"))
                .count();
        assertThat(pipeRowCount)
                .as("Table must have at least header and data rows")
                .isGreaterThanOrEqualTo(2);

        // Verify content type is marked as TABLE or MIXED
        assertThat(tableChunk.contentType)
                .as("Chunk containing table must be marked as TABLE or MIXED")
                .isIn(ContentType.TABLE, ContentType.MIXED);
    }

    /**
     * T017: Unit test for multi-page table handling
     * 
     * User Story 1, Acceptance Scenario 3:
     * Given a complex table spanning multiple PDF pages,
     * When the system processes it,
     * Then the entire table MUST be kept together in a single chunk to preserve semantic cohesion
     */
    @Test
    void shouldKeepMultiPageTableTogetherInSingleChunk() {
        // Arrange
        DocumentFile documentFile = createTestDocumentFile("test-multipage-table.pdf");
        byte[] pdfContent = createPdfWithMultiPageTable();

        // Act
        List<DocumentChunk> chunks = doclingDocumentParser.parse(documentFile, pdfContent);

        // Assert - table should not be split across multiple chunks
        List<DocumentChunk> tableChunks = chunks.stream()
                .filter(chunk -> chunk.contentType == ContentType.TABLE || 
                                 chunk.content.contains("|"))
                .toList();

        // The multi-page table should be in a single chunk
        assertThat(tableChunks)
                .as("Multi-page table should be kept together in single chunk")
                .hasSizeLessThanOrEqualTo(1);

        if (!tableChunks.isEmpty()) {
            DocumentChunk tableChunk = tableChunks.get(0);
            
            // Verify the table is complete (has both header and multiple data rows)
            long tableRows = tableChunk.content.lines()
                    .filter(line -> line.contains("|"))
                    .count();
            
            assertThat(tableRows)
                    .as("Multi-page table should have all rows preserved")
                    .isGreaterThan(10); // Multi-page table should have many rows
        }
    }

    /**
     * T018: Unit test for markdown table syntax validation
     * 
     * Validates that extracted tables conform to standard markdown table format:
     * - Pipe-delimited columns
     * - Header separator row with dashes
     * - Consistent column count across rows
     */
    @Test
    void shouldGenerateValidMarkdownTableSyntax() {
        // Arrange
        DocumentFile documentFile = createTestDocumentFile("test-simple-table.pdf");
        byte[] pdfContent = createPdfWithSimpleTable();

        // Act
        List<DocumentChunk> chunks = doclingDocumentParser.parse(documentFile, pdfContent);

        // Assert
        DocumentChunk tableChunk = chunks.stream()
                .filter(chunk -> chunk.content.contains("|"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No table found in output"));

        String tableMarkdown = tableChunk.content;
        String[] lines = tableMarkdown.split("\n");

        // Verify at least 3 lines (header, separator, data)
        assertThat(lines)
                .as("Valid markdown table must have at least 3 lines")
                .hasSizeGreaterThanOrEqualTo(3);

        // Verify header row has pipes
        assertThat(lines[0])
                .as("Header row must have pipe delimiters")
                .contains("|");

        // Verify separator row (second line should be |---|---|)
        boolean hasSeparator = false;
        for (String line : lines) {
            if (line.matches("^\\s*\\|[-:| ]+\\|\\s*$")) {
                hasSeparator = true;
                break;
            }
        }
        assertThat(hasSeparator)
                .as("Table must have markdown separator row (|---|---|)")
                .isTrue();

        // Verify consistent column count
        List<String> tableRows = tableMarkdown.lines()
                .filter(line -> line.contains("|"))
                .toList();
        
        if (tableRows.size() > 1) {
            int expectedColumns = countColumns(tableRows.get(0));
            
            for (String row : tableRows) {
                int actualColumns = countColumns(row);
                assertThat(actualColumns)
                        .as("All table rows must have consistent column count")
                        .isEqualTo(expectedColumns);
            }
        }
    }

    /**
     * T018: Test handling of PDF with both text and tables
     * 
     * User Story 1, Acceptance Scenario 2:
     * Given a PDF with both text paragraphs and inline tables,
     * When the system chunks the content,
     * Then table context MUST be maintained in chunks that reference it
     */
    @Test
    void shouldPreserveContextAroundTables() {
        // Arrange
        DocumentFile documentFile = createTestDocumentFile("test-text-with-tables.pdf");
        byte[] pdfContent = createPdfWithTextAndTables();

        // Act
        List<DocumentChunk> chunks = doclingDocumentParser.parse(documentFile, pdfContent);

        // Assert
        assertThat(chunks).hasSizeGreaterThan(1);

        // Find chunk with table
        DocumentChunk tableChunk = chunks.stream()
                .filter(chunk -> chunk.content.contains("|"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No table chunk found"));

        // Verify the table chunk also contains surrounding context (not just raw table)
        // OR verify adjacent chunks reference the table context
        boolean hasContext = tableChunk.content.split("\n").length > 5 || // Has more than just table rows
                             chunks.stream().anyMatch(c -> c.position == tableChunk.position - 1 || 
                                                           c.position == tableChunk.position + 1);

        assertThat(hasContext)
                .as("Table chunk should maintain context with surrounding text")
                .isTrue();
    }

    /**
     * Test error handling for invalid PDF
     */
    @Test
    void shouldThrowExceptionForInvalidPdf() {
        // Arrange
        DocumentFile documentFile = createTestDocumentFile("invalid.pdf");
        byte[] invalidContent = "This is not a PDF".getBytes();

        // Act & Assert
        assertThatThrownBy(() -> doclingDocumentParser.parse(documentFile, invalidContent))
                .as("Parser should throw exception for invalid PDF")
                .isInstanceOf(Exception.class);
    }

    /**
     * Test handling of empty PDF
     */
    @Test
    void shouldHandleEmptyPdfGracefully() {
        // Arrange
        DocumentFile documentFile = createTestDocumentFile("empty.pdf");
        byte[] emptyPdf = createEmptyPdf();

        // Act
        List<DocumentChunk> chunks = doclingDocumentParser.parse(documentFile, emptyPdf);

        // Assert - empty PDF should return empty list, not throw exception
        assertThat(chunks)
                .as("Empty PDF should return empty chunk list")
                .isEmpty();
    }

    // Helper methods

    private DocumentFile createTestDocumentFile(String filename) {
        DocumentFile documentFile = new DocumentFile();
        documentFile.id = "test-doc-" + System.currentTimeMillis();
        documentFile.fileName = filename;
        documentFile.fileType = "application/pdf";
        documentFile.fileSize = 1024L;
        return documentFile;
    }

    private byte[] loadTestPdfWithTables() {
        // Try to load real test fixture, fallback to placeholder if not available
        try {
            Path fixturePath = Path.of("src/test/resources/fixtures/test-pdf-with-tables.pdf");
            if (Files.exists(fixturePath)) {
                return Files.readAllBytes(fixturePath);
            }
        } catch (Exception e) {
            // Fall through to placeholder
        }
        
        // Return placeholder - tests should handle gracefully
        return createSimplePdfPlaceholder();
    }

    private byte[] createPdfWithMultiPageTable() {
        // Placeholder for multi-page table PDF
        return createSimplePdfPlaceholder();
    }

    private byte[] createPdfWithSimpleTable() {
        // Placeholder for simple table PDF
        return createSimplePdfPlaceholder();
    }

    private byte[] createPdfWithTextAndTables() {
        // Placeholder for mixed content PDF
        return createSimplePdfPlaceholder();
    }

    private byte[] createEmptyPdf() {
        // Minimal valid PDF structure (empty)
        return "%PDF-1.4\n%%EOF".getBytes();
    }

    private byte[] createSimplePdfPlaceholder() {
        // Minimal valid PDF with placeholder content
        return "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj\nxref\n0 4\n0000000000 65535 f\n0000000009 00000 n\n0000000052 00000 n\n0000000101 00000 n\ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n190\n%%EOF".getBytes();
    }

    private int countColumns(String tableRow) {
        // Count pipe delimiters (columns = pipes - 1 for standard markdown tables)
        long pipes = tableRow.chars().filter(ch -> ch == '|').count();
        return (int) pipes - 1;
    }
}
