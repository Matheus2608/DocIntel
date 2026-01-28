# Feature Specification: Docling-Based Document Ingestion & Markdown Chunking

**Feature Branch**: `001-docling-ingestion`
**Created**: 2025-01-27
**Status**: Draft
**Input**: User description: "update app to use docling for document ingestion with enhanced markdown chunk extraction supporting tables, structured data, and complex PDF layouts"

## User Scenarios & Testing

### User Story 1 - Upload Complex PDF with Tables (Priority: P1)

User uploads a PDF document containing text, tables, and structured data. The system extracts all content and converts it to meaningful markdown chunks that preserve table structure, relationships, and semantic meaning.

**Why this priority**: PDF with tables is the core use case causing current app failures. Fixing this unblocks RAG retrieval accuracy and document comprehension. Direct value to users uploading financial reports, research papers, technical documentation.

**Independent Test**: Can be fully tested by uploading a PDF with embedded table, verifying markdown output contains properly formatted table syntax, and confirming table content is indexable for RAG queries.

**Acceptance Scenarios**:

1. **Given** a user uploads a PDF with a pricing table, **When** the system processes it, **Then** the markdown output MUST contain valid markdown table syntax with headers and rows preserved
2. **Given** a PDF with both text paragraphs and inline tables, **When** the system chunks the content, **Then** table context MUST be maintained in chunks that reference it
3. **Given** a complex table spanning multiple PDF pages, **When** the system processes it, **Then** the entire table MUST be kept together in a single chunk to preserve semantic cohesion

---

### User Story 2 - Extract Structured Data from DOCX/DOC (Priority: P1)

User uploads a Word document with various formatting (lists, headings, embedded objects). The system extracts all structured elements and converts them to semantic markdown chunks that reflect document hierarchy.

**Why this priority**: DOCX/DOC are common document formats in business. Current implementation may lose formatting. This fixes a major ingestion gap blocking production readiness.

**Independent Test**: Can be fully tested by uploading a DOCX with multiple heading levels, lists, and formatting, verifying output maintains hierarchy structure and all text content is captured without loss.

**Acceptance Scenarios**:

1. **Given** a DOCX document with H1, H2, H3 headings and nested lists, **When** the system processes it, **Then** the markdown output MUST reflect the hierarchy with correct heading levels
2. **Given** a DOCX with bullet points and numbered lists, **When** the system chunks content, **Then** list items MUST be grouped together where semantically appropriate
3. **Given** a DOCX with text formatting (bold, italic, links), **When** the system converts to markdown, **Then** formatting MUST be preserved as valid markdown syntax

---

### User Story 3 - Generate Semantically Meaningful Chunks (Priority: P1)

System automatically determines optimal chunk boundaries based on content semantics (paragraphs, sections, tables) rather than arbitrary token counts. Chunks are meaningful units that can answer questions independently.

**Why this priority**: Chunking strategy directly impacts RAG quality. Poor chunking causes incomplete context, lost table data, and wrong answers. This is the root cause of current app failures.

**Independent Test**: Can be fully tested by processing a document, verifying each chunk is independently meaningful and contains complete semantic units (don't cut sentences mid-clause or split tables across chunks).

**Acceptance Scenarios**:

1. **Given** a document with multiple sections and a complex table, **When** the system chunks content, **Then** no semantic unit (paragraph, table, list) MUST be split across chunk boundaries unless unavoidable
2. **Given** a chunk that would exceed maximum size if kept whole, **When** the system decides to split, **Then** the split MUST occur at logical boundaries (paragraph breaks, section boundaries)
3. **Given** a section containing a table and surrounding context, **When** the system chunks, **Then** the table and immediately surrounding explanatory text MUST be in the same chunk

---

### User Story 4 - Handle Edge Cases & Complex Layouts (Priority: P2)

System gracefully handles PDFs with multi-column layouts, scanned images with OCR, forms, and unconventional document structures without data loss or corruption.

**Why this priority**: Improves robustness and prevents silent failures on edge case documents. Necessary for production stability but not blocking basic functionality.

**Independent Test**: Can be fully tested by uploading various edge case documents (scanned PDF, multi-column layout, form-heavy document) and verifying no data is lost and markdown output is valid.

**Acceptance Scenarios**:

1. **Given** a PDF with two-column layout, **When** the system processes it, **Then** text MUST be extracted in logical reading order
2. **Given** a scanned PDF (image-based, not text), **When** system processes it, **Then** system MUST either perform OCR or gracefully indicate OCR unavailable
3. **Given** a complex form with fields and data, **When** system extracts, **Then** form structure MUST be interpretable from markdown output

---

### User Story 5 - Preserve Semantic Relationships (Priority: P2)

System identifies and preserves relationships between chunks: which chunks are part of same section, which contain related tables/figures, enabling context-aware RAG retrieval.

**Why this priority**: Improves RAG retrieval accuracy by maintaining document structure context. Enables more intelligent question answering but not strictly required for MVP.

**Independent Test**: Can be fully tested by processing a multi-section document with cross-references and verifying metadata tracks section relationships.

**Acceptance Scenarios**:

1. **Given** a document with sections and subsections, **When** chunks are generated, **Then** chunk metadata MUST indicate parent section and depth in document hierarchy
2. **Given** a table in section A referenced in section B, **When** chunks are generated, **Then** metadata MUST indicate the relationship for context-aware retrieval

---

### Edge Cases

- What happens when a PDF is corrupted or contains unextractable content (images without OCR)?
- How does system handle extremely large documents (1000+ pages) that would create thousands of chunks?
- What happens when tables have irregular structure (missing cells, merged cells)?
- How does system handle documents with mixed languages or special Unicode characters?
- What happens when a document update is uploaded for the same chat (should it re-ingest or update chunks)?

## Requirements

### Functional Requirements

- **FR-001**: System MUST accept PDF, DOCX, DOC, and TXT file uploads (minimum support)
- **FR-002**: System MUST use Docling library for document parsing and structure extraction
- **FR-003**: System MUST convert extracted document content to valid markdown format
- **FR-004**: System MUST preserve table structures in markdown table syntax (pipe-delimited format minimum)
- **FR-005**: System MUST preserve heading hierarchies and document structure
- **FR-006**: System MUST generate semantic chunks based on content structure, not arbitrary token limits
- **FR-007**: System MUST NOT split semantic units (tables, paragraphs, lists) across chunk boundaries unless unavoidable
- **FR-008**: System MUST handle chunk size limits gracefully (maximum chunk size when semantic unit exceeds limit)
- **FR-009**: System MUST store chunk metadata including: source document, chunk position, heading hierarchy, content type
- **FR-010**: System MUST validate markdown output for syntax correctness before storage
- **FR-011**: System MUST provide informative error messages when document processing fails
- **FR-012**: System MUST support re-ingestion of document updates in existing chats
- **FR-013**: System MUST index all extracted chunks for RAG retrieval with preserved markdown formatting

### Key Entities

- **Document**: Uploaded file (PDF, DOCX, DOC, TXT) with metadata (name, upload time, file size, format)
- **ProcessedDocument**: Result of parsing document with Docling, containing extracted elements and structure
- **DocumentChunk**: Individual semantic chunk of document with content, metadata, and relationships
- **ChunkMetadata**: Includes source document ID, position in document, heading hierarchy, content type (text/table/list), relationships to other chunks

## Success Criteria

### Measurable Outcomes

- **SC-001**: 95% of uploaded documents are successfully parsed without errors or data loss
- **SC-002**: Document ingestion completes within 30 seconds for documents up to 100 pages
- **SC-003**: Generated markdown output is valid and parseable (0% syntax errors in batch validation)
- **SC-004**: 100% of table content in PDFs is correctly extracted and preserved in markdown format
- **SC-005**: RAG retrieval accuracy improves by minimum 40% compared to previous chunking strategy (measured by user satisfaction with answer relevance)
- **SC-006**: Users can successfully upload and query complex documents (tables, multiple sections) that previously caused app failures
- **SC-007**: Zero data loss for extracted document content (all visible content captured in markdown output)
- **SC-008**: Chunk generation preserves semantic units with 90%+ user agreement that chunks are logical, complete units
- **SC-009**: System handles edge cases (scanned PDFs, complex layouts) gracefully with appropriate user feedback

## Assumptions

- Docling library is compatible with existing Java/Quarkus backend
- Document uploads are validated for file type and size before ingestion pipeline
- Maximum chunk size is configurable (target 2000 tokens for optimal RAG performance)
- Table extraction must handle standard table layouts (Docling supports these natively)
- OCR for scanned PDFs is out of scope for MVP (system indicates when document is image-based)
- Concurrent document uploads are within current infrastructure capacity
- Users prefer accuracy of chunk boundaries over processing speed

## Constraints

- **File Size**: Single document upload limited to 50MB (configurable)
- **Processing Time**: Must complete within 60 seconds for documents up to 200 pages
- **Format Support**: Minimum PDF, DOCX, TXT (DOC optional based on Docling support)
- **Markdown Output**: Must be compatible with existing RAG/embedding pipeline
- **Backwards Compatibility**: Existing ingested documents should not be affected; only new uploads use new pipeline
