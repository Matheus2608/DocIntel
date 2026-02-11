package dev.matheus.service;

import dev.matheus.entity.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full pipeline integration test using real PDF file (GuiaDoAtleta2025.pdf)
 * 
 * Tests the complete flow:
 * 1. Document upload
 * 2. Docling parsing
 * 3. Chunk creation and persistence
 * 4. Embedding generation
 * 5. Status transitions
 * 
 * Requires Docling Serve container to be running.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullPipelineIntegrationTest {

    private static final String TEST_PDF_PATH = "src/test/resources/files/GuiaDoAtleta2025.pdf";
    
    @Inject
    DocumentIngestionService documentIngestionService;
    
    @Inject
    HypotheticalQuestionService hypotheticalQuestionService;
    
    @Inject
    ChatService chatService;
    
    private static GenericContainer<?> doclingContainer;
    
    @BeforeAll
    static void startDoclingContainer() {
        // Check if Docker is available
        try {
            DockerClientFactory.instance().client();
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Docker is not available, skipping test");
        }
        
        // Start Docling container if not already running
        try {
            doclingContainer = new GenericContainer<>("ghcr.io/docling-project/docling-serve:v1.9.0")
                    .withExposedPorts(5001)
                    .withEnv("LOG_LEVEL", "INFO")
                    .withCreateContainerCmdModifier(cmd -> cmd
                            .withHostConfig(cmd.getHostConfig()
                                    .withMemory(4L * 1024 * 1024 * 1024)  // 4GB
                                    .withMemorySwap(4L * 1024 * 1024 * 1024)  // 4GB swap
                                    .withCpuCount(2L)))
                    .waitingFor(Wait.forHttp("/health")
                            .forStatusCode(200)
                            .withStartupTimeout(java.time.Duration.ofMinutes(3)));
            
            doclingContainer.start();
            
            // Map container port to local port 5001
            Integer mappedPort = doclingContainer.getMappedPort(5001);
            System.out.println("Docling container started on port: " + mappedPort);
            
            // Update system property for Quarkus config
            System.setProperty("quarkus.docling.base-url", 
                    "http://localhost:" + mappedPort);
            
        } catch (Exception e) {
            System.err.println("Failed to start Docling container: " + e.getMessage());
            Assumptions.assumeTrue(false, "Could not start Docling container");
        }
    }
    
    @AfterAll
    static void stopDoclingContainer() {
        if (doclingContainer != null) {
            doclingContainer.stop();
        }
    }
    
    /**
     * Test 1: Create chat with PDF upload
     */
    @Test
    @Order(1)
    @Transactional
    void shouldUploadPdfAndCreateChat() throws IOException {
        // Given: PDF file
        Path pdfPath = Path.of(TEST_PDF_PATH);
        assertThat(pdfPath).exists();
        
        byte[] pdfBytes = Files.readAllBytes(pdfPath);
        assertThat(pdfBytes).isNotEmpty();
        
        System.out.println("PDF file loaded: " + pdfBytes.length + " bytes");
        
        // When: Creating chat (this triggers full pipeline)
        var chatResponse = chatService.createChat(pdfBytes, "GuiaDoAtleta2025.pdf", "application/pdf");
        
        // Then: Chat created successfully
        assertThat(chatResponse).isNotNull();
        assertThat(chatResponse.id()).isNotBlank();
        assertThat(chatResponse.hasDocument()).isTrue();
        
        System.out.println("Chat created: " + chatResponse.id());
        
        // Verify document was created
        DocumentFile doc = DocumentFile.find("fileName", "GuiaDoAtleta2025.pdf").firstResult();
        assertThat(doc).isNotNull();
        assertThat(doc.processingStatus).isEqualTo(ProcessingStatus.COMPLETED);
        assertThat(doc.chunkCount).isGreaterThan(0);
        assertThat(doc.processorVersion).isEqualTo("docling-serve-v1.9.0");
        
        System.out.println("Document processed: " + doc.id + " with " + doc.chunkCount + " chunks");
    }
    
    /**
     * Test 2: Verify chunks were created
     */
    @Test
    @Order(2)
    @Transactional
    void shouldCreateDocumentChunks() {
        // Given: Document exists
        DocumentFile doc = DocumentFile.find("fileName", "GuiaDoAtleta2025.pdf").firstResult();
        assertThat(doc).isNotNull();
        
        // When: Querying chunks
        List<DocumentChunk> chunks = DocumentChunk.list("documentFile.id", doc.id);
        
        // Then: Chunks created
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.size()).isEqualTo(doc.chunkCount);
        
        System.out.println("Found " + chunks.size() + " chunks");
        
        // Verify chunk properties
        for (DocumentChunk chunk : chunks) {
            assertThat(chunk.content).isNotBlank();
            assertThat(chunk.contentType).isNotNull();
            assertThat(chunk.position).isGreaterThanOrEqualTo(0);
            assertThat(chunk.tokenCount).isGreaterThan(0);
        }
        
        // Check for different content types
        long textChunks = chunks.stream()
                .filter(c -> c.contentType == ContentType.TEXT)
                .count();
        long tableChunks = chunks.stream()
                .filter(c -> c.contentType == ContentType.TABLE)
                .count();
        
        System.out.println("Text chunks: " + textChunks);
        System.out.println("Table chunks: " + tableChunks);
        
        assertThat(textChunks).isGreaterThan(0);
    }
    
    /**
     * Test 3: Verify embeddings were generated
     */
    @Test
    @Order(3)
    @Transactional
    void shouldGenerateEmbeddings() {
        // Given: Document with chunks
        DocumentFile doc = DocumentFile.find("fileName", "GuiaDoAtleta2025.pdf").firstResult();
        assertThat(doc).isNotNull();
        
        List<DocumentChunk> chunks = DocumentChunk.list("documentFile.id", doc.id);
        assertThat(chunks).isNotEmpty();
        
        // When: Querying embeddings
        List<ChunkEmbedding> embeddings = ChunkEmbedding.list(
                "chunk.documentFile.id", doc.id);
        
        // Then: Embeddings created
        assertThat(embeddings).isNotEmpty();
        
        System.out.println("Found " + embeddings.size() + " embeddings");
        
        // Each chunk should have at least 2 embeddings: CONTENT + HYPOTHETICAL_QUESTION(s)
        assertThat(embeddings.size()).isGreaterThanOrEqualTo(chunks.size() * 2);
        
        // Verify embedding types
        long contentEmbeddings = embeddings.stream()
                .filter(e -> "CONTENT".equals(e.embeddingType))
                .count();
        long questionEmbeddings = embeddings.stream()
                .filter(e -> "HYPOTHETICAL_QUESTION".equals(e.embeddingType))
                .count();
        
        System.out.println("Content embeddings: " + contentEmbeddings);
        System.out.println("Question embeddings: " + questionEmbeddings);
        
        assertThat(contentEmbeddings).isEqualTo(chunks.size());
        assertThat(questionEmbeddings).isGreaterThan(0);
        
        // Verify all embeddings have IDs
        for (ChunkEmbedding embedding : embeddings) {
            assertThat(embedding.embeddingId).isNotBlank();
            assertThat(embedding.chunk).isNotNull();
            assertThat(embedding.createdAt).isNotNull();
        }
    }
    
    /**
     * Test 4: Verify status transitions
     */
    @Test
    @Order(4)
    @Transactional
    void shouldHaveCorrectProcessingStatus() {
        // Given: Document exists
        DocumentFile doc = DocumentFile.find("fileName", "GuiaDoAtleta2025.pdf").firstResult();
        assertThat(doc).isNotNull();
        
        // Then: Status is COMPLETED
        assertThat(doc.processingStatus).isEqualTo(ProcessingStatus.COMPLETED);
        assertThat(doc.processedAt).isNotNull();
        assertThat(doc.processingError).isNullOrEmpty();
        assertThat(doc.chunkCount).isGreaterThan(0);
        assertThat(doc.processorVersion).isEqualTo("docling-serve-v1.9.0");
        
        System.out.println("Document status: " + doc.processingStatus);
        System.out.println("Processed at: " + doc.processedAt);
        System.out.println("Chunk count: " + doc.chunkCount);
        System.out.println("Processor version: " + doc.processorVersion);
    }
    
    /**
     * Test 5: Verify chunk content quality
     */
    @Test
    @Order(5)
    @Transactional
    void shouldHaveQualityChunkContent() {
        // Given: Document with chunks
        DocumentFile doc = DocumentFile.find("fileName", "GuiaDoAtleta2025.pdf").firstResult();
        assertThat(doc).isNotNull();
        
        List<DocumentChunk> chunks = DocumentChunk.list("documentFile.id", doc.id);
        assertThat(chunks).isNotEmpty();
        
        // Then: Verify content quality
        for (DocumentChunk chunk : chunks) {
            // Content should not be empty or just whitespace
            assertThat(chunk.content.trim()).isNotEmpty();
            
            // Content should have reasonable length (at least 10 characters)
            assertThat(chunk.content.length()).isGreaterThan(10);
            
            // Token count should be positive
            assertThat(chunk.tokenCount).isPositive();
            
            // Token count should be reasonable (not exceed max)
            assertThat(chunk.tokenCount).isLessThanOrEqualTo(3000);
        }
        
        // Verify chunks are ordered by position
        for (int i = 0; i < chunks.size() - 1; i++) {
            assertThat(chunks.get(i).position)
                    .isLessThan(chunks.get(i + 1).position);
        }
        
        System.out.println("Chunk content quality verified");
    }
    
    /**
     * Test 6: Performance metrics
     */
    @Test
    @Order(6)
    @Transactional
    void shouldProvidePerformanceMetrics() {
        // Given: Document exists
        DocumentFile doc = DocumentFile.find("fileName", "GuiaDoAtleta2025.pdf").firstResult();
        assertThat(doc).isNotNull();
        
        List<DocumentChunk> chunks = DocumentChunk.list("documentFile.id", doc.id);
        List<ChunkEmbedding> embeddings = ChunkEmbedding.list(
                "chunk.documentFile.id", doc.id);
        
        // Calculate metrics
        int totalChunks = chunks.size();
        int totalEmbeddings = embeddings.size();
        long totalTokens = chunks.stream()
                .mapToLong(c -> c.tokenCount)
                .sum();
        
        double avgTokensPerChunk = totalTokens / (double) totalChunks;
        double embeddingsPerChunk = totalEmbeddings / (double) totalChunks;
        
        // Print metrics
        System.out.println("\n=== Performance Metrics ===");
        System.out.println("PDF Size: 91 MB");
        System.out.println("Total Chunks: " + totalChunks);
        System.out.println("Total Embeddings: " + totalEmbeddings);
        System.out.println("Total Tokens: " + totalTokens);
        System.out.println("Avg Tokens/Chunk: " + String.format("%.2f", avgTokensPerChunk));
        System.out.println("Embeddings/Chunk: " + String.format("%.2f", embeddingsPerChunk));
        System.out.println("========================\n");
        
        // Verify reasonable metrics
        assertThat(totalChunks).isGreaterThan(10); // Large PDF should have many chunks
        assertThat(avgTokensPerChunk).isBetween(100.0, 2500.0); // Reasonable chunk size
        assertThat(embeddingsPerChunk).isGreaterThanOrEqualTo(2.0); // At least CONTENT + 1 question
    }
}
