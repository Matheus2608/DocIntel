# Data Model: Embedding Search & Management

**Branch**: `002-embedding-search` | **Date**: 2026-01-28

## Entities

### EmbeddingEntry (Read Model)

Represents an embedding entry returned from search or retrieval operations. This is a **read model** - the actual storage is managed by LangChain4j's pgvector EmbeddingStore.

| Field | Type | Description | Required |
|-------|------|-------------|----------|
| id | String (UUID) | Unique identifier from pgvector store | Yes |
| text | String | Original text content that was embedded | Yes |
| similarity | Double | Cosine similarity score (0.0-1.0) from search | Yes (search only) |
| metadata | Map<String, String> | Key-value metadata pairs | Yes |
| metadata.FILE_NAME | String | Source document or custom identifier | Yes |
| metadata.entry_id | String (UUID) | Stable ID for update/delete operations | Yes (manual entries) |
| metadata.source | String | "manual" for user-added, "ingestion" for documents | Yes |
| metadata.created_at | String (ISO-8601) | Creation timestamp | Yes (manual entries) |
| metadata.PARAGRAPH | String | Original chunk context (ingested docs only) | No |

### SearchQuery (Request)

User's search request submitted to the API.

| Field | Type | Description | Required | Default |
|-------|------|-------------|----------|---------|
| query | String | Text to search for | Yes | - |
| maxResults | Integer | Maximum results to return | No | 10 |
| minSimilarity | Double | Minimum similarity threshold | No | 0.7 |

**Validation Rules**:
- `query` must be non-empty and non-whitespace
- `maxResults` must be between 1 and 100
- `minSimilarity` must be between 0.0 and 1.0

### AddEntryRequest (Request)

Request to add a new embedding entry.

| Field | Type | Description | Required | Default |
|-------|------|-------------|----------|---------|
| text | String | Text content to embed | Yes | - |
| fileName | String | Source identifier for filtering | No | "manual-entry" |
| customMetadata | Map<String, String> | User-defined metadata | No | {} |

**Validation Rules**:
- `text` must be non-empty and non-whitespace
- `text` length should be < 10,000 characters (embedding model limit)
- `fileName` should be alphanumeric with dashes/underscores
- `customMetadata` keys cannot override reserved fields (FILE_NAME, entry_id, source, created_at)

### UpdateEntryRequest (Request)

Request to update an existing embedding entry.

| Field | Type | Description | Required |
|-------|------|-------------|----------|
| entryId | String (UUID) | ID of entry to update | Yes |
| text | String | New text content (triggers re-embedding if changed) | No |
| fileName | String | Updated source identifier | No |
| customMetadata | Map<String, String> | Updated metadata (merged with existing) | No |

**Validation Rules**:
- `entryId` must be a valid UUID
- At least one of `text`, `fileName`, or `customMetadata` must be provided
- Same validation as AddEntryRequest for individual fields

## State Transitions

### Manual Entry Lifecycle

```
[Not Exists] ---(addEntry)---> [Stored]

[Stored] ---(updateEntry: text changed)---> [Re-embedded & Stored]
[Stored] ---(updateEntry: metadata only)---> [Metadata Updated]
[Stored] ---(deleteEntry)---> [Not Exists]
```

### Search Operation (Stateless)

```
[Query Text] ---(embed)---> [Query Embedding] ---(search)---> [Sorted Results]
```

## Relationships

```
┌─────────────────────────────────────────────────────────┐
│                    pgvector Store                        │
│  ┌─────────────────────────────────────────────────────┐│
│  │ Embedding Entry                                      ││
│  │  - id (UUID, primary key)                           ││
│  │  - embedding (vector[768])                          ││
│  │  - text (from TextSegment)                          ││
│  │  - metadata (from TextSegment.Metadata)             ││
│  └─────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────┘
           │
           │ contains (via FILE_NAME metadata)
           ▼
┌─────────────────────────────────────────────────────────┐
│                  Document Sources                        │
│  ┌──────────────┐  ┌───────────────┐  ┌──────────────┐ │
│  │ Ingested Doc │  │ Manual Entry  │  │ HyDE Question│ │
│  │ (source:     │  │ (source:      │  │ (source:     │ │
│  │  ingestion)  │  │  manual)      │  │  ingestion)  │ │
│  └──────────────┘  └───────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────┘
```

## Database Considerations

### pgvector Index Strategy

The existing pgvector store uses IVFFlat index for approximate nearest neighbor search. No schema changes required - new entries use the same embedding dimensions (768).

### Metadata Storage

LangChain4j stores metadata as JSONB in PostgreSQL. The `entry_id` field enables retrieval without full vector search for update/delete operations.

### Filtering Capability

pgvector + LangChain4j supports metadata filtering:
- `IsEqualTo(key, value)` - exact match
- `IsIn(key, values)` - set membership
- `And`, `Or` - logical combinations

Used for:
- Filter by `entry_id` for updates
- Filter by `source = "manual"` to list only manual entries
- Filter by `FILE_NAME` for document-specific search

## TypeScript Types (Frontend)

```typescript
interface EmbeddingEntry {
  id: string;
  text: string;
  similarity: number;
  metadata: {
    FILE_NAME: string;
    entry_id?: string;
    source: 'manual' | 'ingestion';
    created_at?: string;
    PARAGRAPH?: string;
    [key: string]: string | undefined;
  };
}

interface SearchQuery {
  query: string;
  maxResults?: number;
  minSimilarity?: number;
}

interface AddEntryRequest {
  text: string;
  fileName?: string;
  customMetadata?: Record<string, string>;
}

interface UpdateEntryRequest {
  entryId: string;
  text?: string;
  fileName?: string;
  customMetadata?: Record<string, string>;
}

interface SearchResponse {
  results: EmbeddingEntry[];
  totalFound: number;
  queryTime: number; // milliseconds
}

interface AddEntryResponse {
  id: string;
  success: boolean;
}

interface UpdateEntryResponse {
  id: string;
  success: boolean;
  reembedded: boolean;
}
```
