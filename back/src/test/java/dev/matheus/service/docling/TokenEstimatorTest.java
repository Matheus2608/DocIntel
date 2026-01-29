package dev.matheus.service.docling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TokenEstimator
 */
class TokenEstimatorTest {

    private TokenEstimator estimator;

    @BeforeEach
    void setUp() {
        estimator = new TokenEstimator();
    }

    @Test
    void shouldEstimateTokensForShortText() {
        String content = "Hello World"; // 11 characters

        int tokens = estimator.estimate(content);

        // 11 / 4 = 2.75 -> 2 tokens
        assertThat(tokens).isEqualTo(2);
    }

    @Test
    void shouldEstimateTokensForLongText() {
        // 100 characters = 25 tokens
        String content = "a".repeat(100);

        int tokens = estimator.estimate(content);

        assertThat(tokens).isEqualTo(25);
    }

    @Test
    void shouldReturnMinimumOneTokenForNonEmptyContent() {
        String content = "Hi"; // 2 characters -> 0.5 tokens -> minimum 1

        int tokens = estimator.estimate(content);

        assertThat(tokens).isEqualTo(1);
    }

    @Test
    void shouldReturnZeroForEmptyContent() {
        int tokens = estimator.estimate("");

        assertThat(tokens).isEqualTo(0);
    }

    @Test
    void shouldReturnZeroForNullContent() {
        int tokens = estimator.estimate(null);

        assertThat(tokens).isEqualTo(0);
    }
}
