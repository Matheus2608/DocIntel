# Data Model: Docling Document Ingestion

**Feature**: 001-docling-ingestion
**Date**: 2025-01-28

## Entity Overview

This document defines the data model changes required for Docling-based document ingestion.

---

## New Entities

### DocumentChunk

Represents a semantic chunk of a processed document with rich metadata for RAG retrieval.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique chunk identifier |
| documentFileId | UUID | FK to DocumentFile, NOT NULL | Source document reference |
| content | TEXT | NOT NULL | Markdown content of the chunk |
| contentType | ENUM | NOT NULL | Type: TEXT, TABLE, LIST, CODE, HEADING |
| position | INTEGER | NOT NULL | Order within document (0-indexed) |
| sectionHeading | VARCHAR(500) | NULLABLE | Parent section heading text |
| headingLevel | INTEGER | NULLABLE | Depth in document hierarchy (1-6) |
| tokenCount | INTEGER | NOT NULL | Token count for chunk sizing |
| createdAt | TIMESTAMP | NOT NULL, default NOW | Creation timestamp |

**Indexes**:
- `idx_chunk_document_id` on `documentFileId`
- `idx_chunk_position` on `(documentFileId, position)`
- `idx_chunk_content_type` on `contentType`

**Relationships**:
- Many-to-One with DocumentFile

---

### ChunkEmbedding

Links chunks to their vector embeddings (complements existing embedding store).

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Unique identifier |
| chunkId | UUID | FK to DocumentChunk, NOT NULL | Associated chunk |
| embeddingId | VARCHAR(255) | NOT NULL, UNIQUE | Reference to pgvector embedding store |
| embeddingType | ENUM | NOT NULL | Type: CONTENT, HYPOTHETICAL_QUESTION |
| createdAt | TIMESTAMP | NOT NULL, default NOW | Creation timestamp |

**Indexes**:
- `idx_embedding_chunk_id` on `chunkId`
- `idx_embedding_id` on `embeddingId`

---

## Modified Entities

### DocumentFile (existing)

Add fields to track processing status and metadata.

| New Field | Type | Constraints | Description |
|-----------|------|-------------|-------------|
| processingStatus | ENUM | NOT NULL, default PENDING | Status: PENDING, PROCESSING, COMPLETED, FAILED |
| processingError | TEXT | NULLABLE | Error message if processing failed |
| processedAt | TIMESTAMP | NULLABLE | When processing completed |
| chunkCount | INTEGER | NULLABLE | Total chunks generated |
| processorVersion | VARCHAR(50) | NULLABLE | Docling version used (for re-processing) |

**State Transitions**:
```
PENDING → PROCESSING → COMPLETED
                    ↘ FAILED
```

---

### HypoteticalQuestion (existing)

Link to new DocumentChunk entity for better traceability.

| New Field | Type | Constraints | Description |
|-----------|------|-------------|-------------|
| chunkId | UUID | FK to DocumentChunk, NULLABLE | Associated chunk (nullable for backwards compatibility) |

---

## Enums

### ContentType
```java
public enum ContentType {
    TEXT,           // Regular paragraph text
    TABLE,          // Markdown table
    LIST,           // Bullet or numbered list
    CODE,           // Code block
    HEADING,        // Section heading
    MIXED           // Chunk with multiple types
}
```

### ProcessingStatus
```java
public enum ProcessingStatus {
    PENDING,        // Uploaded, not yet processed
    PROCESSING,     // Currently being processed by Docling
    COMPLETED,      // Successfully processed and chunked
    FAILED          // Processing failed (see processingError)
}
```

---

## Database Migration

### Migration Script: V20250128__add_docling_entities.sql

```sql
-- Add processing status to document_file
ALTER TABLE document_file
ADD COLUMN processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
ADD COLUMN processing_error TEXT,
ADD COLUMN processed_at TIMESTAMP,
ADD COLUMN chunk_count INTEGER,
ADD COLUMN processor_version VARCHAR(50);

-- Create document_chunk table
CREATE TABLE document_chunk (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_file_id UUID NOT NULL REFERENCES document_file(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    content_type VARCHAR(20) NOT NULL,
    position INTEGER NOT NULL,
    section_heading VARCHAR(500),
    heading_level INTEGER,
    token_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chunk_document_id ON document_chunk(document_file_id);
CREATE INDEX idx_chunk_position ON document_chunk(document_file_id, position);
CREATE INDEX idx_chunk_content_type ON document_chunk(content_type);

-- Create chunk_embedding table
CREATE TABLE chunk_embedding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chunk_id UUID NOT NULL REFERENCES document_chunk(id) ON DELETE CASCADE,
    embedding_id VARCHAR(255) NOT NULL UNIQUE,
    embedding_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_embedding_chunk_id ON chunk_embedding(chunk_id);
CREATE INDEX idx_embedding_id ON chunk_embedding(embedding_id);

-- Add chunk reference to hypothetical_question (nullable for backwards compatibility)
ALTER TABLE hypotetical_question
ADD COLUMN chunk_id UUID REFERENCES document_chunk(id) ON DELETE SET NULL;
```

---

## Entity Relationships Diagram

```
┌─────────────────┐       ┌─────────────────────┐
│  DocumentFile   │       │    DocumentChunk    │
├─────────────────┤       ├─────────────────────┤
│ id              │◄──────│ documentFileId (FK) │
│ fileName        │   1:N │ id                  │
│ fileType        │       │ content             │
│ processingStatus│       │ contentType         │
│ processedAt     │       │ position            │
│ chunkCount      │       │ sectionHeading      │
└─────────────────┘       │ headingLevel        │
                          │ tokenCount          │
                          └──────────┬──────────┘
                                     │
                                     │ 1:N
                                     ▼
                          ┌─────────────────────┐
                          │   ChunkEmbedding    │
                          ├─────────────────────┤
                          │ id                  │
                          │ chunkId (FK)        │
                          │ embeddingId         │
                          │ embeddingType       │
                          └─────────────────────┘
                                     │
                                     │ references
                                     ▼
                          ┌─────────────────────┐
                          │  pgvector store     │
                          │  (embedding_store)  │
                          └─────────────────────┘
```

---

## Validation Rules

### DocumentChunk
- `content` must not be empty
- `contentType` must be valid enum value
- `position` must be >= 0 and unique within document
- `tokenCount` must be > 0 and <= configured max (2000 default)
- `headingLevel` must be 1-6 if present

### DocumentFile (new validations)
- `processingStatus` transitions must follow state machine
- `processingError` required when status is FAILED
- `processedAt` required when status is COMPLETED
- `chunkCount` must match actual chunk count when COMPLETED

---

## Backwards Compatibility

1. **Existing documents**: `processingStatus` defaults to PENDING, can be re-processed
2. **Existing embeddings**: Remain valid, `chunk_id` in `hypotetical_question` is nullable
3. **Gradual migration**: New uploads use new pipeline, existing can be re-indexed optionally
