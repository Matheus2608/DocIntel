# Feature Specification: Embedding Search & Management

**Feature Branch**: `002-embedding-search`
**Created**: 2026-01-28
**Status**: Draft
**Input**: Create a page to search text in pgvector embedding store, showing results with text, metadata, and similarity scores in descending order; also add/update entries in the embedding database

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORITIZED as user journeys ordered by importance.
  Each user story/journey must be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.
  
  Assign priorities (P1, P2, P3, etc.) to each story, where P1 is the most critical.
  Think of each story as a standalone slice of functionality that can be:
  - Developed independently
  - Tested independently
  - Deployed independently
  - Demonstrated to users independently
-->

### User Story 1 - Search Embeddings by Text Query (Priority: P1)

A user wants to explore what content exists in the embedding database by submitting a text query and seeing the most relevant chunks. This is useful for understanding what documents have been ingested, discovering related content across uploaded files, and debugging the embedding/chunking quality.

**Why this priority**: This is the core feature that delivers immediate value - users can search their ingested documents through a dedicated interface without the conversational chatbot interaction. It's the foundation for all other functionality.

**Independent Test**: A user can submit any text query, receive ranked search results with similarity scores, and understand what content matched and why (through metadata). This alone provides value for exploring the embedding database.

**Acceptance Scenarios**:

1. **Given** a user is on the Embedding Search page, **When** they enter a text query and click "Search", **Then** the system displays matching embedding entries sorted by similarity score in descending order with the original chunk text visible
2. **Given** search results are displayed, **When** a result row is expanded, **Then** the user sees complete metadata including file name, similarity score, and semantic ranking score
3. **Given** multiple results exist, **When** results are displayed, **Then** the highest similarity scores appear first (descending order)
4. **Given** a user performs an empty or whitespace-only search, **When** they click "Search", **Then** an error message explains that a valid query is required
5. **Given** a search returns no results above the minimum threshold, **When** results load, **Then** a message indicates "No matching content found" with suggestions to try different terms

---

### User Story 2 - Add New Embedding Entries (Priority: P2)

A user wants to manually add new text content to the embedding database without uploading an entire document. This enables quick testing of embeddings, adding reference material, or supplementing existing ingested documents with additional context.

**Why this priority**: Enables power users and developers to test embedding quality and manage database content. Less critical than search but adds testing/management capabilities.

**Independent Test**: A user can submit text content with optional metadata, the system generates an embedding and stores it, and subsequent searches can find that newly added content.

**Acceptance Scenarios**:

1. **Given** a user is on the Embedding Search page, **When** they click "Add Entry", **Then** a form appears with fields for text content and optional metadata (file name/source identifier)
2. **Given** the add entry form is open, **When** the user enters text and clicks "Add to Database", **Then** the system generates an embedding for that text, stores it with the provided metadata, and shows a success message
3. **Given** an entry was successfully added, **When** the user performs a search, **Then** the newly added entry can be found in search results if it's relevant to the query
4. **Given** the text field is empty, **When** the user attempts to submit, **Then** a validation error prevents submission and prompts for required content
5. **Given** an entry has been added, **When** the same text is searched for, **Then** it appears in results with appropriate similarity score

---

### User Story 3 - Update Existing Embedding Entries (Priority: P3)

A user wants to modify existing embedding entries - either updating the associated metadata or updating the text content (which triggers re-embedding). This supports content corrections and metadata refinement.

**Why this priority**: Nice-to-have for power users managing embedding database content. Useful but not essential for MVP search functionality.

**Independent Test**: A user can select an existing entry from search results, edit its metadata or text, and the changes persist and are reflected in subsequent searches.

**Acceptance Scenarios**:

1. **Given** a user has search results displayed, **When** they click an edit button on a result row, **Then** an edit form appears pre-populated with the current text, file name, and other metadata
2. **Given** the edit form is open, **When** the user modifies metadata fields and clicks "Update", **Then** the changes are saved and a success message confirms the update
3. **Given** the user modifies the text content in the edit form, **When** they click "Update", **Then** the system re-generates the embedding from the new text, updates it in the database, and reflects the change in subsequent searches
4. **Given** an entry has been updated, **When** the user closes the edit form, **Then** the search results automatically refresh to show the updated entry
5. **Given** an entry update fails, **When** the error occurs, **Then** an error message explains the issue and the user can retry

---

### Edge Cases

- What happens when embedding generation takes longer than expected? (Implement timeout handling with user feedback)
- How does the system handle very long text inputs? (Consider chunking very long submissions to respect embedding model limits)
- What happens if the pgvector database becomes unavailable? (Return appropriate error message to user)
- Should there be a limit on number of custom entries a user can add? (Implementation decision - assume unlimited for MVP)
- How to handle duplicate/near-duplicate entries? (No deduplication required in MVP - user responsible for avoiding duplicates)

## Requirements *(mandatory)*

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right functional requirements.
-->

### Functional Requirements

- **FR-001**: System MUST provide a dedicated page/interface for searching embeddings with a text input field and search button
- **FR-002**: System MUST query pgvector embeddings using the user-submitted text, converting it to an embedding vector before search
- **FR-003**: System MUST return embedding matches sorted in descending order by similarity score
- **FR-004**: System MUST display search results showing: original chunk text, similarity score (embedding-based), semantic ranking score (if available), and file name metadata
- **FR-005**: System MUST allow users to expand or view full result details including all available metadata fields
- **FR-006**: System MUST support adding new embedding entries through a form that accepts text content and optional metadata (source/file name)
- **FR-007**: System MUST automatically generate embeddings for user-submitted text using the same embedding model (text-embedding-3-small, 768 dimensions) used during document ingestion
- **FR-008**: System MUST store newly added entries in pgvector with their embeddings and metadata
- **FR-009**: System MUST support updating existing embedding entries, allowing users to modify metadata (file name, custom key-value pairs)
- **FR-010**: System MUST support updating text content of existing entries and re-generate their embeddings when text is modified
- **FR-011**: System MUST provide form validation requiring non-empty text content before allowing add/update operations
- **FR-012**: System MUST return appropriate error messages when search fails or returns no results
- **FR-013**: System MUST prevent concurrent duplicate submissions (basic debouncing of add/update/search buttons)
- **FR-014**: System MUST log all add/update operations for audit purposes

### Key Entities *(include if feature involves data)*

- **EmbeddingEntry**: Represents a single embedded text segment
  - `id`: Unique identifier
  - `text`: Original text content that was embedded
  - `embedding_vector`: 768-dimensional vector (pgvector)
  - `similarity_score`: Cosine similarity score from embedding search (0.0-1.0)
  - `model_score`: Semantic ranking score from VertexAI (0.0-1.0) - optional/lazy-loaded
  - `metadata`: Flexible JSON object containing:
    - `file_name`: Source document or custom identifier
    - `custom_fields`: User-provided key-value pairs (implementation detail)
  - `created_at`: Timestamp of entry creation
  - `updated_at`: Timestamp of last modification

- **SearchQuery**: User's search request
  - `text`: Query text
  - `min_similarity`: Optional threshold (default 0.7)
  - `max_results`: Result limit (default 10)

## Success Criteria *(mandatory)*

<!--
  ACTION REQUIRED: Define measurable success criteria.
  These must be technology-agnostic and measurable.
-->

### Measurable Outcomes

- **SC-001**: Users can submit a text query and receive results in under 2 seconds for typical database sizes (< 100k embeddings)
- **SC-002**: Search results are accurately ranked by similarity score with the highest matching embeddings appearing first
- **SC-003**: Users can add a new embedding entry and successfully search for it within 5 seconds of addition
- **SC-004**: All search results display required metadata fields (text, similarity score, file name) without requiring additional clicks
- **SC-005**: Users can update embedding entries (text or metadata) and changes persist correctly (verified by subsequent search)
- **SC-006**: Form validation prevents invalid submissions (empty text) with clear error messages, achieving 100% validation coverage
- **SC-007**: The interface supports searching across database sizes up to 50,000 entries without performance degradation
- **SC-008**: Newly added entries are immediately available in subsequent searches (no cache staleness)

## Assumptions

- **Embedding Model**: The same embedding model (text-embedding-3-small, 768 dimensions) used during document ingestion will be available for generating embeddings for user-submitted text
- **Database Availability**: PostgreSQL with pgvector extension is always available and performant
- **Metadata Structure**: Custom metadata for added entries will use simple key-value pairs (no nested objects required in MVP)
- **Search Scope**: All searches query the entire embedding database regardless of file source (no per-file filtering in initial version)
- **User Authorization**: All authenticated users can search and add entries (no role-based restrictions in MVP)
- **Batch Operations**: Updates are single-entry only; no bulk operations required
- **Semantic Ranking**: VertexAI semantic ranker availability is assumed but graceful degradation to embedding similarity only is acceptable

## Design Notes

- **Frontend**: React component(s) with React Query for API calls and form state management
- **Backend**: New REST API endpoints for search, add, and update operations
- **Integration Points**: Leverage existing embedding generation service (already used during document ingestion)
- **UI Patterns**: Match existing design system (custom CSS variables, no new dependencies)
