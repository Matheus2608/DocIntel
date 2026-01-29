# Implementation Plan: Embedding Search & Management

**Branch**: `002-embedding-search` | **Date**: 2026-01-28 | **Spec**: [spec.md](./spec.md)

## Summary

Create a dedicated page for searching embeddings in pgvector by text query, displaying results with similarity scores and metadata in descending order. Users can also add new embedding entries (auto-generating vectors from text) and update existing entries (with re-embedding when text changes). Leverages existing LangChain4j embedding infrastructure.

## Technical Context

**Language/Version**: Java 21+ (Backend), TypeScript (Frontend)
**Primary Dependencies**: Quarkus 3.30+, LangChain4j, React 19, Vite 7.2
**Storage**: PostgreSQL 14+ with pgvector extension (existing)
**Testing**: JUnit 5, React Testing Library, Testcontainers
**Target Platform**: Web application (Linux server backend, browser frontend)
**Project Type**: web (backend + frontend)
**Performance Goals**: < 2 seconds search response, < 500ms UI interactions
**Constraints**: RAG retrieval latency < 2 seconds (per constitution)
**Scale/Scope**: Up to 50,000 embedding entries

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Spec-Driven Development | ✅ PASS | Spec created via `/speckit.specify` before planning |
| II. Test-Driven Development | ✅ READY | Will use TDD subagents for implementation |
| III. First Principles | ✅ PASS | Explored existing code patterns before proposing changes |
| IV. Context Engineering | ✅ READY | Will query tessl for LangChain4j patterns before implementation |
| V. Simplicity & Atomicity | ✅ PASS | Minimal changes, reuses existing embedding infrastructure |
| VI. Root Cause Fixes | N/A | New feature, not a bug fix |
| VII. Progressive Disclosure | ✅ PASS | Plan documents decisions and trade-offs |

**Gate Status**: ✅ ALL GATES PASSED

## Project Structure

### Documentation (this feature)

```text
specs/002-embedding-search/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (OpenAPI)
│   └── embedding-search-api.yaml
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
back/
├── src/main/java/dev/matheus/
│   ├── resource/
│   │   └── EmbeddingSearchResource.java    # NEW: REST API endpoints
│   ├── service/
│   │   └── EmbeddingSearchService.java     # NEW: Search/add/update logic
│   └── dto/
│       ├── EmbeddingSearchRequest.java     # NEW: Search request DTO
│       ├── EmbeddingSearchResponse.java    # NEW: Search response DTO
│       ├── EmbeddingAddRequest.java        # NEW: Add entry request DTO
│       └── EmbeddingUpdateRequest.java     # NEW: Update entry request DTO
└── src/test/java/dev/matheus/
    ├── resource/
    │   └── EmbeddingSearchResourceTest.java
    └── service/
        └── EmbeddingSearchServiceTest.java

front/
├── src/
│   ├── pages/
│   │   └── EmbeddingSearch.tsx             # NEW: Main search page
│   ├── components/
│   │   ├── EmbeddingSearchForm.tsx         # NEW: Search input form
│   │   ├── EmbeddingSearchResults.tsx      # NEW: Results display
│   │   ├── EmbeddingAddModal.tsx           # NEW: Add entry modal
│   │   └── EmbeddingEditModal.tsx          # NEW: Edit entry modal
│   ├── hooks/
│   │   └── useEmbeddingSearch.ts           # NEW: Search/add/update hook
│   └── types/
│       └── embedding.ts                    # NEW: TypeScript types
└── src/__tests__/
    ├── pages/
    │   └── EmbeddingSearch.test.tsx
    └── components/
        └── EmbeddingSearchForm.test.tsx
```

**Structure Decision**: Web application pattern (backend/ + frontend/) following existing project conventions. New files organized in parallel to existing ChatResource/ChatService patterns.

## Complexity Tracking

> No violations to justify - design follows existing patterns and stays minimal.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |
