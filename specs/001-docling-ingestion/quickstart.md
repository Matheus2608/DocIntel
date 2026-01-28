# Quickstart: Docling Document Ingestion

**Feature**: 001-docling-ingestion
**Date**: 2025-01-28

## Prerequisites

1. **Docker** installed and running
2. **Java 21+** with Maven 3.9+
3. **PostgreSQL 14+** with pgvector extension
4. Existing DocIntel backend running

---

## Step 1: Start Docling Serve

Add Docling Serve to your docker-compose.yml:

```yaml
services:
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

Start the container:
```bash
docker-compose up -d docling-serve
```

Verify it's running:
```bash
curl http://localhost:5000/health
# Expected: {"status": "ok"}
```

---

## Step 2: Add Maven Dependencies

Add to `back/pom.xml`:

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

## Step 3: Configure Application

Add to `application.properties`:

```properties
# Docling Serve connection
docling.serve.url=http://localhost:5000
docling.serve.timeout=60s

# Chunking configuration
docling.chunking.strategy=hybrid
docling.chunking.max-tokens=2000

# Feature flags
docling.enabled=true
```

---

## Step 4: Run Database Migration

Execute migration script:
```bash
cd back
psql -U postgres -d docintel -f src/main/resources/db/migration/V20250128__add_docling_entities.sql
```

Or if using Flyway, migrations run automatically on startup.

---

## Step 5: Verify Integration

### Test Document Processing

```bash
# Upload a test PDF
curl -X POST http://localhost:8080/api/chats/{chatId}/documents \
  -F "file=@test-document.pdf"

# Check processing status
curl http://localhost:8080/api/documents/{documentId}/status

# Get generated chunks
curl http://localhost:8080/api/documents/{documentId}/chunks
```

### Expected Response (chunks)

```json
{
  "chunks": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "content": "## Introduction\n\nThis document describes...",
      "contentType": "TEXT",
      "position": 0,
      "sectionHeading": "Introduction",
      "headingLevel": 2,
      "tokenCount": 156
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "content": "| Product | Price | Quantity |\n|---------|-------|----------|\n| Widget A | $10 | 100 |",
      "contentType": "TABLE",
      "position": 1,
      "sectionHeading": "Pricing",
      "headingLevel": 2,
      "tokenCount": 45
    }
  ],
  "totalCount": 15,
  "page": 0,
  "size": 20
}
```

---

## Step 6: Run Tests

### Unit Tests
```bash
cd back
./mvnw test -Dtest=DoclingDocumentParserTest
```

### Integration Tests (requires Docker)
```bash
cd back
./mvnw verify -Pit
```

---

## Troubleshooting

### Docling Serve not responding
```bash
# Check container logs
docker logs docling-serve

# Restart container
docker-compose restart docling-serve
```

### Connection timeout
- Increase `docling.serve.timeout` in application.properties
- Check network connectivity between Quarkus and Docker container

### Table extraction fails
- Verify PDF has extractable tables (not images)
- Check Docling Serve logs for parsing errors
- For image-based tables, ensure OCR is enabled in Docling config

---

## Next Steps

1. **TDD Implementation**: Follow RED-GREEN-REFACTOR cycle
2. **Test Coverage**: Minimum 70% for new services
3. **Performance Testing**: Verify 30-second processing target
4. **Migration**: Re-process existing documents if needed
