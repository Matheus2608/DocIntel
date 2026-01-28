# Implementation Plan: Docling Document Ingestion

**Branch**: `001-docling-ingestion` | **Date**: 2025-01-28 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-docling-ingestion/spec.md`

## Summary

Replace the current PDFBox + Tabula document ingestion pipeline with IBM's Docling library to achieve:
1. Superior table extraction with preserved structure
2. Semantic chunking based on document hierarchy (not arbitrary token splits)
3. Markdown output with valid table syntax
4. Better RAG retrieval through meaningful chunk boundaries

**Technical Approach**: Use Docling Java client connecting to Docling Serve (Docker container) for document processing. Integrate with existing LangChain4j embedding pipeline. Use HybridChunker for token-aware semantic chunking.

## Technical Context

**Language/Version**: Java 21+ (existing Quarkus 3.30.4 application)
**Primary Dependencies**:
- docling-serve-client 0.4.4 (new)
- docling-testcontainers 0.4.4 (new, test scope)
- quarkus-langchain4j (existing)
- pgvector (existing)

**Storage**: PostgreSQL 14+ with pgvector extension (existing)
**Testing**: JUnit 5, Testcontainers, AssertJ (existing)
**Target Platform**: Linux server (Docker deployment)
**Project Type**: Web application (backend focus for this feature)
**Performance Goals**:
- Document processing < 30 seconds for 100 pages
- Zero data loss for visible content
- 95% parsing success rate

**Constraints**:
- Max file size: 50MB
- Max processing time: 60 seconds for 200 pages
- Backwards compatibility: existing embeddings remain valid

**Scale/Scope**: Single-user document uploads, ~10-50 documents per chat typical

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. Spec-Driven Development | ✅ PASS | spec.md complete with 13 FRs, 9 SCs, 5 user stories |
| II. Test-Driven Development | ✅ PLANNED | TDD subagents will be used for all implementation |
| III. First Principles | ✅ PASS | Explored existing codebase (DocumentIngestionService, CustomTableAwareSplitter) |
| IV. Context Engineering | ✅ PASS | Researched Docling Java docs, installed quarkus-langchain4j-core tile |
| V. Simplicity & Atomicity | ✅ PASS | Minimal changes: replace parser, add chunker, keep embedding pipeline |
| VI. Root Cause Fixes | ✅ PASS | Addresses root cause (poor chunking) not symptoms |
| VII. Progressive Disclosure | ✅ PLANNED | Plan documents all decisions and artifacts |

**Post-Design Re-check**: All principles satisfied. No violations.

## Project Structure

### Documentation (this feature)

```text
specs/001-docling-ingestion/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 research findings
├── data-model.md        # Entity definitions and migrations
├── quickstart.md        # Setup and verification guide
├── contracts/           # API contracts
│   └── document-processing-api.yaml
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
back/
├── src/main/java/dev/matheus/
│   ├── entity/
│   │   ├── DocumentFile.java         # Modified: add processingStatus, processedAt
│   │   ├── DocumentChunk.java        # NEW: chunk entity with metadata
│   │   ├── ChunkEmbedding.java       # NEW: chunk-to-embedding mapping
│   │   ├── ContentType.java          # NEW: enum for chunk types
│   │   └── ProcessingStatus.java     # NEW: enum for processing states
│   ├── service/
│   │   ├── docling/
│   │   │   ├── DoclingDocumentParser.java    # NEW: Docling client wrapper
│   │   │   ├── DoclingChunkingService.java   # NEW: HybridChunker integration
│   │   │   └── DoclingConfigProperties.java  # NEW: config class
│   │   ├── DocumentIngestionService.java     # Modified: delegate to Docling
│   │   └── HypotheticalQuestionService.java  # Modified: use new chunks
│   ├── dto/
│   │   └── ChunkResponse.java        # NEW: API response DTO
│   └── resource/
│       └── DocumentProcessingResource.java  # NEW: REST endpoints
├── src/main/resources/
│   ├── application.properties        # Modified: add docling config
│   └── db/migration/
│       └── V20250128__add_docling_entities.sql  # NEW: migration
└── src/test/java/dev/matheus/
    ├── service/docling/
    │   ├── DoclingDocumentParserTest.java      # NEW: unit tests
    │   ├── DoclingChunkingServiceTest.java     # NEW: unit tests
    │   └── DoclingIntegrationTest.java         # NEW: testcontainers
    └── resource/
        └── DocumentProcessingResourceTest.java # NEW: API tests
```

**Structure Decision**: Backend-only changes. Existing frontend continues to upload documents via existing API. New endpoints for processing status and chunk access.

## Implementation Phases

### Phase 1: Infrastructure Setup
- Add Docling Maven dependencies
- Configure Docling Serve in docker-compose
- Add application.properties configuration
- Create database migration script

### Phase 2: Core Entities
- Create DocumentChunk entity
- Create ChunkEmbedding entity
- Create ContentType and ProcessingStatus enums
- Modify DocumentFile entity (add processing fields)

### Phase 3: Docling Integration
- Create DoclingDocumentParser service
- Create DoclingChunkingService with HybridChunker
- Create DoclingConfigProperties for configuration
- Write unit tests for parser and chunker

### Phase 4: Pipeline Integration
- Modify DocumentIngestionService to use Docling
- Modify HypotheticalQuestionService for new chunks
- Wire up embedding generation with new chunks
- Integration tests with Testcontainers

### Phase 5: API Endpoints
- Create DocumentProcessingResource
- Implement status, chunks, reprocess endpoints
- API tests with REST Assured

### Phase 6: Validation & Polish
- End-to-end testing with real documents
- Performance validation (30-second target)
- Documentation updates

## Complexity Tracking

> No violations requiring justification. Design follows constitution principles.

| Aspect | Approach | Rationale |
|--------|----------|-----------|
| Docling Serve as Docker | Sidecar container | Isolates Python deps, easy deployment |
| New entities (DocumentChunk) | Separate table | Clean separation, enables chunk-level operations |
| HybridChunker | Docling native | Best balance of semantic + token awareness |

## Related Artifacts

- **Research**: [research.md](./research.md) - Technology decisions and rationale
- **Data Model**: [data-model.md](./data-model.md) - Entity definitions and migrations
- **API Contracts**: [contracts/document-processing-api.yaml](./contracts/document-processing-api.yaml)
- **Quickstart**: [quickstart.md](./quickstart.md) - Setup and verification guide

## Next Steps

Run `/speckit.tasks` to generate detailed task breakdown for TDD implementation.
