package dev.matheus.service.docling;

import dev.matheus.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ContentTypeDetector
 */
class ContentTypeDetectorTest {

    private ContentTypeDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ContentTypeDetector();
    }

    @Test
    void shouldDetectTableContent() {
        String content = """
                | Column 1 | Column 2 |
                |----------|----------|
                | Value 1  | Value 2  |
                """;

        ContentType type = detector.detect(content);

        assertThat(type).isEqualTo(ContentType.TABLE);
    }

    @Test
    void shouldDetectHeadingContent() {
        String content = "# This is a heading";

        ContentType type = detector.detect(content);

        assertThat(type).isEqualTo(ContentType.HEADING);
    }

    @Test
    void shouldDetectListContent() {
        String content = """
                - Item 1
                - Item 2
                - Item 3
                """;

        ContentType type = detector.detect(content);

        assertThat(type).isEqualTo(ContentType.LIST);
    }

    @Test
    void shouldDetectCodeContent() {
        String content = """
                ```java
                public class Test {}
                ```
                """;

        ContentType type = detector.detect(content);

        assertThat(type).isEqualTo(ContentType.CODE);
    }

    @Test
    void shouldDetectMixedContent() {
        String content = """
                # Heading
                
                | Column 1 | Column 2 |
                |----------|----------|
                | Value 1  | Value 2  |
                """;

        ContentType type = detector.detect(content);

        assertThat(type).isEqualTo(ContentType.MIXED);
    }

    @Test
    void shouldDetectTextForPlainContent() {
        String content = "This is plain text without any markdown formatting.";

        ContentType type = detector.detect(content);

        assertThat(type).isEqualTo(ContentType.TEXT);
    }

    @Test
    void shouldDetectTextForEmptyContent() {
        ContentType type = detector.detect("");

        assertThat(type).isEqualTo(ContentType.TEXT);
    }

    @Test
    void shouldDetectTextForNullContent() {
        ContentType type = detector.detect(null);

        assertThat(type).isEqualTo(ContentType.TEXT);
    }
}
