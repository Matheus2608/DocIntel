package dev.matheus.service;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;
import com.github.dockerjava.api.model.HostConfig;
import dev.matheus.entity.*;
import dev.matheus.test.util.TestFileUtils;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD RED Phase - Integration tests for Docling pipeline integration
 * 
 * T088: End-to-end document processing with Docling
 * T094: Processing status transitions
 * 
 * These tests define the requirements for integrating DoclingDocumentParser
 * with DocumentIngestionService to replace the legacy PDFBox/Tabula pipeline.
 * 
 * Expected behavior:
 * - DocumentFile starts with PENDING status
 * - processDocument() uses DoclingDocumentParser instead of parseCustomPdf()
 * - DocumentChunks are persisted to database
 * - DocumentFile status transitions: PENDING → PROCESSING → COMPLETED
 * - Error handling updates status to FAILED
 * - processorVersion is set to "docling-serve-v1.9.0"
 */
@QuarkusTest
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
class DocumentIngestionServiceIntegrationTest {

    @Container
    static DoclingServeContainer doclingServeContainer = new DoclingServeContainer(
            DoclingServeContainerConfig.builder()
                    .image("ghcr.io/docling-project/docling-serve:v1.9.0")
                    .build()
    )
    .withCreateContainerCmdModifier(cmd -> {
        HostConfig hostConfig = cmd.getHostConfig();
        if (hostConfig == null) {
            hostConfig = new HostConfig();
            cmd.withHostConfig(hostConfig);
        }
        hostConfig
            .withMemory(4L * 1024 * 1024 * 1024)
            .withMemorySwap(4L * 1024 * 1024 * 1024)
            .withCpuCount(2L);
    });

    @Inject
    DocumentIngestionService documentIngestionService;

    @BeforeAll
    static void setUp() {
        if (doclingServeContainer != null && doclingServeContainer.isRunning()) {
            String doclingUrl = doclingServeContainer.getApiUrl();
            System.setProperty("quarkus.rest-client.docling-api.url", doclingUrl);
            System.setProperty("docling.serve.url", doclingUrl);
        }
    }

    /**
     * T088: Verify end-to-end document processing creates chunks
     * 
     * Given: A DocumentFile with PENDING status
     * When: Processing document with processDocument()
     * Then: Status updated to COMPLETED
     * And: DocumentChunks created and persisted
     * And: Chunks have correct metadata
     * And: chunkCount is populated
     * And: processorVersion is set
     */
    @Test
    @Transactional
    void shouldProcessDocumentWithDocling() throws IOException {
        // Given: A DocumentFile with PENDING status
        byte[] pdfBytes = loadTestPdf();
        DocumentFile doc = createTestDocumentFile("test.pdf", pdfBytes);
        assertThat(doc.processingStatus).isEqualTo(ProcessingStatus.PENDING);
        
        // When: Processing document
        documentIngestionService.processDocument(doc.id);
        
        // Then: Status updated to COMPLETED
        doc = DocumentFile.findById(doc.id);
        assertThat(doc.processingStatus)
                .as("Document should be marked as COMPLETED after successful processing")
                .isEqualTo(ProcessingStatus.COMPLETED);
        
        assertThat(doc.processedAt)
                .as("processedAt timestamp should be set")
                .isNotNull();
        
        assertThat(doc.processorVersion)
                .as("processorVersion should indicate Docling was used")
                .isEqualTo("docling-serve-v1.9.0");
        
        // And: DocumentChunks created and persisted
        List<DocumentChunk> chunks = DocumentChunk.list("documentFile.id", doc.id);
        assertThat(chunks)
                .as("Document processing should create chunks")
                .isNotEmpty();
        
        assertThat(doc.chunkCount)
                .as("chunkCount should match number of persisted chunks")
                .isEqualTo(chunks.size());
        
        // And: Chunks have correct metadata
        DocumentChunk firstChunk = chunks.get(0);
        assertThat(firstChunk.position)
                .as("First chunk should have position 0")
                .isEqualTo(0);
        
        assertThat(firstChunk.content)
                .as("Chunk content should not be blank")
                .isNotBlank();
        
        assertThat(firstChunk.tokenCount)
                .as("Chunk should have positive token count")
                .isGreaterThan(0);
        
        assertThat(firstChunk.contentType)
                .as("Chunk should have content type classification")
                .isNotNull();
        
        assertThat(firstChunk.documentFile.id)
                .as("Chunk should be linked to DocumentFile")
                .isEqualTo(doc.id);
    }

    /**
     * T088: Verify error handling updates status to FAILED
     * 
     * Given: A DocumentFile with corrupt data
     * When: Processing fails
     * Then: Status updated to FAILED with error message
     */
    @Test
    @Transactional
    void shouldHandleProcessingFailureGracefully() {
        // Given: A DocumentFile with corrupt data
        byte[] corruptBytes = "This is not a valid PDF".getBytes();
        DocumentFile doc = createTestDocumentFile("corrupt.pdf", corruptBytes);
        final String docId = doc.id;
        
        // When: Processing fails
        assertThatThrownBy(() -> documentIngestionService.processDocument(docId))
                .as("Should throw exception for invalid PDF")
                .isInstanceOf(RuntimeException.class);
        
        // Then: Status updated to FAILED with error message
        DocumentFile failedDoc = DocumentFile.findById(docId);
        assertThat(failedDoc.processingStatus)
                .as("Failed processing should set status to FAILED")
                .isEqualTo(ProcessingStatus.FAILED);
        
        assertThat(failedDoc.processingError)
                .as("Error message should be captured")
                .isNotBlank()
                .contains("Docling");
    }

    /**
     * T094: Verify PENDING → PROCESSING → COMPLETED flow
     * 
     * Given: Document starts as PENDING
     * When: Start processing
     * Then: Status transitions through PROCESSING to COMPLETED
     */
    @Test
    @Transactional
    void shouldTransitionThroughProcessingStatuses() throws IOException {
        // Given: Document starts as PENDING
        byte[] pdfBytes = loadTestPdf();
        DocumentFile doc = createTestDocumentFile("test.pdf", pdfBytes);
        assertThat(doc.processingStatus)
                .as("New document should start as PENDING")
                .isEqualTo(ProcessingStatus.PENDING);
        
        // When: Start processing
        documentIngestionService.startProcessing(doc.id);
        assertThat(doc.processingStatus)
                .as("Document should transition to PROCESSING when started")
                .isEqualTo(ProcessingStatus.PROCESSING);
        
        // And: Complete processing (simulate chunks creation)
        List<DocumentChunk> mockChunks = List.of(
                createMockChunk(doc, 0, "First chunk content"),
                createMockChunk(doc, 1, "Second chunk content")
        );
        
        documentIngestionService.completeProcessing(doc.id, mockChunks);
        
        // Then: Status is COMPLETED
        assertThat(doc.processingStatus)
                .as("Document should be COMPLETED after processing")
                .isEqualTo(ProcessingStatus.COMPLETED);
        
        assertThat(doc.chunkCount)
                .as("chunkCount should be set on completion")
                .isEqualTo(mockChunks.size());
        
        assertThat(doc.processedAt)
                .as("processedAt should be set on completion")
                .isNotNull();
    }

    /**
     * T094: Verify PROCESSING → FAILED transition on error
     * 
     * Given: Document is PROCESSING
     * When: Error occurs
     * Then: Status transitions to FAILED with error details
     */
    @Test
    @Transactional
    void shouldTransitionToFailedOnError() {
        // Given: Document is PROCESSING
        byte[] pdfBytes = new byte[]{1, 2, 3};
        DocumentFile doc = createTestDocumentFile("test.pdf", pdfBytes);
        doc.processingStatus = ProcessingStatus.PROCESSING;
        doc.persist();
        
        // When: Error occurs
        String errorMessage = "Docling API returned 500 Internal Server Error";
        documentIngestionService.markAsFailed(doc.id, errorMessage);
        
        // Then: Status transitions to FAILED
        doc = DocumentFile.findById(doc.id);
        assertThat(doc.processingStatus)
                .as("Document should be marked as FAILED")
                .isEqualTo(ProcessingStatus.FAILED);
        
        assertThat(doc.processingError)
                .as("Error message should be stored")
                .isEqualTo(errorMessage);
        
        assertThat(doc.processedAt)
                .as("processedAt should be set even for failures")
                .isNotNull();
    }

    /**
     * T088: Verify chunks are properly linked to DocumentFile
     * 
     * Given: A processed document
     * When: Querying chunks
     * Then: All chunks are properly linked and ordered
     */
    @Test
    @Transactional
    void shouldLinkChunksToDocumentFile() throws IOException {
        // Given: A processed document
        byte[] pdfBytes = loadTestPdf();
        DocumentFile doc = createTestDocumentFile("test.pdf", pdfBytes);
        
        // When: Processing document
        documentIngestionService.processDocument(doc.id);
        
        // Then: Query chunks by document
        List<DocumentChunk> chunks = DocumentChunk.list("documentFile.id", doc.id);
        
        assertThat(chunks)
                .as("Should be able to query chunks by document ID")
                .isNotEmpty();
        
        // Verify chunks are ordered by position
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            assertThat(chunk.position)
                    .as("Chunk position should match array index")
                    .isEqualTo(i);
            
            assertThat(chunk.documentFile.id)
                    .as("Each chunk should reference the parent document")
                    .isEqualTo(doc.id);
        }
    }

    /**
     * T088: Verify table content is preserved in chunks
     * 
     * Given: A PDF with tables
     * When: Processing document
     * Then: Table chunks contain markdown table format
     */
    @Test
    @Transactional
    void shouldPreserveTableContentInChunks() throws IOException {
        // Given: A PDF with tables
        byte[] pdfBytes = loadTestPdfWithTables();
        DocumentFile doc = createTestDocumentFile("test-with-tables.pdf", pdfBytes);
        
        // When: Processing document
        documentIngestionService.processDocument(doc.id);
        
        // Then: Find table chunks
        List<DocumentChunk> chunks = DocumentChunk.list("documentFile.id", doc.id);
        
        boolean hasTableChunk = chunks.stream()
                .anyMatch(chunk -> chunk.contentType == ContentType.TABLE || 
                                   chunk.content.contains("|"));
        
        assertThat(hasTableChunk)
                .as("Document with tables should produce TABLE chunks")
                .isTrue();
        
        // Verify table format
        DocumentChunk tableChunk = chunks.stream()
                .filter(chunk -> chunk.content.contains("|"))
                .findFirst()
                .orElseThrow();
        
        assertThat(tableChunk.content)
                .as("Table chunk should contain markdown table syntax")
                .contains("|")
                .containsPattern("\\|[-:| ]+\\|"); // Header separator
    }

    // Helper methods

    private DocumentFile createTestDocumentFile(String filename, byte[] data) {
        Chat chat = new Chat();
        chat.title = "Test Chat";
        chat.persist();
        
        DocumentFile doc = new DocumentFile();
        doc.chat = chat;
        doc.fileName = filename;
        doc.fileType = "application/pdf";
        doc.fileSize = (long) data.length;
        doc.fileData = data;
        doc.processingStatus = ProcessingStatus.PENDING;
        doc.persist();
        
        return doc;
    }

    private byte[] loadTestPdf() throws IOException {
        // Try to load test fixture
        try {
            Path smallFixturePath = Path.of("src/test/resources/fixtures/test-pdf-with-tables-small.pdf");
            if (Files.exists(smallFixturePath)) {
                return Files.readAllBytes(smallFixturePath);
            }
        } catch (Exception e) {
            // Fallback to TestFileUtils
        }
        
        return TestFileUtils.readTestFile("GuiaDoAtleta2025.pdf");
    }

    private byte[] loadTestPdfWithTables() throws IOException {
        Path fixturePath = Path.of("src/test/resources/fixtures/test-pdf-with-tables.pdf");
        if (Files.exists(fixturePath)) {
            return Files.readAllBytes(fixturePath);
        }
        return loadTestPdf();
    }

    private DocumentChunk createMockChunk(DocumentFile doc, int position, String content) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.documentFile = doc;
        chunk.position = position;
        chunk.content = content;
        chunk.contentType = ContentType.TEXT;
        chunk.tokenCount = content.split("\\s+").length * 2; // Rough estimate
        return chunk;
    }
}
