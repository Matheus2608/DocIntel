package dev.matheus.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.matheus.entity.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD RED Phase - Integration tests for HypotheticalQuestionService with DocumentChunk entities
 * 
 * T089: Generate embeddings for DocumentChunks
 * T093: Link hypothetical questions to chunks via ChunkEmbedding
 * 
 * These tests define the requirements for integrating HypotheticalQuestionService
 * with the new DocumentChunk and ChunkEmbedding entities.
 * 
 * Expected behavior:
 * - Read from DocumentChunk entities (not TextSegments directly)
 * - Generate hypothetical questions for each chunk
 * - Create ChunkEmbedding entities to link chunks to embeddings
 * - Store embeddings in EmbeddingStore<TextSegment> (backward compatible)
 * - Support retrieval using embedded questions
 */
@QuarkusTest
class HypotheticalQuestionServiceIntegrationTest {

    @Inject
    HypotheticalQuestionService hypotheticalQuestionService;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    /**
     * T089: Verify hypothetical questions generated and linked via ChunkEmbedding
     * 
     * Given: A processed DocumentFile with chunks
     * When: Generating embeddings
     * Then: ChunkEmbedding entities created
     * And: Embeddings linked to EmbeddingStore
     * And: Can retrieve via similarity search
     */
    @Test
    @Transactional
    void shouldGenerateEmbeddingsForDocumentChunks() {
        // Given: A processed DocumentFile with chunks
        DocumentFile doc = createProcessedDocument();
        List<DocumentChunk> chunks = DocumentChunk.list("documentFile.id", doc.id);
        assertThat(chunks)
                .as("Test document should have chunks")
                .hasSize(5);
        
        // When: Generating embeddings
        hypotheticalQuestionService.generateEmbeddings(doc.id);
        
        // Then: ChunkEmbedding entities created
        List<ChunkEmbedding> embeddings = ChunkEmbedding.list("chunk.documentFile.id", doc.id);
        assertThat(embeddings)
                .as("Hypothetical questions should create ChunkEmbedding entities")
                .isNotEmpty();
        
        // Verify each chunk has at least one embedding
        for (DocumentChunk chunk : chunks) {
            List<ChunkEmbedding> chunkEmbeddings = ChunkEmbedding.list("chunk.id", chunk.id);
            assertThat(chunkEmbeddings)
                    .as("Each chunk should have at least one embedding (hypothetical question)")
                    .isNotEmpty();
        }
        
        // And: Embeddings linked to EmbeddingStore (verify via search)
        Embedding queryEmbedding = embeddingModel.embed("sample query").content();
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(
            EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(5)
                .minScore(0.0)
                .build()
        );
        List<EmbeddingMatch<TextSegment>> results = searchResult.matches();
        
        assertThat(results)
                .as("EmbeddingStore should contain the embedded questions")
                .isNotEmpty();
    }

    /**
     * T093: Verify ChunkEmbedding properly links questions to chunks
     * 
     * Given: A DocumentChunk
     * When: Generating hypothetical questions
     * Then: Questions created and linked via ChunkEmbedding
     * And: Each ChunkEmbedding has embeddingId
     */
    @Test
    @Transactional
    void shouldLinkHypotheticalQuestionsToChunks() {
        // Given: A DocumentChunk
        DocumentChunk chunk = createTestChunk();
        
        // When: Generating hypothetical questions
        hypotheticalQuestionService.generateEmbeddingsInTransaction(chunk);
        
        // Then: Questions created and linked via ChunkEmbedding
        List<ChunkEmbedding> chunkEmbeddings = ChunkEmbedding.list("chunk.id", chunk.id);
        
        // Extract question embeddings
        List<ChunkEmbedding> questionEmbeddings = chunkEmbeddings.stream()
                .filter(e -> "HYPOTHETICAL_QUESTION".equals(e.embeddingType))
                .toList();
        
        assertThat(questionEmbeddings)
                .as("Should generate hypothetical question embeddings for chunk")
                .isNotEmpty();
        
        assertThat(chunkEmbeddings)
                .as("Should create multiple ChunkEmbedding entities")
                .hasSizeGreaterThanOrEqualTo(2); // At least CONTENT + one HYPOTHETICAL_QUESTION
        
        // And: Each ChunkEmbedding has embeddingId
        for (ChunkEmbedding emb : chunkEmbeddings) {
            assertThat(emb.embeddingId)
                    .as("ChunkEmbedding must have embeddingId to link to vector store")
                    .isNotBlank();
            
            assertThat(emb.chunk.id)
                    .as("ChunkEmbedding must reference the source chunk")
                    .isEqualTo(chunk.id);
            
            assertThat(emb.embeddingType)
                    .as("Should be marked as HYPOTHETICAL_QUESTION type")
                    .isEqualTo("HYPOTHETICAL_QUESTION");
        }
    }

    /**
     * T089: Verify both chunk content and questions are embedded
     * 
     * Given: A DocumentChunk
     * When: Generating embeddings
     * Then: Both content embedding and question embeddings are created
     */
    @Test
    @Transactional
    void shouldEmbedBothContentAndQuestions() {
        // Given: A DocumentChunk
        DocumentChunk chunk = createTestChunk();
        
        // When: Generating embeddings
        hypotheticalQuestionService.generateEmbeddingsInTransaction(chunk);
        
        // Then: Multiple embedding types created
        List<ChunkEmbedding> embeddings = ChunkEmbedding.list("chunk.id", chunk.id);
        
        boolean hasContentEmbedding = embeddings.stream()
                .anyMatch(e -> "CONTENT".equals(e.embeddingType));
        
        boolean hasQuestionEmbedding = embeddings.stream()
                .anyMatch(e -> "HYPOTHETICAL_QUESTION".equals(e.embeddingType));
        
        assertThat(hasContentEmbedding)
                .as("Chunk content should be embedded directly")
                .isTrue();
        
        assertThat(hasQuestionEmbedding)
                .as("Hypothetical questions should be embedded")
                .isTrue();
    }

    /**
     * T093: Verify embeddings can be traced back to source chunks
     * 
     * Given: Multiple chunks with embeddings
     * When: Finding relevant content
     * Then: Can trace back to source DocumentChunk
     */
    @Test
    @Transactional
    void shouldTraceEmbeddingsBackToSourceChunks() {
        // Given: Multiple chunks with embeddings
        DocumentFile doc = createProcessedDocument();
        hypotheticalQuestionService.generateEmbeddings(doc.id);
        
        // When: Finding relevant content
        String query = "What is the pricing structure?";
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(
            EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(3)
                .minScore(0.0)
                .build()
        );
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();
        
        assertThat(matches)
                .as("Should find relevant embedded content")
                .isNotEmpty();
        
        // Then: Can trace back to source DocumentChunk
        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();
            String embeddingId = match.embeddingId(); // Assuming EmbeddingMatch exposes ID
            
            // Find ChunkEmbedding by embeddingId
            ChunkEmbedding chunkEmbedding = ChunkEmbedding.find("embeddingId", embeddingId).firstResult();
            
            if (chunkEmbedding != null) {
                assertThat(chunkEmbedding.chunk)
                        .as("ChunkEmbedding should link to source DocumentChunk")
                        .isNotNull();
                
                assertThat(chunkEmbedding.chunk.documentFile)
                        .as("Should be able to navigate to source DocumentFile")
                        .isNotNull();
                
                assertThat(chunkEmbedding.chunk.documentFile.id)
                        .as("Should link back to the processed document")
                        .isEqualTo(doc.id);
            }
        }
    }

    /**
     * T089: Verify table chunks generate table-specific questions
     * 
     * Given: A table chunk
     * When: Generating questions
     * Then: Questions are table-appropriate
     */
    @Test
    @Transactional
    void shouldGenerateTableSpecificQuestions() {
        // Given: A table chunk
        DocumentChunk tableChunk = createTestTableChunk();
        
        // When: Generating questions
        List<String> questions = hypotheticalQuestionService.generateQuestions(tableChunk);
        
        // Then: Questions generated
        assertThat(questions)
                .as("Table chunks should generate questions")
                .isNotEmpty();
        
        // Verify questions are stored with correct type
        List<ChunkEmbedding> embeddings = ChunkEmbedding.list("chunk.id", tableChunk.id);
        
        assertThat(embeddings)
                .as("Table chunk questions should be embedded")
                .isNotEmpty();
    }

    /**
     * T093: Verify multiple embeddings per chunk are supported
     * 
     * Given: A chunk with multiple hypothetical questions
     * When: Questions are embedded
     * Then: Multiple ChunkEmbedding entities link to same chunk
     */
    @Test
    @Transactional
    void shouldSupportMultipleEmbeddingsPerChunk() {
        // Given: A chunk
        DocumentChunk chunk = createTestChunk();
        
        // When: Generating multiple questions
        List<String> questions = hypotheticalQuestionService.generateQuestions(chunk);
        
        // Assume at least 3 questions generated
        assertThat(questions)
                .as("Should generate multiple questions per chunk")
                .hasSizeGreaterThanOrEqualTo(3);
        
        // Then: Multiple ChunkEmbedding entities created
        List<ChunkEmbedding> embeddings = ChunkEmbedding.list("chunk.id", chunk.id);
        
        assertThat(embeddings.size())
                .as("Each question should create separate ChunkEmbedding")
                .isGreaterThanOrEqualTo(questions.size());
        
        // Verify all link to same chunk
        for (ChunkEmbedding embedding : embeddings) {
            assertThat(embedding.chunk.id)
                    .as("All embeddings should reference the same chunk")
                    .isEqualTo(chunk.id);
        }
    }

    /**
     * T089: Verify batch processing for all chunks in a document
     * 
     * Given: A document with multiple chunks
     * When: Processing entire document
     * Then: All chunks are processed and embedded
     */
    @Test
    @Transactional
    void shouldProcessAllChunksInDocument() {
        // Given: A document with multiple chunks
        DocumentFile doc = createProcessedDocument();
        List<DocumentChunk> chunks = DocumentChunk.list("documentFile.id", doc.id);
        int chunkCount = chunks.size();
        
        // When: Processing entire document
        hypotheticalQuestionService.generateEmbeddings(doc.id);
        
        // Then: All chunks have embeddings
        for (DocumentChunk chunk : chunks) {
            List<ChunkEmbedding> embeddings = ChunkEmbedding.list("chunk.id", chunk.id);
            assertThat(embeddings)
                    .as("Chunk at position " + chunk.position + " should have embeddings")
                    .isNotEmpty();
        }
        
        // Verify total embedding count
        List<ChunkEmbedding> allEmbeddings = ChunkEmbedding.list("chunk.documentFile.id", doc.id);
        assertThat(allEmbeddings.size())
                .as("Total embeddings should be >= number of chunks (content + questions)")
                .isGreaterThanOrEqualTo(chunkCount);
    }

    // Helper methods

    private DocumentFile createProcessedDocument() {
        Chat chat = new Chat();
        chat.title = "Test Chat";
        chat.persist();
        
        DocumentFile doc = new DocumentFile();
        doc.chat = chat;
        doc.fileName = "test-processed.pdf";
        doc.fileType = "application/pdf";
        doc.fileSize = 1024L;
        doc.fileData = new byte[]{1, 2, 3};
        doc.processingStatus = ProcessingStatus.COMPLETED;
        doc.chunkCount = 5;
        doc.processorVersion = "docling-serve-v1.9.0";
        doc.persist();
        
        // Create test chunks
        for (int i = 0; i < 5; i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.documentFile = doc;
            chunk.position = i;
            chunk.content = "This is test content for chunk " + i + ". It contains important information about the document.";
            chunk.contentType = ContentType.TEXT;
            chunk.tokenCount = 20;
            chunk.persist();
        }
        
        return doc;
    }

    private DocumentChunk createTestChunk() {
        Chat chat = new Chat();
        chat.title = "Test Chat";
        chat.persist();
        
        DocumentFile doc = new DocumentFile();
        doc.chat = chat;
        doc.fileName = "test-single-chunk.pdf";
        doc.fileType = "application/pdf";
        doc.fileSize = 512L;
        doc.fileData = new byte[]{1, 2, 3};
        doc.processingStatus = ProcessingStatus.COMPLETED;
        doc.persist();
        
        DocumentChunk chunk = new DocumentChunk();
        chunk.documentFile = doc;
        chunk.position = 0;
        chunk.content = "This is a test paragraph about machine learning algorithms. " +
                        "It discusses neural networks, deep learning, and their applications.";
        chunk.contentType = ContentType.TEXT;
        chunk.tokenCount = 30;
        chunk.persist();
        
        return chunk;
    }

    private DocumentChunk createTestTableChunk() {
        Chat chat = new Chat();
        chat.title = "Test Chat";
        chat.persist();
        
        DocumentFile doc = new DocumentFile();
        doc.chat = chat;
        doc.fileName = "test-table.pdf";
        doc.fileType = "application/pdf";
        doc.fileSize = 512L;
        doc.fileData = new byte[]{1, 2, 3};
        doc.processingStatus = ProcessingStatus.COMPLETED;
        doc.persist();
        
        DocumentChunk chunk = new DocumentChunk();
        chunk.documentFile = doc;
        chunk.position = 0;
        chunk.content = "| Product | Price | Quantity |\n" +
                        "| --- | --- | --- |\n" +
                        "| Widget A | $10.00 | 100 |\n" +
                        "| Widget B | $15.00 | 50 |\n";
        chunk.contentType = ContentType.TABLE;
        chunk.tokenCount = 25;
        chunk.persist();
        
        return chunk;
    }
}
