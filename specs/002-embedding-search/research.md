# Research: Embedding Search & Management

**Branch**: `002-embedding-search` | **Date**: 2026-01-28

## Research Questions

### 1. How to Search Embeddings Directly with LangChain4j?

**Decision**: Use `EmbeddingStore.search()` with `EmbeddingSearchRequest` builder pattern

**Rationale**: The existing codebase already uses this pattern in `RetrievalInfoService.java`. LangChain4j's `EmbeddingStore<TextSegment>` provides a clean API for similarity search:

```java
EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
    .maxResults(maxResults)
    .minScore(minSimilarity)
    .queryEmbedding(queryEmbedding)
    .build();

EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
List<EmbeddingMatch<TextSegment>> matches = result.matches();
```

**Alternatives Considered**:
- Raw SQL with pgvector `<->` operator: More complex, less maintainable, bypasses LangChain4j abstraction
- Custom repository method: Unnecessary since LangChain4j already provides this

### 2. How to Add New Embeddings to pgvector Store?

**Decision**: Use `EmbeddingStore.add()` or `EmbeddingStore.addAll()` with pre-generated embeddings

**Rationale**: LangChain4j provides straightforward methods:

```java
// Generate embedding from text
Embedding embedding = embeddingModel.embed(text).content();

// Create text segment with metadata
TextSegment segment = TextSegment.from(text, metadata);

// Store embedding
String embeddingId = embeddingStore.add(embedding, segment);
```

**Alternatives Considered**:
- Direct pgvector INSERT via SQL: Bypasses LangChain4j, loses metadata handling
- Batch processing: Overkill for single-entry adds

### 3. How to Update Existing Embeddings?

**Decision**: Delete old embedding by ID + add new embedding (no native update in pgvector store)

**Rationale**: LangChain4j's `EmbeddingStore` interface does not expose an update method. The pattern used in existing code (`ChatService.deleteChat()`) shows the removal approach:

```java
// Remove by ID
embeddingStore.remove(embeddingId);

// Or remove by filter
Filter filter = new IsEqualTo("file_name", filename);
embeddingStore.removeAll(filter);

// Then add new embedding
String newId = embeddingStore.add(newEmbedding, newSegment);
```

**Alternatives Considered**:
- Direct SQL UPDATE: Would need to manage pgvector format manually, error-prone
- In-place metadata update: pgvector stores vectors separately from metadata, requires both operations

### 4. How to Retrieve Embedding Entry by ID?

**Decision**: Use filter-based search with ID metadata or direct embedding ID match

**Rationale**: pgvector store entries have UUIDs. LangChain4j supports filters:

```java
// Option A: Store custom ID in metadata, filter by it
Filter filter = new IsEqualTo("entry_id", entryId);
EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
    .maxResults(1)
    .filter(filter)
    .queryEmbedding(dummyEmbedding) // Required but filtered by ID
    .build();

// Option B: Use embeddingStore.remove(embeddingId) which implies ID is tracked
```

For update functionality, we need to track the embedding ID returned from `add()` operations. The current implementation doesn't expose individual entry IDs through the API.

**Decision**: Add an `entry_id` metadata field to all manually added entries for retrieval/update purposes.

### 5. Frontend State Management Pattern?

**Decision**: Custom React hook with fetch API (consistent with existing patterns)

**Rationale**: The existing frontend uses custom hooks (`useChats`, `useWebSocketChat`) with the Fetch API. No React Query is installed. To maintain consistency:

```typescript
// useEmbeddingSearch.ts
export function useEmbeddingSearch() {
  const [results, setResults] = useState<EmbeddingSearchResult[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const search = async (query: string) => { ... };
  const addEntry = async (text: string, metadata: Record<string, string>) => { ... };
  const updateEntry = async (entryId: string, text: string, metadata: Record<string, string>) => { ... };

  return { results, isLoading, error, search, addEntry, updateEntry };
}
```

**Alternatives Considered**:
- React Query: Would be better but requires adding new dependency, not in current stack
- Redux: Overkill for single-page state

### 6. How to Display Metadata in Search Results?

**Decision**: Return metadata as JSON object, render in expandable card component

**Rationale**: LangChain4j TextSegment stores metadata as `Metadata` object (key-value pairs). Backend serializes to JSON:

```java
record EmbeddingSearchResult(
    String id,
    String text,
    double similarity,
    Map<String, String> metadata
) {}
```

Frontend renders in expandable card:
- Default view: text preview, similarity score, file name
- Expanded view: full text, all metadata key-value pairs

### 7. Backend Endpoint Design?

**Decision**: RESTful resource at `/api/embeddings` with standard CRUD operations

**Rationale**: Following existing patterns in `ChatResource.java`:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/embeddings/search` | POST | Search embeddings by text query |
| `/api/embeddings` | POST | Add new embedding entry |
| `/api/embeddings/{id}` | PUT | Update existing entry |
| `/api/embeddings/{id}` | GET | Get single entry by ID |

Using POST for search (instead of GET) because query text may be long and contains special characters.

### 8. Metadata Schema for Manual Entries?

**Decision**: Required `file_name` (for filtering compatibility) + optional custom key-value pairs

**Rationale**: Existing embeddings use `FILE_NAME` and `PARAGRAPH` metadata keys. To maintain compatibility:

```java
Metadata metadata = Metadata.metadata(
    "FILE_NAME", providedFileName,           // Required, defaults to "manual-entry"
    "entry_id", generatedUUID,               // Required, for update/delete
    "created_at", timestamp,                 // Auto-set
    "source", "manual"                       // Distinguishes from ingested docs
);

// Add user-provided custom fields
customFields.forEach(metadata::put);
```

## Technology Decisions Summary

| Decision | Choice | Confidence |
|----------|--------|------------|
| Search API | LangChain4j EmbeddingStore.search() | High |
| Add API | LangChain4j EmbeddingStore.add() | High |
| Update API | Delete + Add (no native update) | High |
| Entry Tracking | UUID in metadata (`entry_id`) | Medium |
| Frontend State | Custom hook with Fetch API | High |
| Endpoint Design | RESTful /api/embeddings/* | High |
| Metadata Schema | FILE_NAME + entry_id + custom fields | High |

## Open Questions Resolved

All NEEDS CLARIFICATION markers from spec have been resolved through codebase exploration:

1. ✅ Embedding model: text-embedding-3-small, 768 dimensions (from ModelConfig.java)
2. ✅ Storage mechanism: pgvector via LangChain4j EmbeddingStore (from existing services)
3. ✅ Metadata structure: Key-value pairs in Metadata object (from TextSegment usage)
4. ✅ Update strategy: Delete + Add pattern (from ChatService.deleteChat())
