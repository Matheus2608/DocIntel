package dev.matheus.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.data.document.Metadata;
import dev.matheus.dto.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing and searching embedding entries in pgvector
 */
@ApplicationScoped
public class EmbeddingSearchService {

    private static final Logger LOG = Logger.getLogger(EmbeddingSearchService.class);

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    EmbeddingModel embeddingModel;

    /**
     * Search embeddings by text query
     * 
     * @param request search parameters
     * @return search results with similarity scores
     */
    public EmbeddingSearchResponse search(EmbeddingSearchRequest request) {
        LOG.infof("Searching embeddings: query=%s, maxResults=%d, minSimilarity=%.2f",
                request.query(), request.maxResults(), request.minSimilarity());
        
        // 1. Embed query text
        Embedding queryEmbedding = embeddingModel.embed(request.query()).content();
        
        // 2. Build search request
        dev.langchain4j.store.embedding.EmbeddingSearchRequest searchRequest = 
            dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(request.maxResults())
                .minScore(request.minSimilarity())
                .build();
        
        // 3. Execute search
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
        
        // 4. Map matches to response and sort by similarity descending
        List<EmbeddingEntryResponse> entries = result.matches().stream()
            .sorted(Comparator.comparingDouble(EmbeddingMatch<TextSegment>::score).reversed())
            .map(match -> new EmbeddingEntryResponse(
                match.embeddingId(),
                match.embedded().text(),
                match.score(),
                convertMetadata(match.embedded().metadata())
            ))
            .collect(Collectors.toList());
        
        return new EmbeddingSearchResponse(entries, entries.size());
    }
    
    /**
     * Convert LangChain4j Metadata to Map<String, String>
     */
    private Map<String, String> convertMetadata(dev.langchain4j.data.document.Metadata metadata) {
        return metadata.toMap().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> String.valueOf(e.getValue())
            ));
    }

    /**
     * Add a new embedding entry
     * 
     * @param request entry details
     * @return response with generated entry ID
     */
    public EmbeddingAddResponse addEntry(EmbeddingAddRequest request) {
        LOG.infof("Adding embedding entry: text=%s, fileName=%s",
                request.text().substring(0, Math.min(50, request.text().length())),
                request.fileName());
        
        // 1. Generate embedding from text
        Embedding embedding = embeddingModel.embed(request.text()).content();
        
        // 2. Build metadata
        String fileName = (request.fileName() == null || request.fileName().isBlank()) 
                ? "manual-entry" 
                : request.fileName();
        String entryId = UUID.randomUUID().toString();
        
        Metadata metadata = new Metadata()
                .put("fileName", fileName)
                .put("source", "manual")
                .put("entry_id", entryId)
                .put("created_at", Instant.now().toString());
        
        // Add custom metadata if provided
        if (request.customMetadata() != null) {
            request.customMetadata().forEach(metadata::put);
        }
        
        // 3. Create TextSegment with metadata
        TextSegment textSegment = TextSegment.from(request.text(), metadata);
        
        // 4. Store in embedding store
        String storedId = embeddingStore.add(embedding, textSegment);
        
        LOG.infof("Successfully added embedding entry: storedId=%s, entryId=%s", storedId, entryId);
        
        return new EmbeddingAddResponse(storedId, "Entry added successfully");
    }

    /**
     * Get a single embedding entry by ID
     * 
     * @param entryId the entry ID
     * @return the embedding entry
     */
    public EmbeddingEntryResponse getEntry(String entryId) {
        LOG.infof("Getting embedding entry: entryId=%s", entryId);
        
        EmbeddingMatch<TextSegment> match = findEntryMatch(entryId);
        LOG.infof("Successfully retrieved entry: entryId=%s", entryId);
        
        return new EmbeddingEntryResponse(
            match.embeddingId(),
            match.embedded().text(),
            null,  // similarity is null for direct entry retrieval
            convertMetadata(match.embedded().metadata())
        );
    }
    
    /**
     * Internal method to find an entry match by ID
     * 
     * @param entryId the entry ID
     * @return the embedding match (includes embedding vector)
     * @throws NotFoundException if entry not found
     */
    private EmbeddingMatch<TextSegment> findEntryMatch(String entryId) {
        // Create a dummy embedding for filter-based search (required by EmbeddingSearchRequest)
        Embedding dummyEmbedding = embeddingModel.embed("dummy").content();
        
        // Search with filter on entry_id metadata field
        dev.langchain4j.store.embedding.EmbeddingSearchRequest searchRequest = 
            dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                .queryEmbedding(dummyEmbedding)
                .maxResults(1)
                .filter(new IsEqualTo("entry_id", entryId))
                .build();
        
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
        
        if (result.matches().isEmpty()) {
            LOG.warnf("Entry not found: entryId=%s", entryId);
            throw new NotFoundException("Entry not found: " + entryId);
        }
        
        return result.matches().get(0);
    }

    /**
     * Update an existing embedding entry
     * 
     * @param request update parameters
     * @return response with update status
     */
    public EmbeddingUpdateResponse updateEntry(EmbeddingUpdateRequest request) {
        LOG.infof("Updating embedding entry: entryId=%s", request.entryId());
        
        // 1. Get existing entry match (throws NotFoundException if not found)
        // This gives us access to both the text and the existing embedding
        EmbeddingMatch<TextSegment> existingMatch = findEntryMatch(request.entryId());
        String existingText = existingMatch.embedded().text();
        Embedding existingEmbedding = existingMatch.embedding();
        
        // 2. Determine if text changed
        boolean textChanged = request.text() != null && 
                             !request.text().isBlank() && 
                             !request.text().equals(existingText);
        
        // 3. Build new metadata from existing + updates
        Metadata newMetadata = new Metadata();
        
        // Copy existing metadata
        Map<String, String> existingMetadataMap = convertMetadata(existingMatch.embedded().metadata());
        if (existingMetadataMap != null) {
            existingMetadataMap.forEach(newMetadata::put);
        }
        
        // Update fileName if provided
        if (request.fileName() != null && !request.fileName().isBlank()) {
            newMetadata.put("fileName", request.fileName());
        }
        
        // Merge custom metadata if provided
        if (request.customMetadata() != null) {
            request.customMetadata().forEach(newMetadata::put);
        }
        
        // Preserve entry_id for future lookups
        newMetadata.put("entry_id", request.entryId());
        newMetadata.put("updated_at", Instant.now().toString());
        
        // 4. Determine text to use
        String textToStore = textChanged ? request.text() : existingText;
        
        // 5. Delete old entry by filter (using entry_id metadata)
        embeddingStore.removeAll(new IsEqualTo("entry_id", request.entryId()));
        
        // 6. Add new entry
        TextSegment newSegment = TextSegment.from(textToStore, newMetadata);
        
        if (textChanged) {
            // Re-embed with new text
            Embedding newEmbedding = embeddingModel.embed(textToStore).content();
            embeddingStore.add(newEmbedding, newSegment);
            LOG.infof("Entry re-embedded with new text: entryId=%s", request.entryId());
        } else {
            // Reuse existing embedding - no re-embedding needed
            embeddingStore.add(existingEmbedding, newSegment);
            LOG.infof("Entry updated with metadata only: entryId=%s", request.entryId());
        }
        
        String message = textChanged 
            ? "Entry updated and re-embedded successfully" 
            : "Entry metadata updated successfully";
        
        return new EmbeddingUpdateResponse(request.entryId(), message, textChanged);
    }

    /**
     * Delete an embedding entry
     * 
     * @param entryId the entry ID to delete
     */
    public void deleteEntry(String entryId) {
        LOG.infof("Deleting embedding entry: entryId=%s", entryId);
        
        // TODO: Implementation in TDD GREEN phase
        throw new UnsupportedOperationException("Delete entry not implemented yet");
    }
}
