# Research: Docling-Based Document Ingestion

**Feature**: 001-docling-ingestion
**Date**: 2025-01-28
**Status**: Complete

## Research Summary

This document captures research findings for integrating Docling Java into DocIntel's document ingestion pipeline, replacing the current PDFBox + Tabula approach.

---

## 1. Docling Java Library Architecture

### Decision: Use Docling Java Client with Docling Serve Backend

**Rationale**: Docling Java is a client library that connects to Docling Serve, a Python-based backend running in Docker. This architecture provides:
- Advanced PDF understanding (layout, reading order, table structure)
- Consistent document representation via DoclingDocument
- Native chunking strategies aligned to document structure
- Testcontainers support for reliable integration testing

**Alternatives Considered**:

| Alternative | Rejected Because |
|-------------|------------------|
| Keep PDFBox + Tabula | Current approach loses table structure, poor chunking boundaries, causing app failures |
| Pure Python Docling via subprocess | Complex IPC, error handling nightmare, no type safety |
| Apache Tika only | No native table structure extraction, no semantic chunking |

### Maven Artifacts Required

```xml
<!-- Docling Java Client -->
<dependency>
    <groupId>ai.docling</groupId>
    <artifactId>docling-serve-client</artifactId>
    <version>0.4.4</version>
</dependency>

<!-- Testcontainers for Integration Tests -->
<dependency>
    <groupId>ai.docling</groupId>
    <artifactId>docling-testcontainers</artifactId>
    <version>0.4.4</version>
    <scope>test</scope>
</dependency>
```

---

## 2. Docling Serve Deployment

### Decision: Run Docling Serve as Docker Container

**Rationale**: Docling Serve is the official backend providing document processing. Running as Docker container:
- Isolates Python dependencies from Java runtime
- Easy scaling and deployment
- Consistent environment across dev/test/prod
- Official Docker image maintained by IBM

**Docker Configuration**:
```yaml
# docker-compose.yml addition
docling-serve:
  image: docling-project/docling-serve:latest
  ports:
    - "5000:5000"
  environment:
    - DOCLING_WORKERS=2
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:5000/health"]
    interval: 30s
    timeout: 10s
    retries: 3
```

**Alternatives Considered**:

| Alternative | Rejected Because |
|-------------|------------------|
| Embedded Python runtime | Complex dependency management, version conflicts |
| External hosted service | Latency concerns, data privacy, vendor lock-in |
| Sidecar in Kubernetes | Adds complexity for local development |

---

## 3. Chunking Strategy

### Decision: Use Docling's HybridChunker

**Rationale**: Docling provides native chunking that operates on DoclingDocument structure:

1. **HierarchicalChunker**: Creates one chunk per document element (section, paragraph, table)
   - Preserves document hierarchy metadata
   - Keeps tables as atomic units
   - Merges list items by default

2. **HybridChunker** (chosen): Combines hierarchical with tokenization awareness
   - Splits oversized chunks based on token count
   - Merges undersized consecutive chunks with matching headers
   - Configurable max tokens (target: 2000 for RAG)
   - Supports OpenAI tokenizers

**Why HybridChunker over HierarchicalChunker**:
- Prevents chunks from exceeding embedding model limits
- Avoids tiny chunks that lack context
- Maintains semantic coherence while respecting size constraints

**Alternatives Considered**:

| Alternative | Rejected Because |
|-------------|------------------|
| Keep CustomTableAwareSplitter | Operates on raw text, loses document structure context |
| Fixed token-count splitting | Breaks semantic units (tables, paragraphs) arbitrarily |
| Sentence-based splitting | Tables and lists need different treatment than prose |

---

## 4. Document Processing Pipeline

### Decision: Replace PDFBox + Tabula with Docling Client

**Current Pipeline** (to be replaced):
```
PDF → PDFBox (text) + Tabula (tables) → TextNormalizer → CustomTableAwareSplitter → Embeddings
```

**New Pipeline**:
```
Document → DoclingServeClient → DoclingDocument → HybridChunker → Markdown Chunks → Embeddings
```

**Key Changes**:
1. Single entry point for all document types (PDF, DOCX, TXT)
2. Table structure preserved natively (no marker-based extraction)
3. Markdown output with proper table syntax
4. Chunk metadata includes document hierarchy (section, heading level)

---

## 5. Integration with Existing LangChain4j

### Decision: Keep LangChain4j Embedding Pipeline, Replace Document Loading

**Rationale**: The existing embedding and retrieval infrastructure (pgvector, OpenAI embeddings) works well. Only the document loading and chunking needs replacement.

**Integration Points**:
- DoclingDocument → convert to LangChain4j TextSegment with metadata
- Preserve metadata: source document, section hierarchy, content type (text/table)
- Existing HypotheticalQuestionService can consume new chunks
- Embedding store remains unchanged

**Code Pattern**:
```java
// New DoclingDocumentParser implementing LangChain4j DocumentParser
public class DoclingDocumentParser implements DocumentParser {
    private final DoclingServeApi doclingApi;

    @Override
    public Document parse(InputStream inputStream) {
        // 1. Send to Docling Serve
        // 2. Get DoclingDocument with markdown
        // 3. Apply HybridChunker
        // 4. Return LangChain4j Document with segments
    }
}
```

---

## 6. Testing Strategy

### Decision: Use Testcontainers for Integration Tests

**Rationale**: docling-testcontainers provides Docker-based Docling Serve for reliable, reproducible tests.

**Test Categories**:

1. **Unit Tests**: Mock DoclingServeApi, test chunking logic
2. **Integration Tests**: Real Docling Serve via Testcontainers
   - PDF with tables extraction
   - DOCX with formatting preservation
   - Chunk boundary verification
   - Markdown syntax validation

**Test Fixtures Required**:
- `test-pdf-with-tables.pdf`: Multi-page PDF with complex tables
- `test-docx-formatted.docx`: DOCX with headings, lists, formatting
- `test-scanned.pdf`: Image-based PDF (OCR test)
- `test-multicolumn.pdf`: Complex layout test

---

## 7. Configuration

### Decision: Externalize Docling Configuration

**Application Properties**:
```properties
# Docling Serve connection
docling.serve.url=http://localhost:5000
docling.serve.timeout=60s

# Chunking configuration
docling.chunking.strategy=hybrid
docling.chunking.max-tokens=2000
docling.chunking.tokenizer=openai

# Feature flags
docling.enabled=true
docling.fallback-to-legacy=false
```

---

## 8. Migration Strategy

### Decision: Parallel Processing with Feature Flag

**Rationale**: Minimize risk by allowing gradual rollout.

**Phases**:
1. **Phase 1**: Add Docling alongside existing pipeline (feature flag)
2. **Phase 2**: Process new documents with Docling, keep legacy for existing
3. **Phase 3**: Re-process existing documents with Docling (background job)
4. **Phase 4**: Remove legacy pipeline

**Backwards Compatibility**: Existing embeddings remain valid. New documents use new chunking. Optional re-indexing for existing documents.

---

## Sources

- [Docling Java Documentation](https://docling-project.github.io/docling-java/0.4.4/)
- [Docling GitHub Repository](https://github.com/docling-project/docling-java)
- [Docling Chunking Concepts](https://docling-project.github.io/docling/concepts/chunking/)
- [IBM Docling Announcement](https://research.ibm.com/blog/docling-generative-AI)
