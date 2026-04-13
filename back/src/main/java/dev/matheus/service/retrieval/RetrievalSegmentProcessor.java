package dev.matheus.service.retrieval;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.vertexai.VertexAiScoringModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.matheus.dto.RetrievalSegment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes and scores retrieved segments.
 */
@ApplicationScoped
public class RetrievalSegmentProcessor {
    private static final Logger LOG = Logger.getLogger(RetrievalSegmentProcessor.class);
    private static final String PARAGRAPH_KEY = "PARAGRAPH";

    @Inject
    VertexAiScoringModel scoringModel;

    /**
     * Processes matches by scoring them and converting to RetrievalSegments.
     * Removes duplicates based on paragraph content.
     * Skips VertexAI scoring when unique matches already fit maxResults — ranking is unnecessary.
     */
    public List<RetrievalSegment> processMatches(List<EmbeddingMatch<TextSegment>> matches, String question, int maxResults) {
        LOG.debugf("Processing %d matches for scoring (maxResults=%d)", (Object) matches.size(), (Object) maxResults);

        Map<String, EmbeddingMatch<TextSegment>> unique = new LinkedHashMap<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            unique.putIfAbsent(getParagraph(match), match);
        }

        boolean needsScoring = unique.size() > maxResults;
        LOG.debugf("Unique segments after dedup=%d, needsScoring=%b", (Object) unique.size(), (Object) needsScoring);

        return unique.values().stream()
                .map(match -> createRetrievalSegment(match, question, needsScoring))
                .toList();
    }

    public List<RetrievalSegment> processMatches(List<EmbeddingMatch<TextSegment>> matches, String question) {
        return processMatches(matches, question, Integer.MAX_VALUE);
    }

    /**
     * Filters and sorts segments based on score threshold.
     */
    public List<RetrievalSegment> filterAndSort(List<RetrievalSegment> segments, double minScore, int maxResults) {
        LOG.debugf("Filtering and sorting %d segments with minScore=%.2f, maxResults=%d",
                (Object) segments.size(), (Object) minScore, (Object) maxResults);

        List<RetrievalSegment> filtered = segments.stream()
                .filter(segment -> segment.modelScore() != null && segment.modelScore() >= minScore)
                .sorted((s1, s2) -> s2.modelScore().compareTo(s1.modelScore()))
                .limit(maxResults)
                .toList();

        LOG.debugf("Returning %d filtered segments", (Object) filtered.size());
        return filtered;
    }

    private RetrievalSegment createRetrievalSegment(EmbeddingMatch<TextSegment> match, String question, boolean withScoring) {

        String paragraphFromMetadata = match.embedded().metadata().getString(PARAGRAPH_KEY);
        String paragraph = paragraphFromMetadata != null ? paragraphFromMetadata : match.embedded().text();
        String questionToBeSaved = paragraphFromMetadata != null ? match.embedded().text() : question;

        Double modelScore;
        if (withScoring) {
            String paragraphPreview = paragraph.substring(0, Math.min(50, paragraph.length()));
            LOG.debugf("Calling VertexAI scoring for paragraph preview: %s...", (Object) paragraphPreview);
            try {
                var scoreResponse = scoringModel.score(paragraph, question);
                modelScore = scoreResponse.content();
                LOG.debugf("VertexAI score received: %.4f for paragraph: %s...", (Object) modelScore, (Object) paragraphPreview);
            } catch (Exception e) {
                LOG.errorf(e, "Error calling VertexAI scoring model for paragraph: %s...", (Object) paragraphPreview);
                throw e;
            }
        } else {
            modelScore = match.score();
        }

        return new RetrievalSegment(
                questionToBeSaved,
                paragraph,
                match.score(),
                modelScore
        );
    }

    private String getParagraph(EmbeddingMatch<TextSegment> match) {
        String paragraphFromMetadata = match.embedded().metadata().getString(PARAGRAPH_KEY);
        return paragraphFromMetadata != null ? paragraphFromMetadata : match.embedded().text();
    }
}

