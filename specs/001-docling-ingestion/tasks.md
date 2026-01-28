# Tasks: Docling Document Ingestion

**Input**: Design documents from `/specs/001-docling-ingestion/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: TDD is MANDATORY per project constitution. Tests MUST be written FIRST and FAIL before implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `back/src/main/java/dev/matheus/`
- **Tests**: `back/src/test/java/dev/matheus/`
- **Resources**: `back/src/main/resources/`
- **Docker**: Project root

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and Docling dependencies

- [ ] T001 Add Docling Maven dependencies (docling-serve-client, docling-testcontainers) in back/pom.xml
- [ ] T002 [P] Create Docling Serve configuration in docker-compose.yml at project root
- [ ] T003 [P] Add Docling configuration properties to back/src/main/resources/application.properties
- [ ] T004 [P] Create test fixture PDF with tables at back/src/test/resources/fixtures/test-pdf-with-tables.pdf
- [ ] T005 [P] Create test fixture DOCX with formatting at back/src/test/resources/fixtures/test-docx-formatted.docx
- [ ] T006 [P] Create test fixture multi-column PDF at back/src/test/resources/fixtures/test-multicolumn.pdf

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T007 Create database migration script at back/src/main/resources/db/migration/V20250128__add_docling_entities.sql
- [ ] T008 [P] Create ContentType enum in back/src/main/java/dev/matheus/entity/ContentType.java
- [ ] T009 [P] Create ProcessingStatus enum in back/src/main/java/dev/matheus/entity/ProcessingStatus.java
- [ ] T010 Create DocumentChunk entity in back/src/main/java/dev/matheus/entity/DocumentChunk.java
- [ ] T011 Create ChunkEmbedding entity in back/src/main/java/dev/matheus/entity/ChunkEmbedding.java
- [ ] T012 Modify DocumentFile entity to add processing fields in back/src/main/java/dev/matheus/entity/DocumentFile.java
- [ ] T013 Modify HypoteticalQuestion entity to add chunkId field in back/src/main/java/dev/matheus/entity/HypoteticalQuestion.java
- [ ] T014 Create DoclingConfigProperties config class in back/src/main/java/dev/matheus/service/docling/DoclingConfigProperties.java
- [ ] T015 [P] Create ChunkResponse DTO in back/src/main/java/dev/matheus/dto/ChunkResponse.java
- [ ] T016 [P] Create ProcessingStatusResponse DTO in back/src/main/java/dev/matheus/dto/ProcessingStatusResponse.java

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Upload Complex PDF with Tables (Priority: P1) 🎯 MVP

**Goal**: Extract PDF content including tables and convert to valid markdown chunks with preserved table structure

**Independent Test**: Upload a PDF with embedded tables, verify markdown output contains properly formatted table syntax, confirm table content is indexable for RAG queries

### Tests for User Story 1 (TDD RED Phase) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T017 [P] [US1] Write unit test for PDF table extraction in back/src/test/java/dev/matheus/service/docling/DoclingDocumentParserTest.java
- [ ] T018 [P] [US1] Write unit test for markdown table syntax validation in back/src/test/java/dev/matheus/service/docling/DoclingDocumentParserTest.java
- [ ] T019 [P] [US1] Write integration test for PDF processing with Testcontainers in back/src/test/java/dev/matheus/service/docling/DoclingIntegrationTest.java
- [ ] T020 [US1] Verify all US1 tests FAIL (RED phase complete)

### Implementation for User Story 1 (TDD GREEN Phase)

- [ ] T021 [US1] Create DoclingDocumentParser service in back/src/main/java/dev/matheus/service/docling/DoclingDocumentParser.java
- [ ] T022 [US1] Implement PDF parsing with DoclingServeApi in DoclingDocumentParser
- [ ] T023 [US1] Implement table extraction and markdown conversion in DoclingDocumentParser
- [ ] T024 [US1] Implement multi-page table handling (keep tables together) in DoclingDocumentParser
- [ ] T025 [US1] Run US1 tests and verify all PASS (GREEN phase complete)

### Refactor for User Story 1 (TDD REFACTOR Phase)

- [ ] T026 [US1] Review and refactor DoclingDocumentParser for code quality
- [ ] T027 [US1] Add logging and error handling for PDF processing

**Checkpoint**: User Story 1 complete - PDF with tables can be uploaded and converted to markdown

---

## Phase 4: User Story 2 - Extract Structured Data from DOCX/DOC (Priority: P1)

**Goal**: Extract DOCX/DOC content with formatting (headings, lists) and convert to semantic markdown chunks

**Independent Test**: Upload a DOCX with multiple heading levels, lists, and formatting, verify output maintains hierarchy structure

### Tests for User Story 2 (TDD RED Phase) ⚠️

- [ ] T028 [P] [US2] Write unit test for DOCX heading extraction in back/src/test/java/dev/matheus/service/docling/DoclingDocumentParserTest.java
- [ ] T029 [P] [US2] Write unit test for list grouping in back/src/test/java/dev/matheus/service/docling/DoclingDocumentParserTest.java
- [ ] T030 [P] [US2] Write unit test for formatting preservation (bold, italic, links) in back/src/test/java/dev/matheus/service/docling/DoclingDocumentParserTest.java
- [ ] T031 [US2] Verify all US2 tests FAIL (RED phase complete)

### Implementation for User Story 2 (TDD GREEN Phase)

- [ ] T032 [US2] Extend DoclingDocumentParser to handle DOCX format in back/src/main/java/dev/matheus/service/docling/DoclingDocumentParser.java
- [ ] T033 [US2] Implement heading hierarchy preservation (H1, H2, H3) in DoclingDocumentParser
- [ ] T034 [US2] Implement list grouping and markdown conversion in DoclingDocumentParser
- [ ] T035 [US2] Implement text formatting preservation (bold, italic, links) in DoclingDocumentParser
- [ ] T036 [US2] Run US2 tests and verify all PASS (GREEN phase complete)

### Refactor for User Story 2 (TDD REFACTOR Phase)

- [ ] T037 [US2] Review and refactor DOCX handling for code quality
- [ ] T038 [US2] Ensure consistent markdown output format across PDF and DOCX

**Checkpoint**: User Stories 1 AND 2 complete - Both PDF and DOCX can be processed

---

## Phase 5: User Story 3 - Generate Semantically Meaningful Chunks (Priority: P1)

**Goal**: Implement semantic chunking based on document structure, not arbitrary token counts

**Independent Test**: Process a document, verify each chunk is independently meaningful and contains complete semantic units

### Tests for User Story 3 (TDD RED Phase) ⚠️

- [ ] T039 [P] [US3] Write unit test for semantic boundary detection in back/src/test/java/dev/matheus/service/docling/DoclingChunkingServiceTest.java
- [ ] T040 [P] [US3] Write unit test for table atomicity (tables not split) in back/src/test/java/dev/matheus/service/docling/DoclingChunkingServiceTest.java
- [ ] T041 [P] [US3] Write unit test for paragraph boundary respect in back/src/test/java/dev/matheus/service/docling/DoclingChunkingServiceTest.java
- [ ] T042 [P] [US3] Write unit test for max token handling (split at logical boundaries) in back/src/test/java/dev/matheus/service/docling/DoclingChunkingServiceTest.java
- [ ] T043 [US3] Verify all US3 tests FAIL (RED phase complete)

### Implementation for User Story 3 (TDD GREEN Phase)

- [ ] T044 [US3] Create DoclingChunkingService in back/src/main/java/dev/matheus/service/docling/DoclingChunkingService.java
- [ ] T045 [US3] Implement HybridChunker configuration with max 2000 tokens in DoclingChunkingService
- [ ] T046 [US3] Implement semantic boundary detection (section, paragraph, table) in DoclingChunkingService
- [ ] T047 [US3] Implement table atomicity logic (keep tables together) in DoclingChunkingService
- [ ] T048 [US3] Implement oversized chunk splitting at logical boundaries in DoclingChunkingService
- [ ] T049 [US3] Run US3 tests and verify all PASS (GREEN phase complete)

### Refactor for User Story 3 (TDD REFACTOR Phase)

- [ ] T050 [US3] Review and refactor DoclingChunkingService for code quality
- [ ] T051 [US3] Extract configuration to DoclingConfigProperties

**Checkpoint**: User Stories 1, 2, AND 3 complete - Core MVP functionality working

---

## Phase 6: User Story 4 - Handle Edge Cases & Complex Layouts (Priority: P2)

**Goal**: Gracefully handle multi-column layouts, scanned PDFs, and forms without data loss

**Independent Test**: Upload various edge case documents and verify no data is lost and markdown output is valid

### Tests for User Story 4 (TDD RED Phase) ⚠️

- [ ] T052 [P] [US4] Write unit test for multi-column layout reading order in back/src/test/java/dev/matheus/service/docling/DoclingDocumentParserTest.java
- [ ] T053 [P] [US4] Write unit test for scanned PDF OCR indication in back/src/test/java/dev/matheus/service/docling/DoclingDocumentParserTest.java
- [ ] T054 [P] [US4] Write unit test for form field extraction in back/src/test/java/dev/matheus/service/docling/DoclingDocumentParserTest.java
- [ ] T055 [P] [US4] Write unit test for corrupted document error handling in back/src/test/java/dev/matheus/service/docling/DoclingDocumentParserTest.java
- [ ] T056 [US4] Verify all US4 tests FAIL (RED phase complete)

### Implementation for User Story 4 (TDD GREEN Phase)

- [ ] T057 [US4] Implement multi-column layout handling in DoclingDocumentParser
- [ ] T058 [US4] Implement OCR detection and graceful indication for scanned PDFs in DoclingDocumentParser
- [ ] T059 [US4] Implement form field extraction to markdown in DoclingDocumentParser
- [ ] T060 [US4] Implement robust error handling for corrupted documents in DoclingDocumentParser
- [ ] T061 [US4] Run US4 tests and verify all PASS (GREEN phase complete)

### Refactor for User Story 4 (TDD REFACTOR Phase)

- [ ] T062 [US4] Review and refactor error handling for consistency
- [ ] T063 [US4] Add informative error messages per FR-011

**Checkpoint**: User Stories 1-4 complete - Robust document handling working

---

## Phase 7: User Story 5 - Preserve Semantic Relationships (Priority: P2)

**Goal**: Track chunk relationships and hierarchy metadata for context-aware RAG retrieval

**Independent Test**: Process a multi-section document and verify metadata tracks section relationships

### Tests for User Story 5 (TDD RED Phase) ⚠️

- [ ] T064 [P] [US5] Write unit test for section heading metadata in DocumentChunk in back/src/test/java/dev/matheus/service/docling/DoclingChunkingServiceTest.java
- [ ] T065 [P] [US5] Write unit test for heading level tracking in back/src/test/java/dev/matheus/service/docling/DoclingChunkingServiceTest.java
- [ ] T066 [P] [US5] Write unit test for chunk position ordering in back/src/test/java/dev/matheus/service/docling/DoclingChunkingServiceTest.java
- [ ] T067 [US5] Verify all US5 tests FAIL (RED phase complete)

### Implementation for User Story 5 (TDD GREEN Phase)

- [ ] T068 [US5] Implement section heading extraction to chunk metadata in DoclingChunkingService
- [ ] T069 [US5] Implement heading level (1-6) tracking in chunk metadata in DoclingChunkingService
- [ ] T070 [US5] Implement chunk position ordering within document in DoclingChunkingService
- [ ] T071 [US5] Implement content type detection (TEXT, TABLE, LIST, CODE, HEADING) in DoclingChunkingService
- [ ] T072 [US5] Run US5 tests and verify all PASS (GREEN phase complete)

### Refactor for User Story 5 (TDD REFACTOR Phase)

- [ ] T073 [US5] Review and refactor metadata handling for consistency
- [ ] T074 [US5] Ensure all chunk metadata fields populated correctly

**Checkpoint**: All user stories complete - Full feature functionality working

---

## Phase 8: API Endpoints

**Purpose**: REST API for document processing operations

### Tests for API (TDD RED Phase) ⚠️

- [ ] T075 [P] Write contract test for POST /api/documents/{documentId}/process in back/src/test/java/dev/matheus/resource/DocumentProcessingResourceTest.java
- [ ] T076 [P] Write contract test for GET /api/documents/{documentId}/status in back/src/test/java/dev/matheus/resource/DocumentProcessingResourceTest.java
- [ ] T077 [P] Write contract test for GET /api/documents/{documentId}/chunks in back/src/test/java/dev/matheus/resource/DocumentProcessingResourceTest.java
- [ ] T078 [P] Write contract test for POST /api/documents/{documentId}/reprocess in back/src/test/java/dev/matheus/resource/DocumentProcessingResourceTest.java
- [ ] T079 Verify all API tests FAIL (RED phase complete)

### Implementation for API (TDD GREEN Phase)

- [ ] T080 Create DocumentProcessingResource in back/src/main/java/dev/matheus/resource/DocumentProcessingResource.java
- [ ] T081 Implement POST /api/documents/{documentId}/process endpoint
- [ ] T082 Implement GET /api/documents/{documentId}/status endpoint
- [ ] T083 Implement GET /api/documents/{documentId}/chunks endpoint with pagination
- [ ] T084 Implement POST /api/documents/{documentId}/reprocess endpoint
- [ ] T085 Run API tests and verify all PASS (GREEN phase complete)

### Refactor for API (TDD REFACTOR Phase)

- [ ] T086 Review and refactor API resource for consistency
- [ ] T087 Add validation and proper HTTP status codes

---

## Phase 9: Pipeline Integration

**Purpose**: Integrate Docling with existing document ingestion pipeline

### Tests for Integration (TDD RED Phase) ⚠️

- [ ] T088 [P] Write integration test for full document ingestion pipeline in back/src/test/java/dev/matheus/service/DocumentIngestionServiceTest.java
- [ ] T089 [P] Write integration test for chunk embedding generation in back/src/test/java/dev/matheus/service/HypotheticalQuestionServiceTest.java
- [ ] T090 Verify integration tests FAIL (RED phase complete)

### Implementation for Integration (TDD GREEN Phase)

- [ ] T091 Modify DocumentIngestionService to delegate to DoclingDocumentParser in back/src/main/java/dev/matheus/service/DocumentIngestionService.java
- [ ] T092 Modify HypotheticalQuestionService to use DocumentChunk entities in back/src/main/java/dev/matheus/service/HypotheticalQuestionService.java
- [ ] T093 Wire up chunk embedding generation with ChunkEmbedding entity
- [ ] T094 Implement processing status transitions (PENDING → PROCESSING → COMPLETED/FAILED)
- [ ] T095 Run integration tests and verify all PASS (GREEN phase complete)

### Refactor for Integration (TDD REFACTOR Phase)

- [ ] T096 Review and refactor integration code for clean boundaries
- [ ] T097 Remove or deprecate legacy PDFBox/Tabula code paths

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T098 [P] Run performance validation: verify 30-second processing target for 100-page documents
- [ ] T099 [P] Run full test suite and ensure minimum 70% code coverage for new modules
- [ ] T100 [P] Validate markdown syntax correctness (0% syntax errors per SC-003)
- [ ] T101 Run quickstart.md validation steps
- [ ] T102 Code cleanup: remove unused imports, dead code, TODO comments
- [ ] T103 Final review of all error messages for user-friendliness

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - User stories should be completed in priority order (P1 → P2)
  - US1, US2, US3 are all P1 and form the MVP
- **API Endpoints (Phase 8)**: Depends on at least US1 being complete
- **Pipeline Integration (Phase 9)**: Depends on all P1 user stories (US1, US2, US3)
- **Polish (Phase 10)**: Depends on all desired user stories being complete

### User Story Dependencies

| Story | Priority | Dependencies | Can Start After |
|-------|----------|--------------|-----------------|
| US1 - PDF Tables | P1 | Foundational only | Phase 2 complete |
| US2 - DOCX/DOC | P1 | Foundational only | Phase 2 complete |
| US3 - Semantic Chunks | P1 | US1 or US2 parser exists | Phase 3 or 4 |
| US4 - Edge Cases | P2 | US1-US3 complete | Phase 5 complete |
| US5 - Relationships | P2 | US3 chunker exists | Phase 5 complete |

### Within Each User Story (TDD Cycle)

1. **RED**: Write tests FIRST, ensure they FAIL
2. **GREEN**: Implement minimal code to pass tests
3. **REFACTOR**: Improve code quality while tests pass

### Parallel Opportunities

**Phase 1 (Setup)**:
```
T001 (Maven) → sequential first
T002-T006 → all [P] can run in parallel after T001
```

**Phase 2 (Foundational)**:
```
T007 (Migration) → sequential first
T008-T009 (Enums) → [P] parallel
T010-T013 (Entities) → sequential (depend on enums)
T014-T016 (Config/DTOs) → [P] parallel
```

**User Story Tests**:
```
All tests marked [P] within a user story can run in parallel
Example: T017-T019 can all run in parallel
```

---

## Parallel Example: User Story 1

```bash
# TDD RED - Launch all tests for User Story 1 together:
Task: "T017 [P] [US1] Write unit test for PDF table extraction"
Task: "T018 [P] [US1] Write unit test for markdown table syntax validation"
Task: "T019 [P] [US1] Write integration test for PDF processing with Testcontainers"

# After tests written, verify they FAIL:
Task: "T020 [US1] Verify all US1 tests FAIL (RED phase complete)"

# TDD GREEN - Implement sequentially:
Task: "T021 [US1] Create DoclingDocumentParser service" → T022 → T023 → T024

# Verify tests PASS:
Task: "T025 [US1] Run US1 tests and verify all PASS (GREEN phase complete)"

# TDD REFACTOR:
Task: "T026 [US1] Review and refactor DoclingDocumentParser for code quality"
Task: "T027 [US1] Add logging and error handling for PDF processing"
```

---

## Implementation Strategy

### MVP First (User Stories 1, 2, 3 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (PDF with tables)
4. **CHECKPOINT**: Test US1 independently - upload PDF with tables, verify markdown
5. Complete Phase 4: User Story 2 (DOCX/DOC)
6. **CHECKPOINT**: Test US2 independently - upload DOCX, verify hierarchy
7. Complete Phase 5: User Story 3 (Semantic chunks)
8. **CHECKPOINT**: Test US3 independently - verify chunk boundaries
9. Complete Phase 8: API Endpoints
10. Complete Phase 9: Pipeline Integration
11. **STOP and VALIDATE**: Full MVP testing
12. Deploy/demo if ready

### Full Feature Delivery

1. Complete MVP (above)
2. Complete Phase 6: User Story 4 (Edge cases)
3. Complete Phase 7: User Story 5 (Relationships)
4. Complete Phase 10: Polish
5. Final validation and deploy

### Incremental Delivery

Each user story adds independent value:
- After US1: Can upload PDFs with tables
- After US2: Can upload DOCX/DOC documents
- After US3: Chunks are semantically meaningful
- After US4: Edge cases handled gracefully
- After US5: Better RAG context awareness

---

## Task Summary

| Phase | Tasks | Parallel | Description |
|-------|-------|----------|-------------|
| Phase 1: Setup | T001-T006 | 5 [P] | Infrastructure setup |
| Phase 2: Foundational | T007-T016 | 4 [P] | Core entities and config |
| Phase 3: US1 - PDF Tables | T017-T027 | 3 [P] | TDD for PDF processing |
| Phase 4: US2 - DOCX/DOC | T028-T038 | 3 [P] | TDD for DOCX processing |
| Phase 5: US3 - Chunks | T039-T051 | 4 [P] | TDD for semantic chunking |
| Phase 6: US4 - Edge Cases | T052-T063 | 4 [P] | TDD for robustness |
| Phase 7: US5 - Relationships | T064-T074 | 3 [P] | TDD for metadata |
| Phase 8: API Endpoints | T075-T087 | 4 [P] | TDD for REST API |
| Phase 9: Integration | T088-T097 | 2 [P] | Pipeline wiring |
| Phase 10: Polish | T098-T103 | 3 [P] | Validation and cleanup |

**Total Tasks**: 103
**MVP Tasks** (US1-US3 + API + Integration): ~65 tasks
**Parallel Opportunities**: 35 tasks marked [P]

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label (US1-US5) maps task to specific user story
- Each user story is independently completable and testable
- TDD is MANDATORY: tests MUST fail before implementation
- Commit after each TDD cycle (RED → GREEN → REFACTOR)
- Stop at any checkpoint to validate story independently
