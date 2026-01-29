package dev.matheus.resource;

import dev.matheus.dto.ChunkResponse;
import dev.matheus.dto.ProcessingStatusResponse;
import dev.matheus.entity.*;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * TDD RED PHASE - Integration tests for Document Processing API
 * 
 * Phase 8: API Endpoints for Docling Document Processing
 * 
 * These tests define the contract for 4 REST endpoints:
 * 1. POST /api/documents/{id}/process - Trigger async processing
 * 2. GET /api/documents/{id}/status - Check processing status
 * 3. GET /api/documents/{id}/chunks - Retrieve paginated chunks
 * 4. POST /api/documents/{id}/reprocess - Re-process document
 * 
 * EXPECTED TO FAIL: DocumentProcessingResource does not exist yet.
 * 
 * Test IDs:
 * - T075: Trigger document processing
 * - T076: Get processing status
 * - T077: Get document chunks with pagination
 * - T078: Re-process document
 * - T079: 404 for non-existent documents
 * - T080: 409 when already processing
 * - T081: 400 when chunks not ready
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DocumentProcessingResourceTest {

    @Inject
    EntityManager em;

    private String testDocumentId;
    private String processedDocumentId;
    private String processingDocumentId;

    @BeforeEach
    @Transactional
    public void setupTestData() {
        // Clean up any existing test data
        em.createQuery("DELETE FROM DocumentChunk").executeUpdate();
        em.createQuery("DELETE FROM DocumentFile").executeUpdate();
        em.createQuery("DELETE FROM ChatMessage").executeUpdate();
        em.createQuery("DELETE FROM Chat").executeUpdate();

        // Create test chat and documents
        Chat chat1 = new Chat();
        chat1.title = "Test Chat 1";
        em.persist(chat1);

        Chat chat2 = new Chat();
        chat2.title = "Test Chat 2";
        em.persist(chat2);

        Chat chat3 = new Chat();
        chat3.title = "Test Chat 3";
        em.persist(chat3);

        em.flush(); // Generate IDs

        // Document 1: PENDING status (ready to process)
        DocumentFile pendingDoc = new DocumentFile();
        pendingDoc.chat = chat1;
        pendingDoc.fileName = "test-pending.pdf";
        pendingDoc.fileType = "application/pdf";
        pendingDoc.fileSize = 1024L;
        pendingDoc.fileData = "dummy pdf content".getBytes();
        pendingDoc.uploadedAt = LocalDateTime.now();
        pendingDoc.processingStatus = ProcessingStatus.PENDING;
        em.persist(pendingDoc);
        em.flush();
        testDocumentId = pendingDoc.id;

        // Document 2: COMPLETED status with chunks (for status and chunks endpoints)
        DocumentFile completedDoc = new DocumentFile();
        completedDoc.chat = chat2;
        completedDoc.fileName = "test-completed.pdf";
        completedDoc.fileType = "application/pdf";
        completedDoc.fileSize = 2048L;
        completedDoc.fileData = "dummy pdf content 2".getBytes();
        completedDoc.uploadedAt = LocalDateTime.now();
        completedDoc.processingStatus = ProcessingStatus.COMPLETED;
        completedDoc.processedAt = LocalDateTime.now();
        completedDoc.chunkCount = 10;
        completedDoc.processorVersion = "docling-1.0.0";
        em.persist(completedDoc);
        em.flush();
        processedDocumentId = completedDoc.id;

        // Create 10 chunks for the completed document
        for (int i = 0; i < 10; i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.documentFile = completedDoc;
            chunk.content = "Chunk content " + i;
            chunk.contentType = i % 3 == 0 ? dev.matheus.entity.ContentType.TABLE : dev.matheus.entity.ContentType.TEXT;
            chunk.position = i;
            chunk.sectionHeading = "Section " + (i / 3);
            chunk.headingLevel = 1;
            chunk.tokenCount = 50 + i * 10;
            chunk.createdAt = LocalDateTime.now();
            em.persist(chunk);
        }

        // Document 3: PROCESSING status (for 409 conflict test)
        DocumentFile processingDoc = new DocumentFile();
        processingDoc.chat = chat3;
        processingDoc.fileName = "test-processing.pdf";
        processingDoc.fileType = "application/pdf";
        processingDoc.fileSize = 3072L;
        processingDoc.fileData = "dummy pdf content 3".getBytes();
        processingDoc.uploadedAt = LocalDateTime.now();
        processingDoc.processingStatus = ProcessingStatus.PROCESSING;
        em.persist(processingDoc);
        em.flush();
        processingDocumentId = processingDoc.id;
    }

    /**
     * T075: Test triggering document processing
     * 
     * POST /api/documents/{id}/process
     * 
     * Given: A DocumentFile with PENDING status
     * When: POST to /api/documents/{uuid}/process
     * Then: 
     *   - Returns HTTP 202 Accepted
     *   - Response body contains ProcessingStatusResponse
     *   - status = PROCESSING
     *   - documentId matches request
     */
    @Test
    @Order(1)
    public void shouldTriggerDocumentProcessing() {
        // When: Trigger processing for pending document
        var response = given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/documents/" + testDocumentId + "/process")
                .then()
                .statusCode(202)
                .contentType(ContentType.JSON)
                .body("documentId", equalTo(testDocumentId))
                .body("status", equalTo("PROCESSING"))
                .extract()
                .as(ProcessingStatusResponse.class);

        // Then: Verify response structure
        assertThat(response).isNotNull();
        assertThat(response.documentId()).isEqualTo(testDocumentId);
        assertThat(response.status()).isEqualTo(ProcessingStatus.PROCESSING);
    }

    /**
     * T076: Test getting processing status
     * 
     * GET /api/documents/{id}/status
     * 
     * Given: A DocumentFile with COMPLETED status and chunks
     * When: GET to /api/documents/{uuid}/status
     * Then:
     *   - Returns HTTP 200 OK
     *   - Response contains status=COMPLETED
     *   - chunkCount = 10
     *   - processedAt timestamp exists
     *   - processorVersion exists
     */
    @Test
    @Order(2)
    public void shouldGetProcessingStatus() {
        // When: Get status for completed document
        var response = given()
                .when()
                .get("/api/documents/" + processedDocumentId + "/status")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("documentId", equalTo(processedDocumentId))
                .body("status", equalTo("COMPLETED"))
                .body("chunkCount", equalTo(10))
                .body("processedAt", notNullValue())
                .body("processorVersion", equalTo("docling-1.0.0"))
                .extract()
                .as(ProcessingStatusResponse.class);

        // Then: Verify response structure
        assertThat(response).isNotNull();
        assertThat(response.documentId()).isEqualTo(processedDocumentId);
        assertThat(response.status()).isEqualTo(ProcessingStatus.COMPLETED);
        assertThat(response.chunkCount()).isEqualTo(10);
        assertThat(response.processedAt()).isNotNull();
        assertThat(response.processorVersion()).isEqualTo("docling-1.0.0");
    }

    /**
     * T077: Test getting document chunks with pagination
     * 
     * GET /api/documents/{id}/chunks?page=0&size=5
     * 
     * Given: A processed DocumentFile with 10 DocumentChunk entities
     * When: GET to /api/documents/{uuid}/chunks?page=0&size=5
     * Then:
     *   - Returns HTTP 200 OK
     *   - Response contains 5 chunks
     *   - Chunks are in correct position order (0-4)
     *   - totalCount = 10
     *   - page = 0
     *   - size = 5
     *   - hasMore = true
     */
    @Test
    @Order(3)
    public void shouldGetDocumentChunksWithPagination() {
        // When: Get first page of chunks
        var response = given()
                .queryParam("page", 0)
                .queryParam("size", 5)
                .when()
                .get("/api/documents/" + processedDocumentId + "/chunks")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("chunks", hasSize(5))
                .body("totalCount", equalTo(10))
                .body("page", equalTo(0))
                .body("size", equalTo(5))
                .body("hasMore", equalTo(true))
                .extract()
                .jsonPath();

        // Then: Verify chunk structure and ordering
        List<ChunkResponse> chunks = response.getList("chunks", ChunkResponse.class);
        assertThat(chunks).hasSize(5);
        
        // Verify chunks are in position order
        for (int i = 0; i < 5; i++) {
            assertThat(chunks.get(i).position()).isEqualTo(i);
            assertThat(chunks.get(i).content()).isEqualTo("Chunk content " + i);
            assertThat(chunks.get(i).tokenCount()).isGreaterThan(0);
        }
    }

    /**
     * T077b: Test getting second page of chunks
     * 
     * Verifies pagination works correctly for subsequent pages
     */
    @Test
    @Order(4)
    public void shouldGetSecondPageOfChunks() {
        // When: Get second page of chunks
        var response = given()
                .queryParam("page", 1)
                .queryParam("size", 5)
                .when()
                .get("/api/documents/" + processedDocumentId + "/chunks")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("chunks", hasSize(5))
                .body("totalCount", equalTo(10))
                .body("page", equalTo(1))
                .body("size", equalTo(5))
                .body("hasMore", equalTo(false)) // Last page
                .extract()
                .jsonPath();

        // Then: Verify chunks are positions 5-9
        List<ChunkResponse> chunks = response.getList("chunks", ChunkResponse.class);
        assertThat(chunks).hasSize(5);
        
        for (int i = 0; i < 5; i++) {
            assertThat(chunks.get(i).position()).isEqualTo(i + 5);
        }
    }

    /**
     * T077c: Test filtering chunks by content type
     * 
     * Verifies contentType query parameter works
     */
    @Test
    @Order(5)
    public void shouldFilterChunksByContentType() {
        // When: Get only TABLE chunks
        var response = given()
                .queryParam("contentType", "TABLE")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/documents/" + processedDocumentId + "/chunks")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("chunks", hasSize(4)) // Positions 0, 3, 6, 9
                .extract()
                .jsonPath();

        // Then: Verify all chunks are TABLE type
        List<ChunkResponse> chunks = response.getList("chunks", ChunkResponse.class);
        assertThat(chunks).hasSize(4);
        assertThat(chunks).allMatch(c -> c.contentType() == dev.matheus.entity.ContentType.TABLE);
    }

    /**
     * T078: Test re-processing a document
     * 
     * POST /api/documents/{id}/reprocess
     * 
     * Given: A DocumentFile with COMPLETED status and existing chunks
     * When: POST to /api/documents/{uuid}/reprocess
     * Then:
     *   - Returns HTTP 202 Accepted
     *   - Response contains status=PROCESSING
     *   - Old chunks should be deleted (verified in implementation)
     *   - processingStatus reset to PROCESSING
     */
    @Test
    @Order(6)
    public void shouldReprocessDocument() {
        // When: Trigger reprocessing
        var response = given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/documents/" + processedDocumentId + "/reprocess")
                .then()
                .statusCode(202)
                .contentType(ContentType.JSON)
                .body("documentId", equalTo(processedDocumentId))
                .body("status", equalTo("PROCESSING"))
                .extract()
                .as(ProcessingStatusResponse.class);

        // Then: Verify response
        assertThat(response).isNotNull();
        assertThat(response.documentId()).isEqualTo(processedDocumentId);
        assertThat(response.status()).isEqualTo(ProcessingStatus.PROCESSING);
    }

    /**
     * T079: Test 404 for non-existent document
     * 
     * All endpoints should return 404 when document doesn't exist
     */
    @Test
    @Order(7)
    public void shouldReturn404ForNonExistentDocument() {
        String nonExistentId = UUID.randomUUID().toString();

        // Test /process endpoint
        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/documents/" + nonExistentId + "/process")
                .then()
                .statusCode(404);

        // Test /status endpoint
        given()
                .when()
                .get("/api/documents/" + nonExistentId + "/status")
                .then()
                .statusCode(404);

        // Test /chunks endpoint
        given()
                .when()
                .get("/api/documents/" + nonExistentId + "/chunks")
                .then()
                .statusCode(404);

        // Test /reprocess endpoint
        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/documents/" + nonExistentId + "/reprocess")
                .then()
                .statusCode(404);
    }

    /**
     * T080: Test 409 when document already processing
     * 
     * POST /api/documents/{id}/process should return 409 Conflict
     * if document is already in PROCESSING state
     */
    @Test
    @Order(8)
    public void shouldReturn409WhenAlreadyProcessing() {
        // When: Try to process a document that's already processing
        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/documents/" + processingDocumentId + "/process")
                .then()
                .statusCode(409);
    }

    /**
     * T081: Test 400 when chunks not ready
     * 
     * GET /api/documents/{id}/chunks should return 400 Bad Request
     * if document hasn't been processed yet (PENDING status)
     */
    @Test
    @Order(9)
    public void shouldReturn400WhenChunksNotReady() {
        // When: Try to get chunks for a pending document
        given()
                .when()
                .get("/api/documents/" + testDocumentId + "/chunks")
                .then()
                .statusCode(400);
    }

    /**
     * T082: Test processing with options
     * 
     * POST /api/documents/{id}/process with ProcessingOptions body
     * 
     * Tests that custom processing options can be sent in request body
     * Reuses testDocumentId since it's still PENDING after failed HTTP call
     */
    @Test
    @Order(10)
    public void shouldAcceptProcessingOptions() {
        // When: Send processing options in request body
        String requestBody = """
                {
                    "chunkingStrategy": "HYBRID",
                    "maxTokens": 1500,
                    "generateHypotheticalQuestions": true
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/documents/" + testDocumentId + "/process")
                .then()
                .statusCode(202)
                .body("status", equalTo("PROCESSING"));
    }

    /**
     * T083: Test default pagination size
     * 
     * Verifies default pagination parameters work when not specified
     */
    @Test
    @Order(11)
    public void shouldUseDefaultPaginationParameters() {
        // When: Get chunks without pagination params
        given()
                .when()
                .get("/api/documents/" + processedDocumentId + "/chunks")
                .then()
                .statusCode(200)
                .body("chunks", hasSize(10)) // All chunks fit in default size
                .body("page", equalTo(0))
                .body("size", equalTo(20)) // Default size from spec
                .body("hasMore", equalTo(false));
    }
}
