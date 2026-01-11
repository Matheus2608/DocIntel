package dev.matheus.service.retrieval;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.matheus.ai.ScoringModel;
import dev.matheus.dto.RetrievalSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrievalSegmentProcessorTest {

    @Mock
    private ScoringModel scoringModel;

    @InjectMocks
    private RetrievalSegmentProcessor processor;


    @Test
    void shouldProcessMatchesAndRemoveDuplicates() {
        when(scoringModel.score(anyString(), anyString())).thenReturn(8.5);

        TextSegment segment1 = TextSegment.from("Question 1", new Metadata().put("PARAGRAPH", "Same paragraph"));
        TextSegment segment2 = TextSegment.from("Question 2", new Metadata().put("PARAGRAPH", "Same paragraph"));
        TextSegment segment3 = TextSegment.from("Question 3", new Metadata().put("PARAGRAPH", "Different paragraph"));

        EmbeddingMatch<TextSegment> match1 = new EmbeddingMatch<>(0.9, "id1", null, segment1);
        EmbeddingMatch<TextSegment> match2 = new EmbeddingMatch<>(0.85, "id2", null, segment2);
        EmbeddingMatch<TextSegment> match3 = new EmbeddingMatch<>(0.8, "id3", null, segment3);

        List<RetrievalSegment> result = processor.processMatches(
                List.of(match1, match2, match3),
                "test question"
        );

        // Should have only 2 segments because first two have same paragraph
        assertEquals(2, result.size());
    }

    @Test
    void shouldFilterByMinScore() {
        List<RetrievalSegment> segments = List.of(
                new RetrievalSegment("q1", "p1", 0.9, 8.5),
                new RetrievalSegment("q2", "p2", 0.85, 7.2),
                new RetrievalSegment("q3", "p3", 0.8, 6.0),
                new RetrievalSegment("q4", "p4", 0.75, 5.5)
        );

        List<RetrievalSegment> filtered = processor.filterAndSort(segments, 7.0, 10);

        assertEquals(2, filtered.size());
        assertEquals(8.5, filtered.get(0).modelScore());
        assertEquals(7.2, filtered.get(1).modelScore());
    }

    @Test
    void shouldSortByScoreDescending() {
        List<RetrievalSegment> segments = List.of(
                new RetrievalSegment("q1", "p1", 0.9, 6.0),
                new RetrievalSegment("q2", "p2", 0.85, 8.5),
                new RetrievalSegment("q3", "p3", 0.8, 7.2)
        );

        List<RetrievalSegment> sorted = processor.filterAndSort(segments, 5.0, 10);

        assertEquals(8.5, sorted.get(0).modelScore());
        assertEquals(7.2, sorted.get(1).modelScore());
        assertEquals(6.0, sorted.get(2).modelScore());
    }

    @Test
    void shouldLimitToMaxResults() {
        List<RetrievalSegment> segments = List.of(
                new RetrievalSegment("q1", "p1", 0.9, 9.0),
                new RetrievalSegment("q2", "p2", 0.85, 8.0),
                new RetrievalSegment("q3", "p3", 0.8, 7.0),
                new RetrievalSegment("q4", "p4", 0.75, 6.0)
        );

        List<RetrievalSegment> limited = processor.filterAndSort(segments, 5.0, 2);

        assertEquals(2, limited.size());
        assertEquals(9.0, limited.get(0).modelScore());
        assertEquals(8.0, limited.get(1).modelScore());
    }
}

