package dev.matheus.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.matheus.dto.EmbeddingAddRequest;
import dev.matheus.dto.EmbeddingAddResponse;
import dev.matheus.dto.EmbeddingEntryResponse;
import dev.matheus.dto.EmbeddingSearchRequest;
import dev.matheus.dto.EmbeddingSearchResponse;
import dev.matheus.dto.EmbeddingUpdateRequest;
import dev.matheus.dto.EmbeddingUpdateResponse;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmbeddingSearchService.
 * These tests verify the service's search logic, including:
 * - Using LangChain4j's EmbeddingStore for similarity search
 * - Sorting results by similarity score (descending)
 * - Respecting maxResults and minSimilarity parameters
 * - Proper mapping to EmbeddingSearchResponse
 * 
 * RED PHASE: These tests are expected to FAIL because the implementation
 * currently throws UnsupportedOperationException.
 */
@ExtendWith(MockitoExtension.class)
class EmbeddingSearchServiceTest {

    @InjectMocks
    EmbeddingSearchService embeddingSearchService;

    @Mock
    EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    EmbeddingModel embeddingModel;

    private Embedding mockQueryEmbedding;

    @BeforeEach
    void setUp() {
        // Create a mock embedding vector for queries
        mockQueryEmbedding = Embedding.from(createMockVector(384));
    }

    /**
     * Test that search uses EmbeddingModel to embed the query text
     */
    @Test
    void shouldEmbedQueryTextUsingEmbeddingModel() {
        // Arrange
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "test query",
                10,
                0.7
        );
        
        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockQueryEmbedding));
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(Collections.emptyList()));

        // Act
        embeddingSearchService.search(request);

        // Assert
        verify(embeddingModel, times(1)).embed("test query");
    }

    /**
     * Test that search uses EmbeddingStore with correct parameters
     */
    @Test
    void shouldSearchEmbeddingStoreWithCorrectParameters() {
        // Arrange
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "machine learning basics",
                5,
                0.8
        );
        
        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockQueryEmbedding));
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(Collections.emptyList()));

        // Act
        embeddingSearchService.search(request);

        // Assert
        ArgumentCaptor<dev.langchain4j.store.embedding.EmbeddingSearchRequest> captor =
                ArgumentCaptor.forClass(dev.langchain4j.store.embedding.EmbeddingSearchRequest.class);
        verify(embeddingStore, times(1)).search(captor.capture());

        dev.langchain4j.store.embedding.EmbeddingSearchRequest searchRequest = captor.getValue();
        assertEquals(5, searchRequest.maxResults());
        assertEquals(0.8, searchRequest.minScore(), 0.001);
        assertNotNull(searchRequest.queryEmbedding());
    }

    /**
     * Test that search returns results sorted by similarity in descending order
     */
    @Test
    void shouldReturnResultsSortedBySimilarityDescending() {
        // Arrange
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "query",
                10,
                0.5
        );

        // Create mock results with different similarity scores
        List<EmbeddingMatch<TextSegment>> mockMatches = Arrays.asList(
                createEmbeddingMatch("id1", "text 1", 0.6, Map.of("key", "value1")),
                createEmbeddingMatch("id2", "text 2", 0.9, Map.of("key", "value2")),
                createEmbeddingMatch("id3", "text 3", 0.75, Map.of("key", "value3")),
                createEmbeddingMatch("id4", "text 4", 0.55, Map.of("key", "value4"))
        );

        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockQueryEmbedding));
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(mockMatches));

        // Act
        EmbeddingSearchResponse response = embeddingSearchService.search(request);

        // Assert
        assertNotNull(response);
        assertEquals(4, response.totalResults());
        List<EmbeddingEntryResponse> results = response.results();
        assertEquals(4, results.size());

        // Verify descending order
        assertEquals(0.9, results.get(0).similarity(), 0.001);
        assertEquals(0.75, results.get(1).similarity(), 0.001);
        assertEquals(0.6, results.get(2).similarity(), 0.001);
        assertEquals(0.55, results.get(3).similarity(), 0.001);
    }

    /**
     * Test that search respects maxResults parameter
     */
    @Test
    void shouldRespectMaxResultsParameter() {
        // Arrange
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "query",
                2,  // Only want 2 results
                0.5
        );

        List<EmbeddingMatch<TextSegment>> mockMatches = Arrays.asList(
                createEmbeddingMatch("id1", "text 1", 0.9, Map.of()),
                createEmbeddingMatch("id2", "text 2", 0.85, Map.of())
        );

        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockQueryEmbedding));
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(mockMatches));

        // Act
        EmbeddingSearchResponse response = embeddingSearchService.search(request);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.totalResults());
        assertEquals(2, response.results().size());
    }

    /**
     * Test that search filters results below minSimilarity threshold
     */
    @Test
    void shouldFilterResultsBelowMinSimilarityThreshold() {
        // Arrange
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "query",
                10,
                0.7  // Minimum threshold
        );

        // EmbeddingStore should already filter by minScore
        List<EmbeddingMatch<TextSegment>> mockMatches = Arrays.asList(
                createEmbeddingMatch("id1", "text 1", 0.9, Map.of()),
                createEmbeddingMatch("id2", "text 2", 0.75, Map.of())
                // Results with score < 0.7 should not be returned by EmbeddingStore
        );

        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockQueryEmbedding));
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(mockMatches));

        // Act
        EmbeddingSearchResponse response = embeddingSearchService.search(request);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.totalResults());
        // Verify all results meet the threshold
        response.results().forEach(result -> 
                assertTrue(result.similarity() >= 0.7, 
                        "Result similarity " + result.similarity() + " is below threshold 0.7"));
    }

    /**
     * Test that results include all required fields
     */
    @Test
    void shouldIncludeAllRequiredFieldsInResults() {
        // Arrange
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "query",
                5,
                0.5
        );

        Map<String, String> metadata = Map.of(
                "fileName", "test.pdf",
                "section", "introduction"
        );

        List<EmbeddingMatch<TextSegment>> mockMatches = Collections.singletonList(
                createEmbeddingMatch("test-id-123", "test content", 0.85, metadata)
        );

        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockQueryEmbedding));
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(mockMatches));

        // Act
        EmbeddingSearchResponse response = embeddingSearchService.search(request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.totalResults());
        
        EmbeddingEntryResponse result = response.results().get(0);
        assertNotNull(result.id(), "ID should not be null");
        assertNotNull(result.text(), "Text should not be null");
        assertNotNull(result.similarity(), "Similarity should not be null");
        assertNotNull(result.metadata(), "Metadata should not be null");

        assertEquals("test-id-123", result.id());
        assertEquals("test content", result.text());
        assertEquals(0.85, result.similarity(), 0.001);
        assertEquals(2, result.metadata().size());
        assertEquals("test.pdf", result.metadata().get("fileName"));
    }

    /**
     * Test that search returns empty results when no matches found
     */
    @Test
    void shouldReturnEmptyResultsWhenNoMatchesFound() {
        // Arrange
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "query with no matches",
                10,
                0.9
        );

        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockQueryEmbedding));
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(Collections.emptyList()));

        // Act
        EmbeddingSearchResponse response = embeddingSearchService.search(request);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.totalResults());
        assertNotNull(response.results());
        assertTrue(response.results().isEmpty());
    }

    /**
     * Test that search uses default values when optional parameters are null
     */
    @Test
    void shouldUseDefaultValuesWhenOptionalParametersAreNull() {
        // Arrange - Note: The EmbeddingSearchRequest constructor sets defaults
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "query",
                null,  // Should default to 10
                null   // Should default to 0.7
        );

        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockQueryEmbedding));
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(Collections.emptyList()));

        // Act
        embeddingSearchService.search(request);

        // Assert
        ArgumentCaptor<dev.langchain4j.store.embedding.EmbeddingSearchRequest> captor =
                ArgumentCaptor.forClass(dev.langchain4j.store.embedding.EmbeddingSearchRequest.class);
        verify(embeddingStore, times(1)).search(captor.capture());

        dev.langchain4j.store.embedding.EmbeddingSearchRequest searchRequest = captor.getValue();
        assertEquals(10, searchRequest.maxResults(), "Default maxResults should be 10");
        assertEquals(0.7, searchRequest.minScore(), 0.001, "Default minSimilarity should be 0.7");
    }

    /**
     * Test that metadata is properly mapped from TextSegment to response
     */
    @Test
    void shouldProperlyMapMetadataFromTextSegmentToResponse() {
        // Arrange
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "query",
                5,
                0.5
        );

        Map<String, String> metadata = Map.of(
                "fileName", "document.pdf",
                "page", "42",
                "section", "Chapter 3"
        );

        List<EmbeddingMatch<TextSegment>> mockMatches = Collections.singletonList(
                createEmbeddingMatch("id", "content", 0.8, metadata)
        );

        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockQueryEmbedding));
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(mockMatches));

        // Act
        EmbeddingSearchResponse response = embeddingSearchService.search(request);

        // Assert
        assertNotNull(response);
        EmbeddingEntryResponse result = response.results().get(0);
        
        assertEquals(3, result.metadata().size());
        assertEquals("document.pdf", result.metadata().get("fileName"));
        assertEquals("42", result.metadata().get("page"));
        assertEquals("Chapter 3", result.metadata().get("section"));
    }

    /**
     * Test that totalResults matches the actual number of results returned
     */
    @Test
    void shouldReturnTotalResultsMatchingActualResultsCount() {
        // Arrange
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "query",
                10,
                0.5
        );

        List<EmbeddingMatch<TextSegment>> mockMatches = Arrays.asList(
                createEmbeddingMatch("id1", "text 1", 0.9, Map.of()),
                createEmbeddingMatch("id2", "text 2", 0.8, Map.of()),
                createEmbeddingMatch("id3", "text 3", 0.7, Map.of())
        );

        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockQueryEmbedding));
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(mockMatches));

        // Act
        EmbeddingSearchResponse response = embeddingSearchService.search(request);

        // Assert
        assertNotNull(response);
        assertEquals(3, response.totalResults());
        assertEquals(3, response.results().size());
        assertEquals(response.totalResults(), response.results().size());
    }

    // ============================================================
    // addEntry() Tests - RED PHASE
    // These tests verify the addEntry functionality
    // Expected to FAIL until implementation is complete
    // ============================================================

    /**
     * Test that addEntry with valid request returns a response with entryId.
     * 
     * RED PHASE: Expected to FAIL because addEntry() throws UnsupportedOperationException.
     */
    @Test
    void addEntry_withValidRequest_returnsEntryId() {
        // Arrange
        EmbeddingAddRequest request = new EmbeddingAddRequest(
                "This is a sample text to embed",
                "test-document.pdf",
                Map.of("author", "Test Author")
        );
        
        Embedding mockEmbedding = Embedding.from(createMockVector(384));
        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockEmbedding));
        when(embeddingStore.add(any(Embedding.class), any(TextSegment.class)))
                .thenReturn("generated-entry-id-123");

        // Act
        EmbeddingAddResponse response = embeddingSearchService.addEntry(request);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.entryId(), "Entry ID should not be null");
        assertEquals("generated-entry-id-123", response.entryId());
        assertNotNull(response.message(), "Message should not be null");
    }

    /**
     * Test that custom metadata is stored with the embedding entry.
     * 
     * RED PHASE: Expected to FAIL because addEntry() throws UnsupportedOperationException.
     */
    @Test
    void addEntry_withCustomMetadata_includesMetadata() {
        // Arrange
        Map<String, String> customMetadata = Map.of(
                "category", "technical",
                "version", "1.0",
                "department", "engineering"
        );
        EmbeddingAddRequest request = new EmbeddingAddRequest(
                "Technical documentation content",
                "tech-docs.pdf",
                customMetadata
        );
        
        Embedding mockEmbedding = Embedding.from(createMockVector(384));
        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockEmbedding));
        when(embeddingStore.add(any(Embedding.class), any(TextSegment.class)))
                .thenReturn("entry-with-metadata");

        // Act
        embeddingSearchService.addEntry(request);

        // Assert - Verify that EmbeddingStore.add was called with TextSegment containing metadata
        ArgumentCaptor<TextSegment> segmentCaptor = ArgumentCaptor.forClass(TextSegment.class);
        verify(embeddingStore).add(any(Embedding.class), segmentCaptor.capture());
        
        TextSegment capturedSegment = segmentCaptor.getValue();
        assertNotNull(capturedSegment.metadata(), "Metadata should not be null");
        assertEquals("technical", capturedSegment.metadata().getString("category"));
        assertEquals("1.0", capturedSegment.metadata().getString("version"));
        assertEquals("engineering", capturedSegment.metadata().getString("department"));
        assertEquals("tech-docs.pdf", capturedSegment.metadata().getString("fileName"));
    }

    /**
     * Test that default fileName "manual-entry" is used when not provided.
     * 
     * RED PHASE: Expected to FAIL because addEntry() throws UnsupportedOperationException.
     */
    @Test
    void addEntry_withDefaultFileName_usesManualEntry() {
        // Arrange - fileName is null, should default to "manual-entry"
        EmbeddingAddRequest request = new EmbeddingAddRequest(
                "User-entered text content",
                null,  // Should default to "manual-entry"
                Map.of()
        );
        
        Embedding mockEmbedding = Embedding.from(createMockVector(384));
        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockEmbedding));
        when(embeddingStore.add(any(Embedding.class), any(TextSegment.class)))
                .thenReturn("manual-entry-id");

        // Act
        embeddingSearchService.addEntry(request);

        // Assert - Verify TextSegment has fileName = "manual-entry"
        ArgumentCaptor<TextSegment> segmentCaptor = ArgumentCaptor.forClass(TextSegment.class);
        verify(embeddingStore).add(any(Embedding.class), segmentCaptor.capture());
        
        TextSegment capturedSegment = segmentCaptor.getValue();
        assertEquals("manual-entry", capturedSegment.metadata().getString("fileName"),
                "Default fileName should be 'manual-entry'");
    }

    /**
     * Test that embedding model is called to generate vector for the text.
     * 
     * RED PHASE: Expected to FAIL because addEntry() throws UnsupportedOperationException.
     */
    @Test
    void addEntry_generatesEmbedding_callsEmbeddingModel() {
        // Arrange
        String textToEmbed = "This text should be embedded using the embedding model";
        EmbeddingAddRequest request = new EmbeddingAddRequest(
                textToEmbed,
                "document.txt",
                Map.of()
        );
        
        Embedding mockEmbedding = Embedding.from(createMockVector(384));
        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockEmbedding));
        when(embeddingStore.add(any(Embedding.class), any(TextSegment.class)))
                .thenReturn("embedded-entry-id");

        // Act
        embeddingSearchService.addEntry(request);

        // Assert - Verify embedding model was called with the correct text
        verify(embeddingModel, times(1)).embed(textToEmbed);
        
        // Assert - Verify embedding store was called with the generated embedding
        ArgumentCaptor<Embedding> embeddingCaptor = ArgumentCaptor.forClass(Embedding.class);
        verify(embeddingStore).add(embeddingCaptor.capture(), any(TextSegment.class));
        
        Embedding capturedEmbedding = embeddingCaptor.getValue();
        assertNotNull(capturedEmbedding, "Embedding should not be null");
        assertEquals(384, capturedEmbedding.vector().length, "Embedding dimension should match model");
    }

    // ============================================================
    // getEntry() Tests - RED PHASE
    // These tests verify the getEntry functionality
    // Expected to FAIL until implementation is complete
    // ============================================================

    /**
     * Test that getEntry with a valid ID returns the corresponding entry.
     * 
     * RED PHASE: Expected to FAIL because getEntry() throws UnsupportedOperationException.
     */
    @Test
    void getEntry_withValidId_returnsEntry() {
        // Arrange
        String entryId = "existing-entry-123";
        String expectedText = "This is the stored document text";
        Map<String, String> expectedMetadata = Map.of(
                "fileName", "document.pdf",
                "source", "manual",
                "author", "Test Author"
        );

        // Mock the embedding store to return a match when searching by ID
        List<EmbeddingMatch<TextSegment>> mockMatches = Collections.singletonList(
                createEmbeddingMatch(entryId, expectedText, 1.0, expectedMetadata)
        );

        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockQueryEmbedding));
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(mockMatches));

        // Act
        EmbeddingEntryResponse response = embeddingSearchService.getEntry(entryId);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertEquals(entryId, response.id(), "Entry ID should match");
        assertEquals(expectedText, response.text(), "Text should match");
        assertNotNull(response.metadata(), "Metadata should not be null");
        assertEquals("document.pdf", response.metadata().get("fileName"));
        assertEquals("Test Author", response.metadata().get("author"));
    }

    /**
     * Test that getEntry with an invalid/non-existent ID throws NotFoundException.
     * 
     * RED PHASE: Expected to FAIL because getEntry() throws UnsupportedOperationException.
     */
    @Test
    void getEntry_withInvalidId_throwsNotFoundException() {
        // Arrange
        String nonExistentId = "non-existent-id-456";

        // Mock the embedding store to return empty results (entry not found)
        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockQueryEmbedding));
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(Collections.emptyList()));

        // Act & Assert
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> embeddingSearchService.getEntry(nonExistentId),
                "Should throw NotFoundException for non-existent entry ID"
        );

        assertTrue(exception.getMessage().contains(nonExistentId) || 
                   exception.getMessage().toLowerCase().contains("not found"),
                "Exception message should indicate entry was not found");
    }

    // ============================================================
    // updateEntry() Tests - RED PHASE
    // These tests verify the updateEntry functionality
    // Expected to FAIL until implementation is complete
    // ============================================================

    /**
     * Test that updateEntry with text change re-embeds and returns reEmbedded=true.
     * 
     * RED PHASE: Expected to FAIL because updateEntry() throws UnsupportedOperationException.
     */
    @Test
    void updateEntry_withTextChange_reEmbedsAndReturnsTrue() {
        // Arrange
        String entryId = "entry-to-update-123";
        String originalText = "Original document text";
        String newText = "Updated document text with new content";
        
        EmbeddingUpdateRequest request = new EmbeddingUpdateRequest(
                entryId,
                newText,  // Text is changed - should trigger re-embedding
                null,
                null
        );

        // Mock existing entry
        List<EmbeddingMatch<TextSegment>> existingMatches = Collections.singletonList(
                createEmbeddingMatch(entryId, originalText, 1.0, 
                        Map.of("fileName", "doc.pdf", "source", "manual"))
        );

        Embedding newEmbedding = Embedding.from(createMockVector(384));
        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockQueryEmbedding));
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(existingMatches));
        when(embeddingModel.embed(newText))
                .thenReturn(dev.langchain4j.model.output.Response.from(newEmbedding));

        // Act
        EmbeddingUpdateResponse response = embeddingSearchService.updateEntry(request);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertEquals(entryId, response.entryId(), "Entry ID should match");
        assertTrue(response.reEmbedded(), "reEmbedded should be true when text changes");
        assertNotNull(response.message(), "Message should not be null");

        // Verify embedding model was called with the new text
        verify(embeddingModel).embed(newText);
    }

    /**
     * Test that updateEntry with only metadata change does not re-embed.
     * 
     * RED PHASE: Expected to FAIL because updateEntry() throws UnsupportedOperationException.
     */
    @Test
    void updateEntry_withMetadataOnly_doesNotReEmbed() {
        // Arrange
        String entryId = "entry-metadata-update-456";
        String existingText = "Document text that stays the same";
        
        Map<String, String> newMetadata = Map.of(
                "category", "updated-category",
                "version", "2.0"
        );
        
        EmbeddingUpdateRequest request = new EmbeddingUpdateRequest(
                entryId,
                null,  // No text change
                "new-filename.pdf",  // Only metadata changes
                newMetadata
        );

        // Mock existing entry
        List<EmbeddingMatch<TextSegment>> existingMatches = Collections.singletonList(
                createEmbeddingMatch(entryId, existingText, 1.0, 
                        Map.of("fileName", "old-filename.pdf", "source", "manual"))
        );

        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockQueryEmbedding));
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(existingMatches));

        // Act
        EmbeddingUpdateResponse response = embeddingSearchService.updateEntry(request);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertEquals(entryId, response.entryId(), "Entry ID should match");
        assertFalse(response.reEmbedded(), "reEmbedded should be false when only metadata changes");
        assertNotNull(response.message(), "Message should not be null");

        // Verify embedding model was NOT called for re-embedding (only potentially for finding)
        // The embed call for the search should happen, but not for the new text
        verify(embeddingModel, never()).embed(existingText);
    }

    /**
     * Test that updateEntry with non-existent ID throws NotFoundException.
     * 
     * RED PHASE: Expected to FAIL because updateEntry() throws UnsupportedOperationException.
     */
    @Test
    void updateEntry_withInvalidId_throwsNotFoundException() {
        // Arrange
        String nonExistentId = "non-existent-entry-789";
        
        EmbeddingUpdateRequest request = new EmbeddingUpdateRequest(
                nonExistentId,
                "Some updated text",
                null,
                null
        );

        // Mock the embedding store to return empty results (entry not found)
        when(embeddingModel.embed(anyString()))
                .thenReturn(dev.langchain4j.model.output.Response.from(mockQueryEmbedding));
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(Collections.emptyList()));

        // Act & Assert
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> embeddingSearchService.updateEntry(request),
                "Should throw NotFoundException for non-existent entry ID"
        );

        assertTrue(exception.getMessage().contains(nonExistentId) || 
                   exception.getMessage().toLowerCase().contains("not found"),
                "Exception message should indicate entry was not found");
    }

    // Helper methods

    private EmbeddingMatch<TextSegment> createEmbeddingMatch(String id, String text, 
                                                              double similarity, 
                                                              Map<String, String> metadata) {
        Metadata segmentMetadata = new Metadata();
        metadata.forEach(segmentMetadata::put);
        
        TextSegment segment = TextSegment.from(text, segmentMetadata);
        Embedding embedding = Embedding.from(createMockVector(384));
        
        return new EmbeddingMatch<>(similarity, id, embedding, segment);
    }

    private float[] createMockVector(int dimension) {
        float[] vector = new float[dimension];
        Random random = new Random(42);
        for (int i = 0; i < dimension; i++) {
            vector[i] = random.nextFloat();
        }
        return vector;
    }
}
