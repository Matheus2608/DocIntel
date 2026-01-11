package dev.matheus.service.pdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextNormalizerTest {

    @Test
    void shouldRemoveGarbageMarkers() {
        String input = "This is clean text [[LIXO_INICIO]]garbage text[[LIXO_FIM]] more clean text";
        String result = TextNormalizer.normalize(input);

        assertFalse(result.contains("[[LIXO_INICIO]]"));
        assertFalse(result.contains("[[LIXO_FIM]]"));
        assertFalse(result.contains("garbage text"));
        assertTrue(result.contains("This is clean text"));
        assertTrue(result.contains("more clean text"));
    }

    @Test
    void shouldFixSplitWords() {
        String input = "C a m p e o n a t o";
        String result = TextNormalizer.normalize(input);

        assertEquals("Campeonato", result);
    }

    @Test
    void shouldJoinBrokenLines() {
        String input = "This is a sentence that\ncontinues on next line.";
        String result = TextNormalizer.normalize(input);

        assertFalse(result.contains("\n"));
        assertTrue(result.contains("continues on next line"));
    }

    @Test
    void shouldCleanMultipleSpaces() {
        String input = "Too    many     spaces";
        String result = TextNormalizer.normalize(input);

        assertEquals("Too many spaces", result);
    }

    @Test
    void shouldHandleNullAndEmpty() {
        assertEquals("", TextNormalizer.normalize(null));
        assertEquals("", TextNormalizer.normalize(""));
        assertEquals("", TextNormalizer.normalize("   "));
    }

    @Test
    void shouldHandleComplexCase() {
        String input = "Camp eonato\n" +
                "[[LIXO_INICIO]]table content[[LIXO_FIM]]\n" +
                "This is a sentence that\n" +
                "continues here.  Multiple   spaces.";

        String result = TextNormalizer.normalize(input);

        assertFalse(result.contains("[[LIXO"));
        assertFalse(result.contains("table content"));
        assertTrue(result.contains("Campeonato"));
        assertTrue(result.contains("continues here"));
        assertFalse(result.contains("  "));
    }
}

