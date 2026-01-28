# Quickstart: Embedding Search & Management

**Branch**: `002-embedding-search` | **Date**: 2026-01-28

## Overview

This feature adds a dedicated page for searching and managing embedding entries in the pgvector database. Users can:

1. **Search**: Enter text queries to find similar content by semantic similarity
2. **Add**: Create new embedding entries from text (auto-generates vectors)
3. **Update**: Modify existing entries (re-embeds when text changes)

## Prerequisites

- Docker running (for PostgreSQL + pgvector)
- Backend running on port 8080 (`cd back && mvn quarkus:dev`)
- Frontend running on port 5173 (`cd front && npm run dev`)
- OpenAI API key configured (`OPENAI_API_KEY` environment variable)

## Quick Test

### 1. Search Embeddings

```bash
curl -X POST http://localhost:8080/api/embeddings/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "authentication configuration",
    "maxResults": 5,
    "minSimilarity": 0.6
  }'
```

Expected response:
```json
{
  "results": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "text": "Configure authentication using OAuth2...",
      "similarity": 0.89,
      "metadata": {
        "FILE_NAME": "security-guide.pdf",
        "source": "ingestion"
      }
    }
  ],
  "totalFound": 3,
  "queryTime": 156
}
```

### 2. Add Entry

```bash
curl -X POST http://localhost:8080/api/embeddings \
  -H "Content-Type: application/json" \
  -d '{
    "text": "This is a test entry about API design patterns.",
    "fileName": "api-reference",
    "customMetadata": {
      "category": "documentation"
    }
  }'
```

Expected response:
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "success": true
}
```

### 3. Update Entry

```bash
curl -X PUT http://localhost:8080/api/embeddings/660e8400-e29b-41d4-a716-446655440001 \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Updated content about RESTful API design patterns.",
    "customMetadata": {
      "version": "2.0"
    }
  }'
```

Expected response:
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "success": true,
  "reembedded": true
}
```

## Frontend Usage

1. Navigate to `/embeddings` (or click "Embedding Search" in navigation)
2. Enter search query in the text input
3. Click "Search" to view results sorted by similarity
4. Expand result cards to see full metadata
5. Click "Add Entry" to open the add form
6. Click edit icon on a result to modify it

## Implementation Files

### Backend (Java/Quarkus)

| File | Purpose |
|------|---------|
| `EmbeddingSearchResource.java` | REST endpoints |
| `EmbeddingSearchService.java` | Business logic |
| `EmbeddingSearchRequest.java` | Search DTO |
| `EmbeddingSearchResponse.java` | Response DTO |
| `EmbeddingAddRequest.java` | Add entry DTO |
| `EmbeddingUpdateRequest.java` | Update entry DTO |

### Frontend (React/TypeScript)

| File | Purpose |
|------|---------|
| `EmbeddingSearch.tsx` | Main page component |
| `EmbeddingSearchForm.tsx` | Search input form |
| `EmbeddingSearchResults.tsx` | Results display |
| `EmbeddingAddModal.tsx` | Add entry modal |
| `EmbeddingEditModal.tsx` | Edit entry modal |
| `useEmbeddingSearch.ts` | API hook |
| `embedding.ts` | TypeScript types |

## Key Design Decisions

1. **POST for search**: Query text can be long, avoids URL encoding issues
2. **Delete + Add for updates**: LangChain4j doesn't support in-place updates
3. **entry_id metadata**: Stable ID separate from pgvector's internal UUID
4. **Fetch API**: Consistent with existing frontend patterns (no React Query)

## Testing

### Backend Tests

```bash
cd back
mvn test -Dtest=EmbeddingSearchResourceTest
mvn test -Dtest=EmbeddingSearchServiceTest
```

### Frontend Tests

```bash
cd front
npm test -- --testPathPattern="EmbeddingSearch"
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "No results found" | Lower `minSimilarity` threshold or check if documents are ingested |
| "Embedding generation failed" | Verify `OPENAI_API_KEY` is set correctly |
| "Entry not found" | Ensure using `entry_id` from metadata, not pgvector's internal ID |
| Slow search | Check pgvector index, ensure < 50k entries for optimal performance |
