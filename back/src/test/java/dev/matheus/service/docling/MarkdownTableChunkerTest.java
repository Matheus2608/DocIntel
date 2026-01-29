package dev.matheus.service.docling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MarkdownTableChunker
 */
class MarkdownTableChunkerTest {

    private MarkdownTableChunker chunker;
    private TokenEstimator tokenEstimator;

    @BeforeEach
    void setUp() {
        tokenEstimator = new TokenEstimator();
        chunker = new MarkdownTableChunker(500, tokenEstimator); // 500 tokens max
    }

    @Test
    void shouldKeepTableInSingleChunk() {
        String content = """
                | Column 1 | Column 2 |
                |----------|----------|
                | Value 1  | Value 2  |
                | Value 3  | Value 4  |
                """;

        List<String> chunks = chunker.split(content);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("|");
    }

    @Test
    void shouldSeparateTextFromTable() {
        String content = """
                This is some text before the table.
                
                | Column 1 | Column 2 |
                |----------|----------|
                | Value 1  | Value 2  |
                
                This is text after the table.
                """;

        List<String> chunks = chunker.split(content);

        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        
        // Find table chunk
        boolean hasTableChunk = chunks.stream().anyMatch(chunk -> 
            chunk.contains("|") && chunk.contains("Column 1"));
        assertThat(hasTableChunk).isTrue();
    }

    @Test
    void shouldHandleMultipleTables() {
        String content = """
                | Table 1 |
                |---------|
                | Value   |
                
                Some text
                
                | Table 2 |
                |---------|
                | Value   |
                """;

        List<String> chunks = chunker.split(content);

        // Should have at least 2 table chunks
        long tableChunks = chunks.stream()
                .filter(chunk -> chunk.contains("|"))
                .count();
        
        assertThat(tableChunks).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldSplitLargeTextIntoMultipleChunks() {
        // Create text larger than max tokens (500 tokens = 2000 characters)
        // Add newlines to create separate content blocks
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            content.append("word ".repeat(100)).append("\n\n"); // Each block ~500 characters
        }

        List<String> chunks = chunker.split(content.toString());

        // Should split into multiple chunks
        assertThat(chunks).hasSizeGreaterThan(1);
    }

    @Test
    void shouldHandleEmptyContent() {
        List<String> chunks = chunker.split("");

        assertThat(chunks).isEmpty();
    }
}
