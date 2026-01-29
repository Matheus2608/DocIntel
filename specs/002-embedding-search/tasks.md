# Tasks: Embedding Search & Management

**Input**: Design documents from `/specs/002-embedding-search/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/embedding-search-api.yaml, research.md

**Tests**: Constitution mandates TDD - tests are REQUIRED (TDD subagents will be used)

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `back/src/main/java/dev/matheus/`
- **Backend Tests**: `back/src/test/java/dev/matheus/`
- **Frontend**: `front/src/`
- **Frontend Tests**: `front/src/__tests__/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create shared types and DTOs that all user stories depend on

- [X] T001 [P] Create TypeScript types for embedding API in `front/src/types/embedding.ts`
- [X] T002 [P] Create SearchQuery record DTO in `back/src/main/java/dev/matheus/dto/EmbeddingSearchRequest.java`
- [X] T003 [P] Create SearchResponse record DTO in `back/src/main/java/dev/matheus/dto/EmbeddingSearchResponse.java`
- [X] T004 [P] Create EmbeddingEntry record DTO in `back/src/main/java/dev/matheus/dto/EmbeddingEntryResponse.java`
- [X] T005 [P] Create AddEntryRequest record DTO in `back/src/main/java/dev/matheus/dto/EmbeddingAddRequest.java`
- [X] T006 [P] Create UpdateEntryRequest record DTO in `back/src/main/java/dev/matheus/dto/EmbeddingUpdateRequest.java`
- [X] T007 [P] Create AddEntryResponse record DTO in `back/src/main/java/dev/matheus/dto/EmbeddingAddResponse.java`
- [X] T008 [P] Create UpdateEntryResponse record DTO in `back/src/main/java/dev/matheus/dto/EmbeddingUpdateResponse.java`
- [X] T009 [P] Create ErrorResponse record DTO in `back/src/main/java/dev/matheus/dto/ErrorResponse.java`
- [X] T010 Add API base URL for embeddings endpoint to `front/src/config.js`

**Checkpoint**: All DTOs and types ready - service implementation can begin

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core service infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T011 Create EmbeddingSearchService skeleton with dependency injection in `back/src/main/java/dev/matheus/service/EmbeddingSearchService.java`
- [X] T012 Create EmbeddingSearchResource skeleton with JAX-RS annotations in `back/src/main/java/dev/matheus/resource/EmbeddingSearchResource.java`
- [X] T013 Create useEmbeddingSearch hook skeleton in `front/src/hooks/useEmbeddingSearch.ts`
- [X] T014 Create EmbeddingSearch page skeleton in `front/src/pages/EmbeddingSearch.tsx`
- [X] T015 Add route for EmbeddingSearch page in `front/src/App.jsx`
- [X] T016 Add navigation link to EmbeddingSearch in `front/src/components/Sidebar.jsx`

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Search Embeddings by Text Query (Priority: P1) 🎯 MVP

**Goal**: Users can search embeddings by text query and see results sorted by similarity score

**Independent Test**: Submit any text query → receive ranked results with similarity scores and metadata

### Tests for User Story 1 (TDD RED Phase)

> **NOTE**: Write these tests FIRST using `tdd-test-writer` subagent, ensure they FAIL before implementation

- [X] T017 [P] [US1] Write failing integration test for POST /api/embeddings/search in `back/src/test/java/dev/matheus/resource/EmbeddingSearchResourceTest.java`
- [X] T018 [P] [US1] Write failing unit test for EmbeddingSearchService.search() in `back/src/test/java/dev/matheus/service/EmbeddingSearchServiceTest.java`
- [ ] T019 [P] [US1] Write failing component test for EmbeddingSearchForm in `front/src/__tests__/components/EmbeddingSearchForm.test.tsx`
- [ ] T020 [P] [US1] Write failing component test for EmbeddingSearchResults in `front/src/__tests__/components/EmbeddingSearchResults.test.tsx`

### Implementation for User Story 1 (TDD GREEN Phase)

> **NOTE**: Implement using `tdd-implementer` subagent to pass the failing tests

- [X] T021 [US1] Implement EmbeddingSearchService.search() method using LangChain4j EmbeddingStore.search() in `back/src/main/java/dev/matheus/service/EmbeddingSearchService.java`
- [X] T022 [US1] Implement POST /api/embeddings/search endpoint with validation in `back/src/main/java/dev/matheus/resource/EmbeddingSearchResource.java`
- [X] T023 [US1] Implement search() function in useEmbeddingSearch hook in `front/src/hooks/useEmbeddingSearch.ts`
- [X] T024 [P] [US1] Implement EmbeddingSearchForm component with text input and search button in `front/src/components/EmbeddingSearchForm.tsx`
- [X] T025 [P] [US1] Implement EmbeddingSearchResults component with expandable cards in `front/src/components/EmbeddingSearchResults.tsx`
- [X] T026 [US1] Wire up EmbeddingSearch page with form and results components in `front/src/components/EmbeddingSearch.tsx`
- [X] T027 [US1] Add CSS styles for search page components (using Tailwind CSS)
- [X] T028 [US1] Add empty state and no-results messaging to EmbeddingSearchResults
- [X] T029 [US1] Add loading state and error handling to search flow

### Refactor for User Story 1 (TDD REFACTOR Phase)

> **NOTE**: Use `tdd-refactorer` subagent to improve code quality while keeping tests passing

- [X] T030 [US1] Refactor and optimize User Story 1 implementation (backend)

**Checkpoint**: User Story 1 (Search) fully functional and independently testable - this is the MVP

---

## Phase 4: User Story 2 - Add New Embedding Entries (Priority: P2)

**Goal**: Users can manually add text content to the embedding database with optional metadata

**Independent Test**: Add text via form → verify it appears in subsequent searches

### Tests for User Story 2 (TDD RED Phase)

- [ ] T031 [P] [US2] Write failing integration test for POST /api/embeddings in `back/src/test/java/dev/matheus/resource/EmbeddingSearchResourceTest.java`
- [X] T032 [P] [US2] Write failing unit test for EmbeddingSearchService.addEntry() in `back/src/test/java/dev/matheus/service/EmbeddingSearchServiceTest.java`
- [ ] T033 [P] [US2] Write failing component test for EmbeddingAddModal in `front/src/__tests__/components/EmbeddingAddModal.test.tsx`

### Implementation for User Story 2 (TDD GREEN Phase)

- [X] T034 [US2] Implement EmbeddingSearchService.addEntry() method with embedding generation in `back/src/main/java/dev/matheus/service/EmbeddingSearchService.java`
- [X] T035 [US2] Implement POST /api/embeddings endpoint with validation in `back/src/main/java/dev/matheus/resource/EmbeddingSearchResource.java`
- [X] T036 [US2] Implement addEntry() function in useEmbeddingSearch hook in `front/src/hooks/useEmbeddingSearch.ts`
- [X] T037 [US2] Implement EmbeddingAddModal component with form fields in `front/src/components/EmbeddingAddModal.tsx`
- [X] T038 [US2] Add "Add Entry" button to EmbeddingSearch page that opens modal in `front/src/components/EmbeddingSearch.tsx`
- [X] T039 [US2] Add CSS styles for add modal (using Tailwind CSS)
- [X] T040 [US2] Add form validation (required text, optional metadata) to EmbeddingAddModal
- [X] T041 [US2] Add success message and automatic results refresh after adding entry

### Refactor for User Story 2 (TDD REFACTOR Phase)

- [X] T042 [US2] Refactor and optimize User Story 2 implementation (backend)

**Checkpoint**: User Stories 1 AND 2 both work independently - can search and add entries

---

## Phase 5: User Story 3 - Update Existing Embedding Entries (Priority: P3)

**Goal**: Users can modify existing entries (metadata or text with re-embedding)

**Independent Test**: Click edit on search result → modify text → verify changes in subsequent search

### Tests for User Story 3 (TDD RED Phase)

- [ ] T043 [P] [US3] Write failing integration test for GET /api/embeddings/{id} in `back/src/test/java/dev/matheus/resource/EmbeddingSearchResourceTest.java`
- [ ] T044 [P] [US3] Write failing integration test for PUT /api/embeddings/{id} in `back/src/test/java/dev/matheus/resource/EmbeddingSearchResourceTest.java`
- [X] T045 [P] [US3] Write failing unit test for EmbeddingSearchService.getEntry() in `back/src/test/java/dev/matheus/service/EmbeddingSearchServiceTest.java`
- [X] T046 [P] [US3] Write failing unit test for EmbeddingSearchService.updateEntry() in `back/src/test/java/dev/matheus/service/EmbeddingSearchServiceTest.java`
- [ ] T047 [P] [US3] Write failing component test for EmbeddingEditModal in `front/src/__tests__/components/EmbeddingEditModal.test.tsx`

### Implementation for User Story 3 (TDD GREEN Phase)

- [X] T048 [US3] Implement EmbeddingSearchService.getEntry() method with filter-based lookup in `back/src/main/java/dev/matheus/service/EmbeddingSearchService.java`
- [X] T049 [US3] Implement EmbeddingSearchService.updateEntry() method with delete+add pattern in `back/src/main/java/dev/matheus/service/EmbeddingSearchService.java`
- [X] T050 [US3] Implement GET /api/embeddings/{id} endpoint in `back/src/main/java/dev/matheus/resource/EmbeddingSearchResource.java`
- [X] T051 [US3] Implement PUT /api/embeddings/{id} endpoint with validation in `back/src/main/java/dev/matheus/resource/EmbeddingSearchResource.java`
- [X] T052 [US3] Implement getEntry() and updateEntry() functions in useEmbeddingSearch hook in `front/src/hooks/useEmbeddingSearch.ts`
- [X] T053 [US3] Implement EmbeddingEditModal component with pre-populated form in `front/src/components/EmbeddingEditModal.tsx`
- [X] T054 [US3] Add edit button to result cards in EmbeddingSearchResults that opens edit modal
- [X] T055 [US3] Add CSS styles for edit modal (using Tailwind CSS)
- [X] T056 [US3] Add success message and automatic results refresh after updating entry
- [X] T057 [US3] Add error handling for "entry not found" and update failures

### Refactor for User Story 3 (TDD REFACTOR Phase)

- [X] T058 [US3] Refactor and optimize User Story 3 implementation

**Checkpoint**: All user stories (Search, Add, Update) are independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [X] T059 [P] Add DELETE /api/embeddings/{id} endpoint for completeness in `back/src/main/java/dev/matheus/resource/EmbeddingSearchResource.java`
- [ ] T060 [P] Add audit logging for all add/update/delete operations in EmbeddingSearchService
- [ ] T061 [P] Add request debouncing to prevent duplicate submissions in useEmbeddingSearch hook
- [X] T062 [P] Add keyboard shortcuts (Enter to search, Escape to close modals)
- [ ] T063 Run quickstart.md validation - verify all curl examples work
- [ ] T064 Performance testing with 1000+ entries - verify < 2 second response time
- [ ] T065 Final code review and cleanup

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User stories can then proceed sequentially (P1 → P2 → P3)
  - Or in parallel if multiple developers available
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Shares useEmbeddingSearch hook with US1
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Depends on US1 result cards for edit button

### Within Each User Story (TDD Cycle)

1. **RED**: Write failing tests first (`tdd-test-writer` subagent)
2. **GREEN**: Implement minimal code to pass tests (`tdd-implementer` subagent)
3. **REFACTOR**: Improve code quality (`tdd-refactorer` subagent)

### Parallel Opportunities

**Phase 1 (Setup)**:
```
T001, T002, T003, T004, T005, T006, T007, T008, T009 (all [P])
```

**User Story 1 Tests**:
```
T017, T018, T019, T020 (all [P] [US1])
```

**User Story 1 Implementation**:
```
T024, T025 (both [P] [US1] - different component files)
```

**User Story 2 Tests**:
```
T031, T032, T033 (all [P] [US2])
```

**User Story 3 Tests**:
```
T043, T044, T045, T046, T047 (all [P] [US3])
```

**Polish Phase**:
```
T059, T060, T061, T062 (all [P])
```

---

## Parallel Example: User Story 1 Tests

```bash
# Launch all tests for User Story 1 together:
Task(subagent_type="tdd-test-writer"): "Write failing integration test for POST /api/embeddings/search"
Task(subagent_type="tdd-test-writer"): "Write failing unit test for EmbeddingSearchService.search()"
Task(subagent_type="tdd-test-writer"): "Write failing component test for EmbeddingSearchForm"
Task(subagent_type="tdd-test-writer"): "Write failing component test for EmbeddingSearchResults"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (DTOs and types)
2. Complete Phase 2: Foundational (service skeletons, routing)
3. Complete Phase 3: User Story 1 - Search (TDD cycle)
4. **STOP and VALIDATE**: Test search independently
5. Deploy/demo if ready - **this is the MVP**

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add User Story 1 (Search) → Test independently → **MVP Ready**
3. Add User Story 2 (Add) → Test independently → Deploy/Demo
4. Add User Story 3 (Update) → Test independently → Deploy/Demo
5. Polish → Production ready

### TDD Subagent Usage

For each user story phase:

1. **RED Phase**: `Task(subagent_type="tdd-test-writer", prompt="Write failing tests for [component/endpoint]")`
2. **GREEN Phase**: `Task(subagent_type="tdd-implementer", prompt="Implement [component/endpoint] to pass tests")`
3. **REFACTOR Phase**: `Task(subagent_type="tdd-refactorer", prompt="Refactor [component/endpoint] while keeping tests passing")`

---

## Notes

- [P] tasks = different files, no dependencies, can run in parallel
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Verify tests FAIL before implementing (TDD RED phase)
- Commit after each TDD cycle completion
- Stop at any checkpoint to validate story independently
- Use TDD subagents as specified in AGENTS.md - never manual coding for features
