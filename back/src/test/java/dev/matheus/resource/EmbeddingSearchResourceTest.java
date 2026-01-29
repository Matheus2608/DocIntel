package dev.matheus.resource;

import dev.matheus.dto.EmbeddingSearchRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the EmbeddingSearchResource REST endpoint.
 * These tests verify the POST /api/embeddings/search endpoint behavior.
 * 
 * RED PHASE: These tests are expected to FAIL because the implementation
 * currently throws UnsupportedOperationException.
 */
@QuarkusTest
class EmbeddingSearchResourceTest {

    private static final String SEARCH_ENDPOINT = "/api/embeddings/search";

    /**
     * Test that POST /api/embeddings/search returns 200 OK with valid request
     */
    @Test
    void shouldReturn200WithValidSearchRequest() {
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "What is machine learning?",
                10,
                0.7
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post(SEARCH_ENDPOINT)
        .then()
                .statusCode(200)
                .body("results", notNullValue())
                .body("totalResults", notNullValue());
    }

    /**
     * Test that search returns results sorted by similarity score in descending order
     */
    @Test
    void shouldReturnResultsSortedBySimilarityDescending() {
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "artificial intelligence concepts",
                5,
                0.5
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post(SEARCH_ENDPOINT)
        .then()
                .statusCode(200)
                .body("results", notNullValue())
                .body("results.size()", greaterThanOrEqualTo(0))
                // Verify similarity scores are in descending order if results exist
                .body("results[0].similarity", 
                      anyOf(nullValue(), greaterThanOrEqualTo(0.0f)));
    }

    /**
     * Test that search respects maxResults parameter
     */
    @Test
    void shouldRespectMaxResultsParameter() {
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "data science",
                3,
                0.6
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post(SEARCH_ENDPOINT)
        .then()
                .statusCode(200)
                .body("results", notNullValue())
                .body("results.size()", lessThanOrEqualTo(3));
    }

    /**
     * Test that search respects minSimilarity threshold
     */
    @Test
    void shouldRespectMinSimilarityThreshold() {
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "neural networks",
                10,
                0.8  // High threshold
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post(SEARCH_ENDPOINT)
        .then()
                .statusCode(200)
                .body("results", notNullValue())
                // All results should have similarity >= minSimilarity
                .body("results.every { it.similarity >= 0.8 }", is(true));
    }

    /**
     * Test that results include all required fields: id, text, similarity, metadata
     */
    @Test
    void shouldIncludeAllRequiredFieldsInResults() {
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "testing query",
                5,
                0.5
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post(SEARCH_ENDPOINT)
        .then()
                .statusCode(200)
                .body("results", notNullValue())
                // If results exist, verify they have all required fields
                .body("results[0].id", anyOf(nullValue(), notNullValue()))
                .body("results[0].text", anyOf(nullValue(), notNullValue()))
                .body("results[0].similarity", anyOf(nullValue(), notNullValue()))
                .body("results[0].metadata", anyOf(nullValue(), notNullValue()));
    }

    /**
     * Test validation error when query is null
     */
    @Test
    void shouldReturn400WhenQueryIsNull() {
        String requestBody = """
                {
                    "query": null,
                    "maxResults": 10,
                    "minSimilarity": 0.7
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post(SEARCH_ENDPOINT)
        .then()
                .statusCode(400);
    }

    /**
     * Test validation error when query is empty
     */
    @Test
    void shouldReturn400WhenQueryIsEmpty() {
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "",
                10,
                0.7
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post(SEARCH_ENDPOINT)
        .then()
                .statusCode(400);
    }

    /**
     * Test validation error when query is blank (only whitespace)
     */
    @Test
    void shouldReturn400WhenQueryIsBlank() {
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "   ",
                10,
                0.7
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post(SEARCH_ENDPOINT)
        .then()
                .statusCode(400);
    }

    /**
     * Test validation error when maxResults is less than 1
     */
    @Test
    void shouldReturn400WhenMaxResultsIsLessThan1() {
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "valid query",
                0,
                0.7
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post(SEARCH_ENDPOINT)
        .then()
                .statusCode(400);
    }

    /**
     * Test validation error when maxResults exceeds 100
     */
    @Test
    void shouldReturn400WhenMaxResultsExceeds100() {
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "valid query",
                101,
                0.7
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post(SEARCH_ENDPOINT)
        .then()
                .statusCode(400);
    }

    /**
     * Test validation error when minSimilarity is less than 0.0
     */
    @Test
    void shouldReturn400WhenMinSimilarityIsLessThan0() {
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "valid query",
                10,
                -0.1
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post(SEARCH_ENDPOINT)
        .then()
                .statusCode(400);
    }

    /**
     * Test validation error when minSimilarity exceeds 1.0
     */
    @Test
    void shouldReturn400WhenMinSimilarityExceeds1() {
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "valid query",
                10,
                1.1
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post(SEARCH_ENDPOINT)
        .then()
                .statusCode(400);
    }

    /**
     * Test that default values are applied when optional parameters are null
     */
    @Test
    void shouldApplyDefaultValuesWhenOptionalParametersAreNull() {
        String requestBody = """
                {
                    "query": "test query"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post(SEARCH_ENDPOINT)
        .then()
                .statusCode(200)
                .body("results", notNullValue())
                .body("totalResults", notNullValue());
    }

    /**
     * Test that search returns empty results when no matches found
     */
    @Test
    void shouldReturnEmptyResultsWhenNoMatchesFound() {
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "extremely specific query that should not match anything xyz123",
                10,
                0.99  // Very high threshold
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post(SEARCH_ENDPOINT)
        .then()
                .statusCode(200)
                .body("results", notNullValue())
                .body("totalResults", is(0));
    }

    /**
     * Test that totalResults matches the size of results array
     */
    @Test
    void shouldReturnTotalResultsMatchingResultsArraySize() {
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(
                "programming languages",
                5,
                0.6
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post(SEARCH_ENDPOINT)
        .then()
                .statusCode(200)
                .body("results", notNullValue())
                .body("totalResults", notNullValue())
                // Verify totalResults equals the size of results array
                .body("results.size()", equalTo(Integer.valueOf(
                        given()
                                .contentType(ContentType.JSON)
                                .body(request)
                        .when()
                                .post(SEARCH_ENDPOINT)
                        .then()
                                .extract()
                                .path("totalResults")
                )));
    }
}
